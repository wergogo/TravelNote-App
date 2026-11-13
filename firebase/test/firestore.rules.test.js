// Security-rules tests for TravelNote.
//
// "Allowed" cases perform the same reads and writes as the Android app
// (app/src/main/java/com/wergogo/travelnote/data/source/firebase/), batches included.
// "Denied" cases are things a malicious signed-in user could try.
//
// Run from the firebase/ folder:  npm test
// (starts the Firestore emulator for the offline project "demo-travelnote").

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  addDoc,
  arrayRemove,
  arrayUnion,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  orderBy,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';

const DAY = '2026-07-01';
let env;

// ── Fixture ────────────────────────────────────────────────────────────────
// alice owns open1 (members alice, bob, dave), locked1 (alice only) and legacy1
// (alice, bob; created before 1.0.2, so it has no join-code record). carol is a
// signed-in user who belongs to no trip.

const trip = (fields) => ({
  name: 'Paris',
  destination: 'France',
  startDate: '2026-07-01',
  endDate: '2026-07-03',
  totalBudget: 1000,
  currency: 'EUR',
  coverId: 'cover_city',
  createdAt: new Date(),
  ...fields,
});

const memberDoc = (uid, role) => ({
  userId: uid,
  nickname: uid,
  avatarId: 'avatar_01',
  role,
  joinedAt: serverTimestamp(),
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-travelnote',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (const uid of ['alice', 'bob', 'dave', 'carol']) {
      await setDoc(doc(db, `users/${uid}`), {
        id: uid, email: `${uid}@example.com`, nickname: uid, avatarId: 'avatar_01',
      });
    }
    await setDoc(doc(db, 'trips/open1'), trip({
      ownerId: 'alice', memberIds: ['alice', 'bob', 'dave'], visibility: 'open', joinCode: 'OPEN11',
    }));
    await setDoc(doc(db, 'trips/locked1'), trip({
      ownerId: 'alice', memberIds: ['alice'], visibility: 'locked', joinCode: 'LOCK11',
    }));
    await setDoc(doc(db, 'trips/legacy1'), trip({
      ownerId: 'alice', memberIds: ['alice', 'bob'], visibility: 'open', joinCode: 'OLD111',
    }));
    // Saved without a visibility field: treated as open, like the app does.
    await setDoc(doc(db, 'trips/novis1'), trip({
      ownerId: 'alice', memberIds: ['alice'], joinCode: 'NOVIS1',
    }));
    await setDoc(doc(db, 'joinCodes/OPEN11'), { tripId: 'open1' });
    await setDoc(doc(db, 'joinCodes/LOCK11'), { tripId: 'locked1' });
    await setDoc(doc(db, 'joinCodes/NOVIS1'), { tripId: 'novis1' });
    for (const [uid, role] of [['alice', 'owner'], ['bob', 'member'], ['dave', 'member']]) {
      await setDoc(doc(db, `trips/open1/members/${uid}`), memberDoc(uid, role));
    }
    await setDoc(doc(db, `trips/open1/days/${DAY}/events/ev1`), {
      type: 'activity', time: '10:00', cost: 20, title: 'Louvre', locationName: 'Paris',
      authorId: 'bob', authorNickname: 'bob', authorAvatarId: 'avatar_01',
      createdBy: 'bob', createdAt: new Date(),
    });
    // Saved by an older app version: no createdBy field.
    await setDoc(doc(db, `trips/open1/days/${DAY}/expenses/exp0`), {
      category: 'food', amount: 12.5, description: 'Lunch', placeLabel: '',
      authorId: 'alice', authorNickname: 'alice', authorAvatarId: 'avatar_01', createdAt: new Date(),
    });
    await setDoc(doc(db, `trips/open1/days/${DAY}/notes/n1`), {
      text: 'Bring passports', authorId: 'bob', authorNickname: 'bob', authorAvatarId: 'avatar_01',
      createdBy: 'bob', createdAt: new Date(),
    });
  });
});

