// Tests for scripts/set-password.mjs against the Auth emulator (Admin SDK, no credentials).
// Run with `npm test`, which starts the emulator first.

import assert from 'node:assert/strict';
import { after, before, beforeEach, test } from 'node:test';
import { deleteApp, initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { listUsers, setPassword } from '../scripts/set-password.mjs';

const PROJECT = 'demo-travelnote';
const host = process.env.FIREBASE_AUTH_EMULATOR_HOST;
let app;
let auth;

before(() => {
  app = initializeApp({ projectId: PROJECT }, 'set-password-test');
  auth = getAuth(app);
});

after(async () => {
  await deleteApp(app);
});

beforeEach(async () => {
  await fetch(`http://${host}/emulator/v1/projects/${PROJECT}/accounts`, { method: 'DELETE' });
  await auth.createUser({ email: 'tester1@example.test', password: 'old-pass-1' });
  await auth.createUser({ email: 'tester2@example.test', password: 'old-pass-2' });
});

async function signIn(email, password) {
  const res = await fetch(`http://${host}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true }),
  });
  return res.status;
}

test('the new password works and the old one does not', async () => {
  const result = await setPassword(auth, 'tester1@example.test', 'new-pass-1');
  assert.equal(result.email, 'tester1@example.test');
  assert.ok(result.uid);
  assert.equal(await signIn('tester1@example.test', 'new-pass-1'), 200);
  assert.equal(await signIn('tester1@example.test', 'old-pass-1'), 400);
});

test('an unknown email is rejected', async () => {
  await assert.rejects(() => setPassword(auth, 'nobody@example.test', 'new-pass-1'), /No account with email nobody@example\.test/);
});

test('a password shorter than 6 is rejected and the old one still works', async () => {
  await assert.rejects(() => setPassword(auth, 'tester1@example.test', '12345'), /at least 6 characters/);
  assert.equal(await signIn('tester1@example.test', 'old-pass-1'), 200);
});

test('listUsers returns the created accounts', async () => {
  const users = await listUsers(auth);
  assert.deepEqual(users.map((u) => u.email).sort(), ['tester1@example.test', 'tester2@example.test']);
  for (const u of users) {
    assert.ok(u.uid);
    assert.ok(u.createdAt);
    assert.ok('lastSignInAt' in u);
  }
});
