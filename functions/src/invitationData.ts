import { DocumentData, FirestoreDataConverter, QueryDocumentSnapshot, Timestamp } from "firebase-admin/firestore";

export interface InvitationData {
  email: string;
  name: string;
  trainerId: string;
  createdAt: Timestamp;
  expiresAt: Timestamp;
}

export function invitationDataOf(data: DocumentData): InvitationData {
  const { email, name, trainerId, createdAt, expiresAt } = data;
  if (typeof email !== "string" || typeof name !== "string" || typeof trainerId !== "string") {
    throw new Error("Malformed invitation: email, name or trainerId is not a string");
  }
  if (!(createdAt instanceof Timestamp) || !(expiresAt instanceof Timestamp)) {
    throw new Error("Malformed invitation: createdAt or expiresAt is not a timestamp");
  }
  return { email, name, trainerId, createdAt, expiresAt };
}

export const invitationConverter: FirestoreDataConverter<InvitationData> = {
  toFirestore(invitation: InvitationData): DocumentData {
    return { ...invitation };
  },
  fromFirestore(snapshot: QueryDocumentSnapshot): InvitationData {
    return invitationDataOf(snapshot.data());
  }
};