import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { logger } from "firebase-functions";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { isMediaOfClient } from "./mediaPath.js";
export { acceptInvitation, getInvitation, inviteClient, resendInvitation } from "./invitations.js";
export { sendQueuedMail } from "./mails.js";
import { orphanCandidates, orphansOf } from "./orphanedMedia.js";
import { removedStoragePaths, storagePathsOf } from "./removedStoragePaths.js";

initializeApp();

const REGION = "europe-west1";
const ORPHAN_MIN_AGE_MS = 7 * 24 * 60 * 60 * 1000;

export const cleanUpReportMedia = onDocumentWritten({ document: "reports/{reportId}", region: REGION }, async (event) => {
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

export const cleanUpOrphanedMedia = onSchedule(
  { schedule: "every monday 03:00", timeZone: "Europe/Warsaw", region: REGION },
  async () => {
    const bucket = getStorage().bucket();
    const [files] = await bucket.getFiles({ prefix: "clients/" });
    const stored = files.map((file) => ({ path: file.name, createdAt: new Date(file.metadata.timeCreated ?? Date.now()) }));
    const candidates = orphanCandidates(stored, new Date(), ORPHAN_MIN_AGE_MS);

    const orphans: string[] = [];
    for (const [clientId, paths] of candidates) {
      const reports = await getFirestore().collection("reports").where("clientId", "==", clientId).get();
      const referenced = new Set(reports.docs.flatMap((report) => storagePathsOf(report.data())));
      orphans.push(...orphansOf(paths, referenced));
    }

    await Promise.all(orphans.map((path) => bucket.file(path).delete({ ignoreNotFound: true })));
    const candidateCount = [...candidates.values()].reduce((sum, paths) => sum + paths.length, 0);
    logger.info(`Checked ${files.length} file(s), ${candidateCount} older than 7 days, deleted ${orphans.length} orphan(s)`, { orphans });
  }
);