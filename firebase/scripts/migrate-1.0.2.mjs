#!/usr/bin/env node
// One-off data migration for TravelNote 1.0.2. Run it once, BEFORE publishing firebase/firestore.rules.
//
//  1. Creates the joinCodes/{CODE} lookup record for every existing trip. Every old join code then
//     keeps working straight away, and nobody can claim a leaked old code for a trip of their own.
//  2. Writes a deletedTrips/{id} tombstone for every trip that was deleted but left data behind
//     (Firestore doesn't delete sub-collections), so that id can never be re-created to read it.
//  3. With --purge-orphans, also deletes that leftover data.
//
// It uses the Firebase Admin SDK, which bypasses security rules. Credentials come from a
// service-account key file kept OUTSIDE the repository (never commit it):
//
//   GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json npm run migrate                   # dry run: report only
//   GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json npm run migrate -- --apply        # write the changes
//   GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json npm run migrate -- --apply --purge-orphans
//
// Safe to re-run: existing join-code records and tombstones are left untouched, and a code already
// used by another trip is reported, never overwritten.

import { pathToFileURL } from 'node:url';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';

// Every sub-collection the app writes under trips/{id}.
const TRIP_SUBCOLLECTIONS = ['members', 'days', 'events', 'expenses', 'notes'];

/** "trips/{id}/…" → id, otherwise null. */
function tripIdOf(path) {
  const parts = path.split('/');
  return parts[0] === 'trips' && parts.length > 2 ? parts[1] : null;
}

/**
 * @param {import('firebase-admin/firestore').Firestore} db
 * @param {{ apply?: boolean, purgeOrphans?: boolean, hooks?: { afterScan?: () => Promise<void> } }} options
 *   Nothing is written unless apply is true. `hooks` exists only so tests can simulate activity mid-run.
 */
export async function migrate(db, { apply = false, purgeOrphans = false, hooks = {} } = {}) {
  const report = {
    joinCodesCreated: [],   // { code, tripId }
    joinCodesAlreadyOk: 0,
    collisions: [],         // { code, tripId, recordTripId }: the owner should regenerate the code
    tripsWithoutCode: [],   // tripId
    orphanTrips: [],        // ids of deleted trips that still have data underneath
    tombstonesCreated: [],  // tripId
    orphansPurged: [],      // tripId
  };

  // 1. Join-code lookup records for existing trips.
  const trips = await db.collection('trips').get();
  const existingTripIds = new Set(trips.docs.map((d) => d.id));
  const plannedCodes = new Map();   // code → tripId, so a dry run also spots two trips sharing a code
  for (const trip of trips.docs) {
    const code = trip.get('joinCode');
    if (typeof code !== 'string' || code === '') {
      report.tripsWithoutCode.push(trip.id);
      continue;
    }
    const recordRef = db.doc(`joinCodes/${code}`);
    const record = await recordRef.get();
    const recordTripId = record.exists ? record.get('tripId') : plannedCodes.get(code);
    if (recordTripId === undefined) {
      plannedCodes.set(code, trip.id);
      report.joinCodesCreated.push({ code, tripId: trip.id });
      if (apply) await recordRef.create({ tripId: trip.id });
    } else if (recordTripId === trip.id) {
      report.joinCodesAlreadyOk += 1;
    } else {
      report.collisions.push({ code, tripId: trip.id, recordTripId });
    }
  }

  // 2. Deleted trips whose sub-collections are still there.
  const orphanIds = new Set();
  for (const name of TRIP_SUBCOLLECTIONS) {
    const snap = await db.collectionGroup(name).select().get();
    for (const doc of snap.docs) {
      const tripId = tripIdOf(doc.ref.path);
      if (tripId && !existingTripIds.has(tripId)) orphanIds.add(tripId);
    }
  }
  await hooks.afterScan?.();

  // A trip created while the script runs (e.g. by app 1.0.1) isn't in the list fetched above, so
  // check each candidate again right before touching it: never tombstone or purge a live trip.
  for (const tripId of [...orphanIds].sort()) {
    if ((await db.doc(`trips/${tripId}`).get()).exists) orphanIds.delete(tripId);
  }
  report.orphanTrips = [...orphanIds].sort();

  for (const tripId of report.orphanTrips) {
    const tombstoneRef = db.doc(`deletedTrips/${tripId}`);
    if (!(await tombstoneRef.get()).exists) {
      report.tombstonesCreated.push(tripId);
      if (apply) {
        await tombstoneRef.create({ deletedBy: 'migration-1.0.2', deletedAt: FieldValue.serverTimestamp() });
      }
    }
    // 3. Optionally remove the leftover data itself.
    if (purgeOrphans) {
      report.orphansPurged.push(tripId);
      if (apply) await db.recursiveDelete(db.doc(`trips/${tripId}`));
    }
  }

  return report;
}

function printReport(report, { apply, purgeOrphans }) {
  const line = (label, items) => console.log(`${label}: ${items.length}${items.length ? '\n  ' + items.join('\n  ') : ''}`);
  console.log(apply ? '== Migration applied ==' : '== Dry run: nothing written ==');
  line('Join-code records to create', report.joinCodesCreated.map((c) => `${c.code} → trips/${c.tripId}`));
  console.log(`Join-code records already correct: ${report.joinCodesAlreadyOk}`);
  line('Code collisions (owner should tap "Regenerate code")',
    report.collisions.map((c) => `${c.code}: trips/${c.tripId} (record points at ${c.recordTripId})`));
  line('Trips without a join code', report.tripsWithoutCode);
  line('Deleted trips with leftover data', report.orphanTrips);
  line('Tombstones to create', report.tombstonesCreated);
  if (purgeOrphans) line('Leftover data to delete', report.orphansPurged);
  if (!apply) console.log('\nRe-run with --apply to write these changes.');
}

const runAsScript = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
if (runAsScript) {
  const args = new Set(process.argv.slice(2));
  const options = { apply: args.has('--apply'), purgeOrphans: args.has('--purge-orphans') };
  const usingEmulator = Boolean(process.env.FIRESTORE_EMULATOR_HOST);
  if (!usingEmulator && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    console.error('Set GOOGLE_APPLICATION_CREDENTIALS to the path of your service-account key file.');
    process.exit(1);
  }
  const app = usingEmulator
    ? initializeApp({ projectId: process.env.GCLOUD_PROJECT || 'demo-travelnote' })
    : initializeApp({ credential: applicationDefault() });
  printReport(await migrate(getFirestore(app), options), options);
}