const as = (uid) => env.authenticatedContext(uid).firestore();
const signedOut = () => env.unauthenticatedContext().firestore();

// ── Profiles ───────────────────────────────────────────────────────────────

describe('profiles (users/{uid})', () => {
  test('a user can read and update their own profile', async () => {
    const db = as('alice');
    await assertSucceeds(getDoc(doc(db, 'users/alice')));
    await assertSucceeds(updateDoc(doc(db, 'users/alice'), { nickname: 'Ali' }));
  });

  test('registering creates your own profile', async () => {
    const db = as('erin');
    await assertSucceeds(setDoc(doc(db, 'users/erin'), {
      id: 'erin', email: 'erin@example.com', nickname: 'Erin', avatarId: 'avatar_02',
      createdAt: serverTimestamp(),
    }));
  });

  test("cannot read someone else's profile (emails stay private)", async () => {
    await assertFails(getDoc(doc(as('carol'), 'users/alice')));
  });

  test('cannot list all users', async () => {
    await assertFails(getDocs(collection(as('carol'), 'users')));
  });

  test("cannot write someone else's profile", async () => {
    await assertFails(updateDoc(doc(as('carol'), 'users/alice'), { nickname: 'pwned' }));
  });

  test('signed-out clients cannot read profiles', async () => {
    await assertFails(getDoc(doc(signedOut(), 'users/alice')));
  });
});

// ── Reading trips ──────────────────────────────────────────────────────────

describe('reading trips', () => {
  test('a member can open the trip', async () => {
    await assertSucceeds(getDoc(doc(as('bob'), 'trips/open1')));
  });

  test('a non-member cannot read the trip', async () => {
    await assertFails(getDoc(doc(as('carol'), 'trips/open1')));
  });

  test('Home screen query: trips whose memberIds contain me', async () => {
    const db = as('bob');
    await assertSucceeds(getDocs(query(collection(db, 'trips'), where('memberIds', 'array-contains', 'bob'))));
  });

  test('cannot list every trip', async () => {
    await assertFails(getDocs(collection(as('carol'), 'trips')));
  });

  test('cannot find a trip by querying its join code', async () => {
    const db = as('carol');
    await assertFails(getDocs(query(collection(db, 'trips'), where('joinCode', '==', 'OPEN11'))));
  });

  test('looking up a trip that no longer exists reports "not found" rather than an error', async () => {
    const snap = await assertSucceeds(getDoc(doc(as('carol'), 'trips/gone')));
    assert.equal(snap.exists(), false);
  });
});

// ── Creating a trip ────────────────────────────────────────────────────────

describe('creating a trip', () => {
  const newTrip = (ownerId, memberIds = [ownerId]) =>
    trip({ ownerId, memberIds, visibility: 'open', joinCode: 'NEW111', createdAt: serverTimestamp() });

  test('creates the trip, owner member record and join-code record in one batch', async () => {
    const db = as('carol');
    const batch = writeBatch(db);
    batch.set(doc(db, 'trips/t-new'), newTrip('carol'));
    batch.set(doc(db, 'trips/t-new/members/carol'), memberDoc('carol', 'owner'));
    batch.set(doc(db, 'joinCodes/NEW111'), { tripId: 't-new' });
    await assertSucceeds(batch.commit());
  });

  test('uniqueness check: looking up an unused code', async () => {
    await assertSucceeds(getDoc(doc(as('carol'), 'joinCodes/ZZZ999')));
  });

  test('cannot create a trip owned by someone else', async () => {
    await assertFails(setDoc(doc(as('carol'), 'trips/t-x'), newTrip('alice')));
  });

  test('cannot create a trip that already includes other people', async () => {
    await assertFails(setDoc(doc(as('carol'), 'trips/t-x'), newTrip('carol', ['carol', 'bob'])));
  });

  test("cannot point a join code at someone else's trip", async () => {
    await assertFails(setDoc(doc(as('carol'), 'joinCodes/HACK11'), { tripId: 'open1' }));
  });
});

