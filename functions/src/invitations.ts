import { randomBytes } from "node:crypto";
import { getAuth } from "firebase-admin/auth";
import { DocumentSnapshot, getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
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

const REGION = "europe-west1";
const INVITATIONS = "invitations";
const USERS = "users";
const MAIL = "mail";

interface InvitationData {
  email: string;
  name: string;
  trainerId: string;
  code: string;
  createdAt: Timestamp;
  expiresAt: Timestamp;
}

export const inviteClient = onCall({ region: REGION }, async (request) => {
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
  const invitation: InvitationData = {
    email,
    name,
    trainerId,
    code: await uniqueCode(),
    createdAt: Timestamp.fromDate(now),
    expiresAt: Timestamp.fromDate(expiresAtFrom(now))
  };
  const ref = getFirestore().collection(INVITATIONS).doc();
  await ref.set(invitation);
  await queueMail(invitation);
  logger.info(`Trainer ${trainerId} invited a client`, { invitationId: ref.id });
  return { invitationId: ref.id };
});

export const resendInvitation = onCall({ region: REGION }, async (request) => {
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

  const renewed: InvitationData = {
    ...invitation,
    code: await uniqueCode(),
    expiresAt: Timestamp.fromDate(expiresAtFrom(new Date()))
  };
  await ref.update({ code: renewed.code, expiresAt: renewed.expiresAt });
  await queueMail(renewed);
  logger.info(`Trainer ${trainerId} resent an invitation`, { invitationId });
  return { invitationId };
});

export const getInvitation = onCall({ region: REGION }, async (request) => {
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

export const acceptInvitation = onCall({ region: REGION }, async (request) => {
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
  const found = await getFirestore().collection(INVITATIONS).where("code", "==", code).limit(1).get();
  const snapshot = found.docs[0];
  if (snapshot === undefined) {
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

async function uniqueCode(): Promise<string> {
  for (;;) {
    const code = generateCode((size) => randomBytes(size));
    const taken = await getFirestore().collection(INVITATIONS).where("code", "==", code).limit(1).get();
    if (taken.empty) return code;
  }
}

async function queueMail(invitation: InvitationData): Promise<void> {
  const trainer = await getFirestore().collection(USERS).doc(invitation.trainerId).get();
  const projectId = process.env.GCLOUD_PROJECT ?? "";
  const link = invitationLink(projectId, invitation.code);
  const message = invitationMail(invitation.name, trainer.get("name") ?? "Trener", link, invitation.code, invitation.expiresAt.toDate());
  await getFirestore().collection(MAIL).add({ to: invitation.email, message });
}