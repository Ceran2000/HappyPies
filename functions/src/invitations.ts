import "./options.js";
import { defineSecret, defineString } from "firebase-functions/params";
import { onCall } from "firebase-functions/v2/https";
import { invitationHandlers } from "./invitationHandlers.js";
import { sendMail, smtpPassword } from "./mailer.js";

const invitationLinkHost = defineString("INVITATION_LINK_HOST");
const invitationCodeKey = defineSecret("INVITATION_CODE_KEY");

const handlers = invitationHandlers({
  sendMail,
  codeKey: () => invitationCodeKey.value(),
  linkHost: () => invitationLinkHost.value(),
  now: () => new Date()
});

export const inviteClient = onCall({ secrets: [smtpPassword, invitationCodeKey] }, (request) => handlers.inviteClient(request));
export const resendInvitation = onCall({ secrets: [smtpPassword, invitationCodeKey] }, (request) => handlers.resendInvitation(request));
export const getInvitation = onCall({ secrets: [invitationCodeKey] }, (request) => handlers.getInvitation(request));
export const acceptInvitation = onCall({ secrets: [invitationCodeKey] }, (request) => handlers.acceptInvitation(request));