// ── Editing a trip ─────────────────────────────────────────────────────────

describe("editing a trip's settings", () => {
  const regenerate = (db, tripId, newCode, oldCode) => {
    const batch = writeBatch(db);
    batch.update(doc(db, `trips/${tripId}`), { joinCode: newCode });
    batch.set(doc(db, `joinCodes/${newCode}`), { tripId });
    batch.delete(doc(db, `joinCodes/${oldCode}`));
    return batch.commit();
  };

  test('the owner can edit settings and visibility', async () => {
    await assertSucceeds(updateDoc(doc(as('alice'), 'trips/open1'), {
      name: 'Paris 2026', destination: 'France', startDate: '2026-07-01', endDate: '2026-07-04',
      totalBudget: 1500, currency: 'EUR', visibility: 'locked', coverId: 'cover_beach',
    }));
  });

  test('the owner cannot hand the trip to someone else', async () => {
    await assertFails(updateDoc(doc(as('alice'), 'trips/open1'), { ownerId: 'bob' }));
  });

  test('the owner cannot rewrite the member list', async () => {
    await assertFails(updateDoc(doc(as('alice'), 'trips/open1'), { memberIds: ['alice'] }));
  });

  test('a member cannot edit settings', async () => {
    await assertFails(updateDoc(doc(as('bob'), 'trips/open1'), { name: 'Bob trip' }));
  });

  test('the owner can regenerate the join code (one batch)', async () => {
    await assertSucceeds(regenerate(as('alice'), 'open1', 'NEW222', 'OPEN11'));
  });

  test('the owner regenerates the code in a transaction, exactly as the app does', async () => {
    const db = as('alice');
    const tripRef = doc(db, 'trips/open1');
    await assertSucceeds(runTransaction(db, async (tx) => {
      const oldCode = (await tx.get(tripRef)).data().joinCode;
      const oldRecord = await tx.get(doc(db, `joinCodes/${oldCode}`));
      tx.update(tripRef, { joinCode: 'NEW444' });
      tx.set(doc(db, 'joinCodes/NEW444'), { tripId: 'open1' });
      if (oldRecord.exists() && oldRecord.data().tripId === 'open1') tx.delete(doc(db, `joinCodes/${oldCode}`));
    }));
  });

  test('a member cannot regenerate the join code', async () => {
    await assertFails(regenerate(as('bob'), 'open1', 'NEW222', 'OPEN11'));
  });

  test('regenerating an old trip that has no join-code record (the batch the app sends)', async () => {
    const db = as('alice');
    const batch = writeBatch(db);
    batch.update(doc(db, 'trips/legacy1'), { joinCode: 'NEW333' });
    batch.set(doc(db, 'joinCodes/NEW333'), { tripId: 'legacy1' });
    await assertSucceeds(batch.commit());
  });

  test('deleting a join-code record that does not exist is a harmless no-op', async () => {
    await assertSucceeds(regenerate(as('alice'), 'legacy1', 'NEW333', 'OLD111'));
  });
});

// ── Join codes ─────────────────────────────────────────────────────────────

describe('join codes', () => {
  test('anyone signed in can look up a code they already know', async () => {
    await assertSucceeds(getDoc(doc(as('carol'), 'joinCodes/OPEN11')));
  });

  test('join codes cannot be listed', async () => {
    await assertFails(getDocs(collection(as('carol'), 'joinCodes')));
  });

  test('signed-out clients cannot look up codes', async () => {
    await assertFails(getDoc(doc(signedOut(), 'joinCodes/OPEN11')));
  });

  test('a code record may only hold the trip id', async () => {
    await assertFails(setDoc(doc(as('bob'), 'joinCodes/OLD111'), { tripId: 'legacy1', note: 'extra' }));
  });

  test('code records cannot be modified', async () => {
    await assertFails(updateDoc(doc(as('alice'), 'joinCodes/OPEN11'), { tripId: 'locked1' }));
  });

  test("a member who isn't the owner cannot delete a code record", async () => {
    await assertFails(deleteDoc(doc(as('bob'), 'joinCodes/OPEN11')));
  });
});

