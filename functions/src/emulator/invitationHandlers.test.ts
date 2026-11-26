import { beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { invitationIdOf, InvitationMail } from "../invitation.js";
import { invitationHandlers } from "../invitationHandlers.js";

const PROJECT_ID = "demo-happypies";
const CODE_KEY = "test-key";
const DAY_MS = 24 * 60 * 60 * 1000;
const TRAINER = { uid: "trainer-1" };
const OTHER_TRAINER = { uid: "trainer-2" };
const CLIENT = { uid: "client-1" };
const ANNA = { name: "Anna Kowalska", email: "anna@example.com" };

initializeApp({ projectId: PROJECT_ID });

let sentMails: { to: string; mail: InvitationMail }[] = [];
let mailFails = false;
let now = new Date();

const handlers = invitationHandlers({
  async sendMail(to, mail) {
    if (mailFails) throw new Error("SMTP unavailable");
    sentMails.push({ to, mail });
  },
  codeKey: () => CODE_KEY,
  linkHost: () => "test.web.app",
  now: () => now
});

beforeEach(async () => {
  await clearEmulators();
  sentMails = [];
  mailFails = false;
  now = new Date();
  const users = getFirestore().collection("users");
  await users.doc(TRAINER.uid).set({ role: "TRAINER", name: "Jan Trener", email: "jan@example.com" });
  await users.doc(OTHER_TRAINER.uid).set({ role: "TRAINER", name: "Ewa Trenerka", email: "ewa@example.com" });
  await users.doc(CLIENT.uid).set({ role: "CLIENT", name: "Klient", email: "klient@example.com", trainerId: TRAINER.uid, dogs: [] });
});

test("only a signed-in trainer can invite", async () => {
  await assert.rejects(handlers.inviteClient({ data: ANNA }), { code: "unauthenticated" });
  await assert.rejects(handlers.inviteClient({ data: ANNA, auth: CLIENT }), { code: "permission-denied" });
});

test("invitation with an invalid email is rejected", async () => {
  await assert.rejects(handlers.inviteClient({ data: { ...ANNA, email: "anna" }, auth: TRAINER }), { code: "invalid-argument" });
});

test("invitation is stored under the code hmac without the code and the code is mailed", async () => {
  const { invitationId } = await handlers.inviteClient({ data: ANNA, auth: TRAINER });

  const code = lastMailedCode();
  assert.equal(invitationId, invitationIdOf(code, CODE_KEY));
  assert.equal(sentMails[0].to, ANNA.email);
  const stored = await getFirestore().collection("invitations").doc(invitationId).get();
  assert.deepEqual(Object.keys(stored.data() ?? {}).sort(), ["createdAt", "email", "expiresAt", "name", "trainerId"]);
});

test("invitation for an email with an account is rejected", async () => {
  await getAuth().createUser({ email: ANNA.email, password: "password1" });
  await assert.rejects(handlers.inviteClient({ data: ANNA, auth: TRAINER }), { code: "already-exists", details: { reason: "account-exists" } });
});

test("second invitation for the same email is rejected", async () => {
  await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  await assert.rejects(handlers.inviteClient({ data: ANNA, auth: OTHER_TRAINER }), { code: "already-exists", details: { reason: "invitation-exists" } });
});

test("parallel invitations for the same email create one invitation", async () => {
  const results = await Promise.allSettled([
    handlers.inviteClient({ data: ANNA, auth: TRAINER }),
    handlers.inviteClient({ data: ANNA, auth: TRAINER })
  ]);

  assert.deepEqual(results.map((result) => result.status).sort(), ["fulfilled", "rejected"]);
  assert.equal(await invitationCount(), 1);
});

test("invitation is removed when the mail cannot be sent", async () => {
  mailFails = true;
  await assert.rejects(handlers.inviteClient({ data: ANNA, auth: TRAINER }), /SMTP unavailable/);
  assert.equal(await invitationCount(), 0);
});

test("resend rejects a malformed invitation id", async () => {
  await assert.rejects(handlers.resendInvitation({ data: { invitationId: "a/b/c" }, auth: TRAINER }), { code: "invalid-argument" });
});

test("resend of another trainer's invitation is rejected", async () => {
  const { invitationId } = await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  await assert.rejects(handlers.resendInvitation({ data: { invitationId }, auth: OTHER_TRAINER }), { code: "not-found" });
});

test("resend moves the invitation to a new code and renews it", async () => {
  const { invitationId } = await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  const oldCode = lastMailedCode();
  now = new Date(now.getTime() + 3 * DAY_MS);

  const resent = await handlers.resendInvitation({ data: { invitationId }, auth: TRAINER });

  const newCode = lastMailedCode();
  assert.equal(resent.invitationId, invitationIdOf(newCode, CODE_KEY));
  await assert.rejects(handlers.getInvitation({ data: { code: oldCode } }), { code: "not-found" });
  const preview = await handlers.getInvitation({ data: { code: newCode } });
  assert.equal(Number(preview.expiresAtMillis), now.getTime() + 7 * DAY_MS);
  assert.equal(await invitationCount(), 1);
});

test("invitation preview shows the client and the trainer", async () => {
  await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  const preview = await handlers.getInvitation({ data: { code: lastMailedCode() } });
  assert.equal(preview.email, ANNA.email);
  assert.equal(preview.name, ANNA.name);
  assert.equal(preview.trainerName, "Jan Trener");
});

test("expired invitation is rejected", async () => {
  now = new Date(Date.now() - 8 * DAY_MS);
  await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  now = new Date();
  await assert.rejects(handlers.getInvitation({ data: { code: lastMailedCode() } }), { code: "failed-precondition", details: { reason: "expired" } });
});

test("unknown code is rejected", async () => {
  await assert.rejects(handlers.getInvitation({ data: { code: "ABCD2345" } }), { code: "not-found" });
  await assert.rejects(handlers.getInvitation({ data: { code: 42 } }), { code: "not-found" });
});

test("accepting creates the account and the client profile and removes the invitation", async () => {
  await handlers.inviteClient({ data: ANNA, auth: TRAINER });

  const result = await handlers.acceptInvitation({ data: { code: lastMailedCode(), password: "password1" } });

  assert.equal(result.email, ANNA.email);
  const account = await getAuth().getUserByEmail(ANNA.email);
  const profile = await getFirestore().collection("users").doc(account.uid).get();
  assert.deepEqual(profile.data(), { role: "CLIENT", name: ANNA.name, email: ANNA.email, trainerId: TRAINER.uid, dogs: [] });
  assert.equal(await invitationCount(), 0);
});

test("accepting with a short password creates nothing", async () => {
  await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  await assert.rejects(handlers.acceptInvitation({ data: { code: lastMailedCode(), password: "short" } }), { code: "invalid-argument" });
  await assert.rejects(getAuth().getUserByEmail(ANNA.email), { code: "auth/user-not-found" });
  assert.equal(await invitationCount(), 1);
});

test("invitation can be accepted only once", async () => {
  await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  const code = lastMailedCode();
  await handlers.acceptInvitation({ data: { code, password: "password1" } });
  await assert.rejects(handlers.acceptInvitation({ data: { code, password: "password2" } }), { code: "not-found" });
});

test("accepting when the email got an account in the meantime keeps the invitation", async () => {
  await handlers.inviteClient({ data: ANNA, auth: TRAINER });
  await getAuth().createUser({ email: ANNA.email, password: "password1" });
  await assert.rejects(handlers.acceptInvitation({ data: { code: lastMailedCode(), password: "password2" } }), { code: "already-exists", details: { reason: "account-exists" } });
  assert.equal(await invitationCount(), 1);
});

function lastMailedCode(): string {
  const code = sentMails.at(-1)?.mail.text.match(/kod: ([A-Z0-9]{8})/)?.[1];
  if (code === undefined) throw new Error("No invitation code was mailed");
  return code;
}

async function invitationCount(): Promise<number> {
  return (await getFirestore().collection("invitations").count().get()).data().count;
}

async function clearEmulators(): Promise<void> {
  await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, { method: "DELETE" });
  await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/emulator/v1/projects/${PROJECT_ID}/accounts`, { method: "DELETE" });
}