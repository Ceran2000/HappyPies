import { test } from "node:test";
import assert from "node:assert/strict";
import {
  CODE_LENGTH,
  expiresAtFrom,
  generateCode,
  invitationLink,
  invitationMail,
  isExpired,
  isValidPassword,
  normalizeCode,
  normalizeEmail,
  normalizeName
} from "./invitation.js";

const now = new Date("2026-11-23T12:00:00Z");

test("generated code has the code length and no ambiguous characters", () => {
  const code = generateCode((size) => Uint8Array.from({ length: size }, (_, i) => i * 37));
  assert.equal(code.length, CODE_LENGTH);
  assert.doesNotMatch(code, /[01ILO]/);
  assert.equal(normalizeCode(code), code);
});

test("code is normalized from lower case, spaces and dashes", () => {
  assert.equal(normalizeCode(" k7p2-qxhm "), "K7P2QXHM");
});

test("code with wrong length, ambiguous characters or wrong type is rejected", () => {
  assert.equal(normalizeCode("K7P2QXH"), undefined);
  assert.equal(normalizeCode("K7P2QXH0"), undefined);
  assert.equal(normalizeCode(12345678), undefined);
});

test("email is trimmed and lower-cased", () => {
  assert.equal(normalizeEmail("  Anna.Kowalska@Przyklad.PL "), "anna.kowalska@przyklad.pl");
});

test("invalid email is rejected", () => {
  assert.equal(normalizeEmail("anna"), undefined);
  assert.equal(normalizeEmail("anna@przyklad"), undefined);
  assert.equal(normalizeEmail("an na@przyklad.pl"), undefined);
  assert.equal(normalizeEmail(undefined), undefined);
});

test("name is trimmed with inner whitespace collapsed", () => {
  assert.equal(normalizeName("  Anna   Kowalska "), "Anna Kowalska");
});

test("blank or too long name is rejected", () => {
  assert.equal(normalizeName("   "), undefined);
  assert.equal(normalizeName("a".repeat(101)), undefined);
});

test("password needs at least eight characters", () => {
  assert.equal(isValidPassword("1234567"), false);
  assert.equal(isValidPassword("12345678"), true);
  assert.equal(isValidPassword(12345678), false);
});

test("invitation expires exactly seven days after it is sent", () => {
  const expiresAt = expiresAtFrom(now);
  assert.equal(expiresAt.toISOString(), "2026-11-30T12:00:00.000Z");
  assert.equal(isExpired(expiresAt, new Date("2026-11-30T11:59:59Z")), false);
  assert.equal(isExpired(expiresAt, new Date("2026-11-30T12:00:00Z")), true);
});

test("mail escapes names in html and contains the link and the code", () => {
  const mail = invitationMail("<Anna>", "Jan", "https://p.web.app/i/K7P2QXHM", "K7P2QXHM", expiresAtFrom(now));
  assert.match(mail.html, /&lt;Anna&gt;/);
  assert.match(mail.html, /href="https:\/\/p\.web\.app\/i\/K7P2QXHM"/);
  assert.match(mail.text, /K7P2QXHM/);
  assert.match(mail.text, /30\.11\.2026/);
});

test("invitation link points to the code page on the given host", () => {
  assert.equal(invitationLink("dev-happypies.web.app", "ABCD2345"), "https://dev-happypies.web.app/i/ABCD2345");
});