// ── Joining a trip ─────────────────────────────────────────────────────────

describe('joining a trip', () => {
  const currentCode = { open1: 'OPEN11', locked1: 'LOCK11', novis1: 'NOVIS1' };
  // The app's join batch: add yourself to memberIds and write your member record, which carries
  // the code you joined with (the rules compare it with the trip's current code).
  const join = (db, tripId, uid, { code = currentCode[tripId], extra = {}, role = 'member' } = {}) => {
    const batch = writeBatch(db);
    batch.update(doc(db, `trips/${tripId}`), { memberIds: arrayUnion(uid), ...extra });
    const member = memberDoc(uid, role);
    if (code !== null) member.joinCode = code;   // null = record without a code
    batch.set(doc(db, `trips/${tripId}/members/${uid}`), member);
    return batch.commit();
  };

  test('a non-member can join an open trip with its current code (adds only themselves)', async () => {
    await assertSucceeds(join(as('carol'), 'open1', 'carol'));
  });

  test('cannot join with an old or wrong code', async () => {
    await assertFails(join(as('carol'), 'open1', 'carol', { code: 'WRONG1' }));
  });

  test('cannot join knowing only the trip id (no code)', async () => {
    await assertFails(join(as('carol'), 'open1', 'carol', { code: null }));
  });

  test('a trip saved without a visibility field counts as open', async () => {
    await assertSucceeds(join(as('carol'), 'novis1', 'carol'));
  });

  test('cannot join a locked trip', async () => {
    await assertFails(join(as('carol'), 'locked1', 'carol'));
  });

  test('cannot add someone else to a trip', async () => {
    await assertFails(updateDoc(doc(as('carol'), 'trips/open1'), { memberIds: arrayUnion('erin') }));
  });

  test('cannot change other fields while joining', async () => {
    await assertFails(join(as('carol'), 'open1', 'carol', { extra: { name: 'Hijacked' } }));
  });

  test('cannot take ownership while joining', async () => {
    await assertFails(join(as('carol'), 'open1', 'carol', { extra: { ownerId: 'carol' } }));
  });

  test('cannot join with the owner role', async () => {
    await assertFails(join(as('carol'), 'open1', 'carol', { role: 'owner' }));
  });

  test('cannot create a member record without joining', async () => {
    await assertFails(setDoc(doc(as('carol'), 'trips/open1/members/carol'), memberDoc('carol', 'member')));
  });
});

// ── Leaving and deleting ───────────────────────────────────────────────────

describe('leaving a trip', () => {
  test('a member can leave (removes only themselves and their member record)', async () => {
    const db = as('bob');
    const batch = writeBatch(db);
    batch.update(doc(db, 'trips/open1'), { memberIds: arrayRemove('bob') });
    batch.delete(doc(db, 'trips/open1/members/bob'));
    await assertSucceeds(batch.commit());
  });

  test('the owner cannot leave their own trip', async () => {
    await assertFails(updateDoc(doc(as('alice'), 'trips/open1'), { memberIds: arrayRemove('alice') }));
  });

  test('a member cannot remove another member', async () => {
    await assertFails(updateDoc(doc(as('bob'), 'trips/open1'), { memberIds: arrayRemove('dave') }));
  });

  test('leaving in two separate writes (as app 1.0.1 does) still works', async () => {
    const db = as('bob');
    await assertSucceeds(updateDoc(doc(db, 'trips/open1'), { memberIds: arrayRemove('bob') }));
    await assertSucceeds(deleteDoc(doc(db, 'trips/open1/members/bob')));
  });

  test('a former member loses access after leaving', async () => {
    const db = as('bob');
    const batch = writeBatch(db);
    batch.update(doc(db, 'trips/open1'), { memberIds: arrayRemove('bob') });
    batch.delete(doc(db, 'trips/open1/members/bob'));
    await batch.commit();
    await assertFails(getDoc(doc(db, 'trips/open1')));
    await assertFails(getDocs(collection(db, `trips/open1/days/${DAY}/events`)));
  });
});

