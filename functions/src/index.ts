import { initializeApp } from "firebase-admin/app";
import { getStorage } from "firebase-admin/storage";
import { logger } from "firebase-functions";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { isMediaOfClient, removedStoragePaths } from "./removedStoragePaths.js";

initializeApp();

export const cleanUpReportMedia = onDocumentWritten({ document: "reports/{reportId}", region: "europe-west1" }, async (event) => {
  const before = event.data?.before.data();
  const removed = removedStoragePaths(before, event.data?.after.data());
  if (removed.length === 0) return;

  const reportId = event.params.reportId;
  const deletable = removed.filter((path) => isMediaOfClient(path, before?.clientId));
  const foreign = removed.filter((path) => !deletable.includes(path));
  if (foreign.length > 0) {
    logger.warn(`Report ${reportId} referenced files outside its client's media folder, not deleted`, { foreign });
  }

  const bucket = getStorage().bucket();
  await Promise.all(deletable.map((path) => bucket.file(path).delete({ ignoreNotFound: true })));
  logger.info(`Deleted ${deletable.length} file(s) of report ${reportId}`, { deletable });
});