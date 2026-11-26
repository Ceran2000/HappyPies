import { randomInt } from "node:crypto";
import { getAuth } from "firebase-admin/auth";
import { DocumentReference, getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { HttpsError } from "firebase-functions/v2/https";
import {
  expiresAtFrom,
  generateCode,
  invitationIdOf,
  invitationLink,
  invitationMail,
  InvitationMail,
  isExpired,
  isInvitationId,
  isValidPassword,
  normalizeCode,
  normalizeEmail,
  normalizeName
} from "./invitation.js";
import { invitationConverter, InvitationData } from "./invitationData.js";

const INVITATIONS = "invitations";
const USERS = "users";
const GRPC_NOT_FOUND = 5;

export interface InvitationDependencies {
  sendMail(to: string, mail: InvitationMail): Promise<void>;
  codeKey(): string;
  linkHost(): string;
  now(): Date;
}

export interface CallRequest {
  data?: Record<string, unknown> | null;
  auth?: { uid: string };
}

interface ValidInvitation {
  ref: DocumentReference<InvitationData>;
  invitation: InvitationData;
}

export function invitationHandlers(dependencies: InvitationDependencies) {
  return {
    async inviteClient(request: CallRequest) {
      const trainerId = await requireTrainer(request.auth?.uid);
      const name = normalizeName(request.data?.name);
      const email = normalizeEmail(request.data?.email);
      if (name === undefined || email === undefined) {
        throw new HttpsError("invalid-argument", "Invalid name or email");
      }
      if (await accountExists(email)) {
        throw new HttpsError("already-exists", "Account already exists", { reason: "account-exists" });
      }

      const now = dependencies.now();
      const code = newCode();
      const invitation: InvitationData = {
        email,
        name,
        trainerId,
        createdAt: Timestamp.fromDate(now),
        expiresAt: Timestamp.fromDate(expiresAtFrom(now))
      };
      const ref = invitations().doc(invitationIdOf(code, dependencies.codeKey()));
      await getFirestore().runTransaction(async (transaction) => {
        const pending = await transaction.get(invitations().where("email", "==", email).limit(1));
        if (!pending.empty) {
          throw new HttpsError("already-exists", "Invitation already exists", { reason: "invitation-exists" });
        }
        transaction.create(ref, invitation);
      });
      try {
        await sendInvitationMail(invitation, code);
      } catch (error) {
        await ref.delete();
        throw error;
      }
      logger.info(`Trainer ${trainerId} invited a client`, { invitationId: ref.id });
      return { invitationId: ref.id };
    },

    async resendInvitation(request: CallRequest) {
      const trainerId = await requireTrainer(request.auth?.uid);
      const invitationId = request.data?.invitationId;
      if (!isInvitationId(invitationId)) {
        throw new HttpsError("invalid-argument", "Invalid invitationId");
      }
      const ref = invitations().doc(invitationId);
      const invitation = (await ref.get()).data();
      if (invitation === undefined || invitation.trainerId !== trainerId) {
        throw new HttpsError("not-found", "Invitation not found");
      }

      const code = newCode();
      const renewed: InvitationData = { ...invitation, expiresAt: Timestamp.fromDate(expiresAtFrom(dependencies.now())) };
      const renewedRef = invitations().doc(invitationIdOf(code, dependencies.codeKey()));
      const batch = getFirestore().batch();
      batch.create(renewedRef, renewed);
      batch.delete(ref, { exists: true });
      try {
        await batch.commit();
      } catch (error) {
        if (hasErrorCode(error, GRPC_NOT_FOUND)) {
          throw new HttpsError("not-found", "Invitation not found");
        }
        throw error;
      }
      await sendInvitationMail(renewed, code);
      logger.info(`Trainer ${trainerId} resent an invitation`, { invitationId: renewedRef.id });
      return { invitationId: renewedRef.id };
    },

    async getInvitation(request: CallRequest) {
      const { invitation } = await validInvitation(request.data?.code);
      return {
        email: invitation.email,
        name: invitation.name,
        trainerName: await trainerNameOf(invitation.trainerId),
        expiresAtMillis: String(invitation.expiresAt.toMillis())
      };
    },

    async acceptInvitation(request: CallRequest) {
      const password = request.data?.password;
      if (!isValidPassword(password)) {
        throw new HttpsError("invalid-argument", "Password too short");
      }
      const { ref, invitation } = await validInvitation(request.data?.code);

      let uid: string;
      try {
        uid = (await getAuth().createUser({ email: invitation.email, password })).uid;
      } catch (error) {
        if (hasErrorCode(error, "auth/email-already-exists")) {
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
        batch.delete(ref, { exists: true });
        await batch.commit();
      } catch (error) {
        await getAuth().deleteUser(uid);
        if (hasErrorCode(error, GRPC_NOT_FOUND)) {
          throw new HttpsError("not-found", "Unknown invitation code");
        }
        throw error;
      }
      logger.info(`Invitation ${ref.id} accepted`, { uid });
      return { email: invitation.email };
    }
  };

  async function validInvitation(input: unknown): Promise<ValidInvitation> {
    const code = normalizeCode(input);
    if (code === undefined) {
      throw new HttpsError("not-found", "Unknown invitation code");
    }
    const ref = invitations().doc(invitationIdOf(code, dependencies.codeKey()));
    const invitation = (await ref.get()).data();
    if (invitation === undefined) {
      throw new HttpsError("not-found", "Unknown invitation code");
    }
    if (isExpired(invitation.expiresAt.toDate(), dependencies.now())) {
      throw new HttpsError("failed-precondition", "Invitation expired", { reason: "expired" });
    }
    return { ref, invitation };
  }

  async function sendInvitationMail(invitation: InvitationData, code: string): Promise<void> {
    const link = invitationLink(dependencies.linkHost(), code);
    const trainerName = await trainerNameOf(invitation.trainerId);
    await dependencies.sendMail(invitation.email, invitationMail(invitation.name, trainerName, link, code, invitation.expiresAt.toDate()));
  }
}

function invitations() {
  return getFirestore().collection(INVITATIONS).withConverter(invitationConverter);
}

function newCode(): string {
  return generateCode((max) => randomInt(max));
}

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

async function trainerNameOf(trainerId: string): Promise<string> {
  const name = (await getFirestore().collection(USERS).doc(trainerId).get()).get("name");
  if (typeof name !== "string") {
    throw new Error(`Trainer ${trainerId} has no name`);
  }
  return name;
}

async function accountExists(email: string): Promise<boolean> {
  try {
    await getAuth().getUserByEmail(email);
    return true;
  } catch (error) {
    if (hasErrorCode(error, "auth/user-not-found")) return false;
    throw error;
  }
}

function hasErrorCode(error: unknown, code: string | number): boolean {
  return typeof error === "object" && error !== null && "code" in error && error.code === code;
}