describe('deleting a trip', () => {
  // The app's delete batch: member records, join-code record, the trip, and a tombstone.
  const deleteTrip = (db, tripId, code, memberIds) => {
    const batch = writeBatch(db);
    for (const uid of memberIds) batch.delete(doc(db, `trips/${tripId}/members/${uid}`));
    batch.delete(doc(db, `joinCodes/${code}`));
    batch.delete(doc(db, `trips/${tripId}`));
    batch.set(doc(db, `deletedTrips/${tripId}`), { deletedBy: 'alice', deletedAt: serverTimestamp() });
    return batch.commit();
  };

  test('the owner deletes the trip with its member records and code, leaving a tombstone', async () => {
    await assertSucceeds(deleteTrip(as('alice'), 'open1', 'OPEN11', ['alice', 'bob', 'dave']));
  });

  test('a deleted trip cannot be re-created to reach its leftover data', async () => {
    await deleteTrip(as('alice'), 'open1', 'OPEN11', ['alice', 'bob', 'dave']);
    await assertFails(setDoc(doc(as('carol'), 'trips/open1'), trip({
      ownerId: 'carol', memberIds: ['carol'], visibility: 'open', joinCode: 'MINE11',
    })));
  });

  test('a member cannot delete the trip', async () => {
    await assertFails(deleteDoc(doc(as('bob'), 'trips/open1')));
  });

  test('a trip cannot be deleted without leaving a tombstone (blocks the 1.0.1-style delete)', async () => {
    await assertFails(deleteDoc(doc(as('alice'), 'trips/open1')));
  });

  test('a tombstone can only be written while deleting your own trip', async () => {
    await assertFails(setDoc(doc(as('alice'), 'deletedTrips/open1'), {
      deletedBy: 'alice', deletedAt: serverTimestamp(),
    }));
    await assertFails(setDoc(doc(as('carol'), 'deletedTrips/someone-elses'), {
      deletedBy: 'carol', deletedAt: serverTimestamp(),
    }));
  });

  test('a trip with many members can still be deleted in one batch (rules access limits)', async () => {
    const members = Array.from({ length: 15 }, (_, i) => `m${i}`);
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'trips/big1'), trip({
        ownerId: 'alice', memberIds: ['alice', ...members], visibility: 'open', joinCode: 'BIG111',
      }));
      await setDoc(doc(db, 'joinCodes/BIG111'), { tripId: 'big1' });
      for (const uid of ['alice', ...members]) {
        await setDoc(doc(db, `trips/big1/members/${uid}`), memberDoc(uid, uid === 'alice' ? 'owner' : 'member'));
      }
    });
    await assertSucceeds(deleteTrip(as('alice'), 'big1', 'BIG111', ['alice', ...members]));
  });

  test('tombstones cannot be read or removed', async () => {
    await deleteTrip(as('alice'), 'open1', 'OPEN11', ['alice', 'bob', 'dave']);
    await assertFails(getDoc(doc(as('alice'), 'deletedTrips/open1')));
    await assertFails(deleteDoc(doc(as('alice'), 'deletedTrips/open1')));
  });
});

// ── Member records ─────────────────────────────────────────────────────────

describe('member records', () => {
  test('members can see the member list', async () => {
    await assertSucceeds(getDocs(collection(as('bob'), 'trips/open1/members')));
  });

  test('non-members cannot see the member list', async () => {
    await assertFails(getDocs(collection(as('carol'), 'trips/open1/members')));
  });

  test('a member can update the nickname/avatar copy on their own record', async () => {
    await assertSucceeds(updateDoc(doc(as('bob'), 'trips/open1/members/bob'), {
      nickname: 'Bobby', avatarId: 'avatar_05',
    }));
  });

  test("cannot edit another member's record", async () => {
    await assertFails(updateDoc(doc(as('bob'), 'trips/open1/members/dave'), { nickname: 'pwned' }));
  });

  test('cannot promote yourself to owner', async () => {
    await assertFails(updateDoc(doc(as('bob'), 'trips/open1/members/bob'), { role: 'owner' }));
  });

  test("the owner cannot delete another member's record while the trip exists", async () => {
    await assertFails(deleteDoc(doc(as('alice'), 'trips/open1/members/bob')));
  });
});

