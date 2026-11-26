import { initializeApp } from "firebase-admin/app";

initializeApp();

export { acceptInvitation, getInvitation, inviteClient, resendInvitation } from "./invitations.js";
export { cleanUpOrphanedMedia, cleanUpReportMedia } from "./media.js";