import { test } from "node:test";
import assert from "node:assert/strict";
import { Timestamp } from "firebase-admin/firestore";
import { invitationDataOf } from "./invitationData.js";

const valid = {
  email: "anna@example.com",
  name: "Anna",
  trainerId: "trainer-1",
  createdAt: Timestamp.fromMillis(1_000),
  expiresAt: Timestamp.fromMillis(2_000)
};

test("invitation is read with only its known fields", () => {
  assert.deepEqual(invitationDataOf({ ...valid, code: "K7P2QXHM" }), valid);
});

test("invitation with a missing or non-string field is rejected", () => {
  assert.throws(() => invitationDataOf({ ...valid, email: undefined }), /email, name or trainerId/);
  assert.throws(() => invitationDataOf({ ...valid, trainerId: 42 }), /email, name or trainerId/);
});

test("invitation with a non-timestamp date is rejected", () => {
  assert.throws(() => invitationDataOf({ ...valid, expiresAt: 2_000 }), /createdAt or expiresAt/);
  assert.throws(() => invitationDataOf({ ...valid, createdAt: undefined }), /createdAt or expiresAt/);
});