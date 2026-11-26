import { test } from "node:test";
import assert from "node:assert/strict";
import { queuedMailOf } from "./mail.js";

const valid = { to: "anna@example.com", message: { subject: "Zaproszenie", text: "Cześć", html: "<p>Cześć</p>" } };

test("queued mail is read from the mail document", () => {
  assert.deepEqual(queuedMailOf(valid), { to: "anna@example.com", subject: "Zaproszenie", text: "Cześć", html: "<p>Cześć</p>" });
});

test("mail without recipient is rejected", () => {
  assert.throws(() => queuedMailOf({ ...valid, to: " " }), /recipient/);
  assert.throws(() => queuedMailOf({ message: valid.message }), /recipient/);
});

test("mail without subject is rejected", () => {
  assert.throws(() => queuedMailOf({ ...valid, message: { ...valid.message, subject: 42 } }), /subject/);
});

test("mail without text or html body is rejected", () => {
  assert.throws(() => queuedMailOf({ ...valid, message: { ...valid.message, text: undefined } }), /body/);
  assert.throws(() => queuedMailOf({ ...valid, message: { ...valid.message, html: "" } }), /body/);
});

test("missing document or message is rejected", () => {
  assert.throws(() => queuedMailOf(undefined), /recipient/);
  assert.throws(() => queuedMailOf({ to: "anna@example.com" }), /subject/);
});