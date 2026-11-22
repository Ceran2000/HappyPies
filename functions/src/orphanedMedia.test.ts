import { test } from "node:test";
import assert from "node:assert/strict";
import { orphanCandidates, orphansOf } from "./orphanedMedia.js";

const DAY_MS = 24 * 60 * 60 * 1000;
const now = new Date("2026-11-30T03:00:00Z");
const daysAgo = (days: number) => new Date(now.getTime() - days * DAY_MS);

test("files younger than the minimum age are not candidates", () => {
  const files = [{ path: "clients/c1/media/new", createdAt: daysAgo(6) }];
  assert.deepEqual(orphanCandidates(files, now, 7 * DAY_MS), new Map());
});

test("old media files are candidates grouped by client", () => {
  const files = [
    { path: "clients/c1/media/a", createdAt: daysAgo(8) },
    { path: "clients/c2/media/b", createdAt: daysAgo(30) },
    { path: "clients/c1/media/c", createdAt: daysAgo(7) }
  ];
  assert.deepEqual(
    orphanCandidates(files, now, 7 * DAY_MS),
    new Map([["c1", ["clients/c1/media/a", "clients/c1/media/c"]], ["c2", ["clients/c2/media/b"]]])
  );
});

test("paths outside client media folders are not candidates", () => {
  const files = [
    { path: "clients/c1/other/a", createdAt: daysAgo(30) },
    { path: "clients/c1/media/a/b", createdAt: daysAgo(30) },
    { path: "clients/c1/media/", createdAt: daysAgo(30) }
  ];
  assert.deepEqual(orphanCandidates(files, now, 7 * DAY_MS), new Map());
});

test("referenced files are kept and the rest are orphans", () => {
  const candidates = ["clients/c1/media/a", "clients/c1/media/b"];
  assert.deepEqual(orphansOf(candidates, new Set(["clients/c1/media/a"])), ["clients/c1/media/b"]);
});

test("all candidates of a client without reports are orphans", () => {
  assert.deepEqual(orphansOf(["clients/c1/media/a"], new Set()), ["clients/c1/media/a"]);
});