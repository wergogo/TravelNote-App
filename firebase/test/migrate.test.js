// Tests for scripts/migrate-1.0.2.mjs against the Firestore emulator (Admin SDK, rules bypassed).
// Run with `npm test`, which starts the emulator first.

import assert from 'node:assert/strict';
import { after, before, beforeEach, test } from 'node:test';
import { deleteApp, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { migrate } from '../scripts/migrate-1.0.2.mjs';

const PROJECT = 'demo-travelnote';
let app;
let db;

before(() => {
  app = initializeApp({ projectId: PROJECT }, 'migrate-test');
  db = getFirestore(app);
});

after(async () => {
  await deleteApp(app);
});

beforeEach(async () => {
  const host = process.env.FIRESTORE_EMULATOR_HOST;
  await fetch(`http://${host}/emulator/v1/projects/${PROJECT}/databases/(default)/documents`, { method: 'DELETE' });

  // Created before 1.0.2: no join-code record yet.
  await db.doc('trips/old1').set({ name: 'Old trip', joinCode: 'OLD111', ownerId: 'alice', memberIds: ['alice', 'bob'] });
  // Created by 1.0.2: already has its record.
  await db.doc('trips/new1').set({ name: 'New trip', joinCode: 'NEW111', ownerId: 'bob', memberIds: ['bob'] });
  await db.doc('joinCodes/NEW111').set({ tripId: 'new1' });
  // Its code was taken by another trip's record in the meantime.
  await db.doc('trips/clash1').set({ name: 'Clash', joinCode: 'CLASH1', ownerId: 'carol', memberIds: ['carol'] });
  await db.doc('joinCodes/CLASH1').set({ tripId: 'other' });
  // Deleted with the old app: only its sub-collections are left.
  await db.doc('trips/gone1/members/alice').set({ userId: 'alice', role: 'owner' });
  await db.doc('trips/gone1/days/2026-07-01/expenses/e1').set({ amount: 10, lat: 48.85, lng: 2.35 });
  // Existing trips' own sub-collections must not count as orphans.
  await db.doc('trips/old1/members/alice').set({ userId: 'alice', role: 'owner' });
});

test('a dry run reports what it would do and writes nothing', async () => {
  const report = await migrate(db);

  assert.deepEqual(report.joinCodesCreated, [{ code: 'OLD111', tripId: 'old1' }]);
  assert.equal(report.joinCodesAlreadyOk, 1);
  assert.deepEqual(report.collisions, [{ code: 'CLASH1', tripId: 'clash1', recordTripId: 'other' }]);
  assert.deepEqual(report.orphanTrips, ['gone1']);
  assert.deepEqual(report.tombstonesCreated, ['gone1']);
  assert.equal((await db.doc('joinCodes/OLD111').get()).exists, false);
  assert.equal((await db.doc('deletedTrips/gone1').get()).exists, false);
});

test('--apply creates missing join-code records and tombstones deleted trips', async () => {
  await migrate(db, { apply: true });

  assert.deepEqual((await db.doc('joinCodes/OLD111').get()).data(), { tripId: 'old1' });
  assert.equal((await db.doc('deletedTrips/gone1').get()).get('deletedBy'), 'migration-1.0.2');
  assert.equal((await db.doc('trips/gone1/members/alice').get()).exists, true, 'leftover data is kept by default');
  assert.deepEqual((await db.doc('joinCodes/CLASH1').get()).data(), { tripId: 'other' }, 'collisions are never overwritten');
});

test('--purge-orphans also deletes the leftover data of deleted trips', async () => {
  await migrate(db, { apply: true, purgeOrphans: true });

  assert.equal((await db.doc('trips/gone1/members/alice').get()).exists, false);
  assert.equal((await db.doc('trips/gone1/days/2026-07-01/expenses/e1').get()).exists, false);
  assert.equal((await db.doc('trips/old1/members/alice').get()).exists, true, 'existing trips are untouched');
});

test('a dry run also reports two old trips that share a code', async () => {
  await db.doc('trips/dupA').set({ name: 'A', joinCode: 'DUP111', ownerId: 'alice', memberIds: ['alice'] });
  await db.doc('trips/dupB').set({ name: 'B', joinCode: 'DUP111', ownerId: 'bob', memberIds: ['bob'] });

  const report = await migrate(db);

  assert.ok(report.joinCodesCreated.some((c) => c.code === 'DUP111' && c.tripId === 'dupA'));
  assert.deepEqual(report.collisions.filter((c) => c.code === 'DUP111'),
    [{ code: 'DUP111', tripId: 'dupB', recordTripId: 'dupA' }]);
});

test('a trip created while the script runs is never tombstoned or purged', async () => {
  // gone1 looks orphaned when the script lists trips, then "app 1.0.1" creates it mid-run.
  const hooks = {
    afterScan: () => db.doc('trips/gone1').set({ name: 'Created mid-run', joinCode: 'MID111', ownerId: 'alice', memberIds: ['alice'] }),
  };

  const report = await migrate(db, { apply: true, purgeOrphans: true, hooks });

  assert.deepEqual(report.orphanTrips, []);
  assert.equal((await db.doc('deletedTrips/gone1').get()).exists, false);
  assert.equal((await db.doc('trips/gone1/members/alice').get()).exists, true);
});

test('running it a second time changes nothing', async () => {
  await migrate(db, { apply: true });
  const second = await migrate(db, { apply: true });

  assert.deepEqual(second.joinCodesCreated, []);
  assert.deepEqual(second.tombstonesCreated, []);
  assert.equal(second.joinCodesAlreadyOk, 2);
});
