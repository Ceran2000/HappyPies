import { test } from "node:test";
import assert from "node:assert/strict";
import { isMediaOfClient } from "./mediaPath.js";
import { removedStoragePaths } from "./removedStoragePaths.js";

const media = (...paths: string[]) => ({ media: paths.map((storagePath) => ({ storagePath })) });

test("deleted report removes all its files", () => {
  assert.deepEqual(removedStoragePaths(media("a", "b"), undefined), ["a", "b"]);
});

test("edit removes only detached files", () => {
  assert.deepEqual(removedStoragePaths(media("a", "b", "c"), media("a", "c")), ["b"]);
});

test("new report removes nothing", () => {
  assert.deepEqual(removedStoragePaths(undefined, media("a")), []);
});

test("edit without media changes removes nothing", () => {
  assert.deepEqual(removedStoragePaths(media("a"), media("a")), []);
});

test("report without media field removes nothing", () => {
  assert.deepEqual(removedStoragePaths({}, undefined), []);
});

test("media of the report's client is deletable", () => {
  assert.equal(isMediaOfClient("clients/c1/media/uuid", "c1"), true);
});

test("media of another client is not deletable", () => {
  assert.equal(isMediaOfClient("clients/c2/media/uuid", "c1"), false);
});

test("paths outside the media folder are not deletable", () => {
  assert.equal(isMediaOfClient("clients/c1/other/uuid", "c1"), false);
  assert.equal(isMediaOfClient("clients/c1/media/", "c1"), false);
  assert.equal(isMediaOfClient("clients/c1/media/a/b", "c1"), false);
  assert.equal(isMediaOfClient("clients/c1/media/uuid", "c1/media/x"), false);
});

test("report without clientId deletes nothing", () => {
  assert.equal(isMediaOfClient("clients/c1/media/uuid", undefined), false);
  assert.equal(isMediaOfClient("clients//media/uuid", ""), false);
});