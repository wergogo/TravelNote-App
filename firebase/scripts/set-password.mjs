#!/usr/bin/env node
// Sets a new password for a Firebase Auth account (email/password) using the Admin SDK.
// Handy for test accounts with fake emails, where a reset email can never arrive and the
// Firebase console offers no way to set a password directly.
//
// Usage (from the firebase/ folder):
//
//   npm run set-password -- --list                         # table of all accounts
//   npm run set-password -- <email> <new-password>         # set a new password (min. 6 characters)
//
// Credentials come from a service-account key file:
//
//   GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json npm run set-password -- --list
//
// Keep the service-account key OUTSIDE the repo (never commit it) and delete it when done.
// The password is never printed. Changing it signs the account out of its existing sessions.

import { pathToFileURL } from 'node:url';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';

const MIN_PASSWORD_LENGTH = 6;

/**
 * @param {import('firebase-admin/auth').Auth} auth
 * @returns {Promise<{ uid: string, email: string }>}
 */
export async function setPassword(auth, email, newPassword) {
  if (typeof newPassword !== 'string' || newPassword.length < MIN_PASSWORD_LENGTH) {
    throw new Error(`The password must be at least ${MIN_PASSWORD_LENGTH} characters long.`);
  }
  let user;
  try {
    user = await auth.getUserByEmail(email);
  } catch (e) {
    if (e?.code === 'auth/user-not-found') throw new Error(`No account with email ${email}`);
    throw e;
  }
  await auth.updateUser(user.uid, { password: newPassword });
  return { uid: user.uid, email: user.email };
}

/**
 * @param {import('firebase-admin/auth').Auth} auth
 * @returns {Promise<{ uid: string, email?: string, createdAt: string, lastSignInAt: string }[]>}
 */
export async function listUsers(auth) {
  const users = [];
  let pageToken;
  do {
    const page = await auth.listUsers(1000, pageToken);
    for (const u of page.users) {
      users.push({
        uid: u.uid,
        email: u.email,
        createdAt: u.metadata.creationTime,
        lastSignInAt: u.metadata.lastSignInTime,
      });
    }
    pageToken = page.pageToken;
  } while (pageToken);
  return users;
}

const USAGE = `Usage:
  npm run set-password -- --list
  npm run set-password -- <email> <new-password>`;

const runAsScript = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
if (runAsScript) {
  const args = process.argv.slice(2);
  const list = args.length === 1 && args[0] === '--list';
  const set = args.length === 2 && !args[0].startsWith('--');
  if (!list && !set) {
    console.error(USAGE);
    process.exit(1);
  }
  const usingEmulator = Boolean(process.env.FIREBASE_AUTH_EMULATOR_HOST);
  if (!usingEmulator && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    console.error('Set GOOGLE_APPLICATION_CREDENTIALS to the path of your service-account key file.');
    process.exit(1);
  }
  try {
    const app = usingEmulator
      ? initializeApp({ projectId: process.env.GCLOUD_PROJECT || 'demo-travelnote' })
      : initializeApp({ credential: applicationDefault() });
    const auth = getAuth(app);
    if (list) {
      const users = await listUsers(auth);
      console.table(users.map((u) => ({ email: u.email ?? '(none)', uid: u.uid, created: u.createdAt, lastSignIn: u.lastSignInAt })));
      console.log(`${users.length} account(s).`);
    } else {
      const { uid, email } = await setPassword(auth, args[0], args[1]);
      console.log(`Password updated for ${email} (uid ${uid}). Existing sessions are signed out.`);
    }
  } catch (e) {
    console.error(`Error: ${e.message}`);
    process.exit(1);
  }
}
