import { before, after, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc, getDoc, getDocs, collection, query, where, serverTimestamp } from 'firebase/firestore';

let env;
const profile = (id, role = 'USER', locked = 0) => ({ id, email: `${id}@test.local`, username: id, full_name: id, role, is_locked: locked, avatar_url: '' });
const video = () => ({ id: 'film', title: 'Film', video_url: 'https://example.com/film.mp4', status: 'ACTIVE', _deleted: false, _server_updated_at: serverTimestamp() });
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-streamwire', firestore: { rules: await readFile(new URL('../firestore.rules', import.meta.url), 'utf8') } });
});
after(async () => { if (env) await env.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    for (const id of ['alice', 'bob', 'admin']) await setDoc(doc(db, 'users', id), profile(id, id === 'admin' ? 'ADMIN' : 'USER'));
    await setDoc(doc(db, 'videos', 'film'), video());
  });
});

test('registration cannot self-assign ADMIN or use another email', async () => {
  const db = env.authenticatedContext('new', { email: 'new@test.local' }).firestore();
  await assertFails(setDoc(doc(db, 'users', 'new'), profile('new', 'ADMIN')));
  await assertSucceeds(setDoc(doc(db, 'users', 'new'), profile('new')));
  await assertFails(updateDoc(doc(db, 'users', 'new'), { role: 'ADMIN', _server_updated_at: serverTimestamp() }));
});
test('private documents are restricted to their owner, including reads', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  const data = { user_id: 'alice', video_id: 'film', _deleted: false, _server_updated_at: serverTimestamp() };
  await assertSucceeds(setDoc(doc(alice, 'users/alice/favorites/film'), data));
  await assertFails(getDoc(doc(bob, 'users/alice/favorites/film')));
  await assertFails(setDoc(doc(bob, 'users/alice/favorites/film'), data));
  await assertFails(setDoc(doc(alice, 'users/alice/favorites/film'), { ...data, password: 'never-upload' }));
});
test('only active ADMIN can manage videos and cannot demote itself', async () => {
  const admin = env.authenticatedContext('admin').firestore();
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'videos/film'), video()));
  await assertSucceeds(setDoc(doc(admin, 'videos/film'), video()));
  await assertFails(updateDoc(doc(admin, 'users/admin'), { role: 'USER', _server_updated_at: serverTimestamp() }));
  await assertSucceeds(updateDoc(doc(admin, 'users/alice'), { is_locked: 1, _server_updated_at: serverTimestamp() }));
  await assertFails(setDoc(doc(alice, 'users/alice/settings/quality'), { user_id: 'alice', name: 'quality', value: '720p', _deleted: false, _server_updated_at: serverTimestamp() }));
});
test('guest catalog queries exclude hidden videos and tombstones', async () => {
  const admin = env.authenticatedContext('admin').firestore();
  await assertSucceeds(setDoc(doc(admin, 'videos/hidden'), { ...video(), id: 'hidden', status: 'PENDING' }));
  const guest = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(guest, 'videos/hidden')));
  await assertSucceeds(getDocs(query(collection(guest, 'videos'), where('status', '==', 'ACTIVE'), where('_deleted', '==', false))));
  await assertFails(getDocs(collection(guest, 'videos')));
});
test('offline deletion tombstones are allowed, invalid history is denied', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(alice, 'users/alice/history/h'), { user_id: 'alice', _deleted: true, _server_updated_at: serverTimestamp() }));
  const data = { id: 'h', user_id: 'alice', video_id: 'film', episode_title: '1', last_position_ms: 20, total_duration_ms: 10, updated_at: 'now', _deleted: false, _server_updated_at: serverTimestamp() };
  await assertFails(setDoc(doc(alice, 'users/alice/history/h'), data));
  await assertSucceeds(setDoc(doc(alice, 'users/alice/history/h'), { ...data, last_position_ms: 5 }));
});
