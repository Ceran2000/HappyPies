import { randomBytes } from "node:crypto";
import { getAuth } from "firebase-admin/auth";
import { DocumentSnapshot, getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { defineSecret, defineString } from "firebase-functions/params";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  expiresAtFrom,
  generateCode,
  invitationIdOf,
  invitationLink,
  invitationMail,
  isExpired,
  isValidPassword,
  normalizeCode,
  normalizeEmail,
  normalizeName
} from "./invitation.js";
import { sendMail, smtpPassword } from "./mailer.js";

const REGION = "europe-west1";
const INVITATIONS = "invitations";
const USERS = "users";

const invitationLinkHost = defineString("INVITATION_LINK_HOST");
const invitationCodeKey = defineSecret("INVITATION_CODE_KEY");

interface InvitationData {
  email: string;
  name: string;
  trainerId: string;
  createdAt: Timestamp;
  expiresAt: Timestamp;
}

export const inviteClient = onCall({ region: REGION, secrets: [smtpPassword, invitationCodeKey] }, async (request) => {
  const trainerId = await requireTrainer(request.auth?.uid);
  const name = normalizeName(request.data?.name);
  const email = normalizeEmail(request.data?.email);
  if (name === undefined || email === undefined) {
    throw new HttpsError("invalid-argument", "Invalid name or email");
  }
  if (await accountExists(email)) {
    throw new HttpsError("already-exists", `Account for ${email} already exists`, { reason: "account-exists" });
  }
  const pending = await getFirestore().collection(INVITATIONS).where("email", "==", email).limit(1).get();
  if (!pending.empty) {
    throw new HttpsError("already-exists", `Invitation for ${email} already exists`, { reason: "invitation-exists" });
  }

  const now = new Date();
  const code = generateCode((size) => randomBytes(size));
  const invitation: InvitationData = {
    email,
    name,
    trainerId,
    createdAt: Timestamp.fromDate(now),
    expiresAt: Timestamp.fromDate(expiresAtFrom(now))
  };
  const ref = getFirestore().collection(INVITATIONS).doc(invitationIdOf(code, invitationCodeKey.value()));
  await ref.create(invitation);
  try {
    await sendInvitationMail(invitation, code);
  } catch (error) {
    await ref.delete();
    throw error;
  }
  logger.info(`Trainer ${trainerId} invited a client`, { invitationId: ref.id });
  return { invitationId: ref.id };
});

export const resendInvitation = onCall({ region: REGION, secrets: [smtpPassword, invitationCodeKey] }, async (request) => {
  const trainerId = await requireTrainer(request.auth?.uid);
  const invitationId = request.data?.invitationId;
  if (typeof invitationId !== "string" || invitationId.length === 0) {
    throw new HttpsError("invalid-argument", "Missing invitationId");
  }
  const ref = getFirestore().collection(INVITATIONS).doc(invitationId);
  const snapshot = await ref.get();
  const invitation = snapshot.exists ? (snapshot.data() as InvitationData) : undefined;
  if (invitation === undefined || invitation.trainerId !== trainerId) {
    throw new HttpsError("not-found", `Invitation ${invitationId} not found`);
  }

  const code = generateCode((size) => randomBytes(size));
  const renewed: InvitationData = {
    email: invitation.email,
    name: invitation.name,
    trainerId: invitation.trainerId,
    createdAt: invitation.createdAt,
    expiresAt: Timestamp.fromDate(expiresAtFrom(new Date()))
  };
  const renewedRef = getFirestore().collection(INVITATIONS).doc(invitationIdOf(code, invitationCodeKey.value()));
  const batch = getFirestore().batch();
  batch.create(renewedRef, renewed);
  batch.delete(ref);
  await batch.commit();
  await sendInvitationMail(renewed, code);
  logger.info(`Trainer ${trainerId} resent an invitation`, { invitationId: renewedRef.id });
  return { invitationId: renewedRef.id };
});

export const getInvitation = onCall({ region: REGION, secrets: [invitationCodeKey] }, async (request) => {
  const snapshot = await validInvitation(request.data?.code);
  const invitation = snapshot.data() as InvitationData;
  const trainer = await getFirestore().collection(USERS).doc(invitation.trainerId).get();
  return {
    email: invitation.email,
    name: invitation.name,
    trainerName: trainer.get("name") ?? "",
    expiresAtMillis: String(invitation.expiresAt.toMillis())
  };
});

export const acceptInvitation = onCall({ region: REGION, secrets: [invitationCodeKey] }, async (request) => {
  const password = request.data?.password;
  if (!isValidPassword(password)) {
    throw new HttpsError("invalid-argument", "Password too short");
  }
  const snapshot = await validInvitation(request.data?.code);
  const invitation = snapshot.data() as InvitationData;

  let uid: string;
  try {
    uid = (await getAuth().createUser({ email: invitation.email, password })).uid;
  } catch (error) {
    if ((error as { code?: string }).code === "auth/email-already-exists") {
      throw new HttpsError("already-exists", "Account already exists", { reason: "account-exists" });
    }
    throw error;
  }

  try {
    const batch = getFirestore().batch();
    batch.create(getFirestore().collection(USERS).doc(uid), {
      role: "CLIENT",
      name: invitation.name,
      email: invitation.email,
      trainerId: invitation.trainerId,
      dogs: []
    });
    batch.delete(snapshot.ref);
    await batch.commit();
  } catch (error) {
    await getAuth().deleteUser(uid);
    throw error;
  }
  logger.info(`Invitation ${snapshot.id} accepted`, { uid });
  return { email: invitation.email };
});

async function requireTrainer(uid: string | undefined): Promise<string> {
  if (uid === undefined) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  const user = await getFirestore().collection(USERS).doc(uid).get();
  if (user.get("role") !== "TRAINER") {
    throw new HttpsError("permission-denied", "Only a trainer can manage invitations");
  }
  return uid;
}

async function validInvitation(input: unknown): Promise<DocumentSnapshot> {
  const code = normalizeCode(input);
  if (code === undefined) {
    throw new HttpsError("not-found", "Unknown invitation code");
  }
  const snapshot = await getFirestore().collection(INVITATIONS).doc(invitationIdOf(code, invitationCodeKey.value())).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Unknown invitation code");
  }
  if (isExpired((snapshot.data() as InvitationData).expiresAt.toDate(), new Date())) {
    throw new HttpsError("failed-precondition", "Invitation expired", { reason: "expired" });
  }
  return snapshot;
}

async function accountExists(email: string): Promise<boolean> {
  try {
    await getAuth().getUserByEmail(email);
    return true;
  } catch (error) {
    if ((error as { code?: string }).code === "auth/user-not-found") return false;
    throw error;
  }
}

async function sendInvitationMail(invitation: InvitationData, code: string): Promise<void> {
  const trainer = await getFirestore().collection(USERS).doc(invitation.trainerId).get();
  const link = invitationLink(invitationLinkHost.value(), code);
  await sendMail(invitation.email, invitationMail(invitation.name, trainer.get("name") ?? "Trener", link, code, invitation.expiresAt.toDate()));
}