// ── Days and entries ───────────────────────────────────────────────────────

describe('days and entries', () => {
  const dayPath = `trips/open1/days/${DAY}`;
  const newEvent = (fields) => ({
    type: 'activity', time: '09:00', cost: 0, flightNumber: '', depAirport: '', arrAirport: '',
    title: 'Museum', locationName: '', authorNickname: 'bob', authorAvatarId: 'avatar_01',
    createdAt: serverTimestamp(), ...fields,
  });
  const newExpense = (fields) => ({
    category: 'food', amount: 30, description: 'Dinner', placeLabel: '',
    authorNickname: 'x', authorAvatarId: 'avatar_01', createdAt: serverTimestamp(), ...fields,
  });
  // What the app sends when editing an event: everything except createdAt/createdBy.
  const eventEdit = (fields) => ({
    type: 'activity', time: '10:30', cost: 25, flightNumber: '', depAirport: '', arrAirport: '',
    title: 'Louvre (tickets)', locationName: 'Paris', authorId: 'bob', authorNickname: 'bob',
    authorAvatarId: 'avatar_01', ...fields,
  });

  test('a member can read a day, even before anything is saved for it', async () => {
    await assertSucceeds(getDoc(doc(as('bob'), 'trips/open1/days/2026-07-02')));
  });

  test("a member can list the day's events in order", async () => {
    const db = as('bob');
    await assertSucceeds(getDocs(query(collection(db, `${dayPath}/events`), orderBy('createdAt'))));
  });

  test('a non-member cannot read entries', async () => {
    await assertFails(getDocs(collection(as('carol'), `${dayPath}/events`)));
  });

  test('a member can add an event they paid for', async () => {
    await assertSucceeds(setDoc(doc(as('bob'), `${dayPath}/events/e-new`),
      newEvent({ authorId: 'bob', createdBy: 'bob' }), { merge: true }));
  });

  test('a member can log an expense paid by another member', async () => {
    await assertSucceeds(setDoc(doc(as('bob'), `${dayPath}/expenses/x-new`),
      newExpense({ authorId: 'alice', createdBy: 'bob' }), { merge: true }));
  });

  test('the payer must be a trip member', async () => {
    await assertFails(setDoc(doc(as('bob'), `${dayPath}/expenses/x-new`),
      newExpense({ authorId: 'carol', createdBy: 'bob' }), { merge: true }));
  });

  test('cannot claim that someone else created an entry', async () => {
    await assertFails(setDoc(doc(as('bob'), `${dayPath}/events/e-new`),
      newEvent({ authorId: 'bob', createdBy: 'alice' }), { merge: true }));
  });

  test('new entries must record who created them (blocks pre-1.0.2 app writes)', async () => {
    await assertFails(setDoc(doc(as('bob'), `${dayPath}/events/e-new`),
      newEvent({ authorId: 'bob' }), { merge: true }));
  });

  test('a member can add a note', async () => {
    await assertSucceeds(addDoc(collection(as('bob'), `${dayPath}/notes`), {
      text: 'Meet at 9', authorId: 'bob', authorNickname: 'bob', authorAvatarId: 'avatar_01',
      createdAt: serverTimestamp(), createdBy: 'bob',
    }));
  });

  test("cannot take over authorship of someone else's note", async () => {
    await assertFails(updateDoc(doc(as('dave'), `${dayPath}/notes/n1`), { authorId: 'dave' }));
  });

  test("profile propagation can list a trip's days", async () => {
    await assertSucceeds(getDocs(collection(as('bob'), 'trips/open1/days')));
  });

  test('cannot post a note as someone else', async () => {
    await assertFails(addDoc(collection(as('bob'), `${dayPath}/notes`), {
      text: 'Fake', authorId: 'alice', authorNickname: 'alice', authorAvatarId: 'avatar_01',
      createdAt: serverTimestamp(), createdBy: 'bob',
    }));
  });

  test("any member can edit another member's entry", async () => {
    await assertSucceeds(setDoc(doc(as('dave'), `${dayPath}/events/ev1`), eventEdit({}), { merge: true }));
  });

  test('cannot change who created an entry', async () => {
    await assertFails(updateDoc(doc(as('dave'), `${dayPath}/events/ev1`), { createdBy: 'dave' }));
  });

  test('entries saved before 1.0.2 (no createdBy) can still be edited', async () => {
    await assertSucceeds(setDoc(doc(as('bob'), `${dayPath}/expenses/exp0`), {
      category: 'food', amount: 15, description: 'Lunch', placeLabel: '',
      authorId: 'alice', authorNickname: 'alice', authorAvatarId: 'avatar_01',
    }, { merge: true }));
  });

  test('editing an entry someone just deleted does not recreate it', async () => {
    await assertFails(setDoc(doc(as('bob'), `${dayPath}/events/gone`), eventEdit({}), { merge: true }));
  });

  test('a member can delete any entry', async () => {
    await assertSucceeds(deleteDoc(doc(as('dave'), `${dayPath}/events/ev1`)));
  });

  test('a non-member cannot add entries', async () => {
    await assertFails(setDoc(doc(as('carol'), `${dayPath}/events/c-1`),
      newEvent({ authorId: 'carol', createdBy: 'carol' }), { merge: true }));
  });

  test('profile changes can be copied onto your own entries', async () => {
    const db = as('bob');
    await assertSucceeds(getDocs(query(collection(db, `${dayPath}/events`), where('authorId', '==', 'bob'))));
    await assertSucceeds(updateDoc(doc(db, `${dayPath}/events/ev1`), {
      authorNickname: 'Bobby', authorAvatarId: 'avatar_05',
    }));
  });

  test('many entry updates in one trip fit in one batch (rules access limits)', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      for (let i = 0; i < 25; i++) {
        await setDoc(doc(db, `${dayPath}/expenses/bulk${i}`), {
          category: 'food', amount: i, description: '', placeLabel: '',
          authorId: 'bob', authorNickname: 'bob', authorAvatarId: 'avatar_01', createdBy: 'bob', createdAt: new Date(),
        });
      }
    });
    const db = as('bob');
    const batch = writeBatch(db);
    for (let i = 0; i < 25; i++) {
      batch.update(doc(db, `${dayPath}/expenses/bulk${i}`), { authorNickname: 'Bobby', authorAvatarId: 'avatar_05' });
    }
    await assertSucceeds(batch.commit());
  });

  test('cannot switch the payer to a non-member', async () => {
    await assertFails(updateDoc(doc(as('bob'), `${dayPath}/events/ev1`), { authorId: 'carol' }));
  });
});

// ── Backfilling join codes for trips created before 1.0.2 ──────────────────

describe('backfilling join codes for old trips', () => {
  test("a member can create the missing record for their trip's current code", async () => {
    await assertSucceeds(setDoc(doc(as('bob'), 'joinCodes/OLD111'), { tripId: 'legacy1' }));
  });

  test("cannot create a record for a code the trip doesn't use", async () => {
    await assertFails(setDoc(doc(as('bob'), 'joinCodes/ZZZ999'), { tripId: 'legacy1' }));
  });

  test('a non-member cannot create the record', async () => {
    await assertFails(setDoc(doc(as('carol'), 'joinCodes/OLD111'), { tripId: 'legacy1' }));
  });

  test('an existing record cannot be overwritten', async () => {
    await assertFails(setDoc(doc(as('bob'), 'joinCodes/OPEN11'), { tripId: 'open1' }));
  });
});
