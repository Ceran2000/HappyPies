export const INVITATION_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000;
export const CODE_LENGTH = 8;
export const MIN_PASSWORD_LENGTH = 8;
const MAX_NAME_LENGTH = 100;
const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function generateCode(randomBytes: (size: number) => Uint8Array): string {
  return Array.from(randomBytes(CODE_LENGTH), (byte) => CODE_ALPHABET[byte % CODE_ALPHABET.length]).join("");
}

export function normalizeCode(input: unknown): string | undefined {
  if (typeof input !== "string") return undefined;
  const code = input.replace(/[\s-]/g, "").toUpperCase();
  return code.length === CODE_LENGTH && [...code].every((char) => CODE_ALPHABET.includes(char)) ? code : undefined;
}

export function normalizeEmail(input: unknown): string | undefined {
  if (typeof input !== "string") return undefined;
  const email = input.trim().toLowerCase();
  return EMAIL.test(email) ? email : undefined;
}

export function normalizeName(input: unknown): string | undefined {
  if (typeof input !== "string") return undefined;
  const name = input.trim().replace(/\s+/g, " ");
  return name.length > 0 && name.length <= MAX_NAME_LENGTH ? name : undefined;
}

export function isValidPassword(input: unknown): input is string {
  return typeof input === "string" && input.length >= MIN_PASSWORD_LENGTH;
}

export function expiresAtFrom(now: Date): Date {
  return new Date(now.getTime() + INVITATION_VALIDITY_MS);
}

export function isExpired(expiresAt: Date, now: Date): boolean {
  return expiresAt.getTime() <= now.getTime();
}

export function invitationLink(host: string, code: string): string {
  return `https://${host}/i/${code}`;
}

export interface InvitationMail {
  subject: string;
  text: string;
  html: string;
}

export function invitationMail(clientName: string, trainerName: string, link: string, code: string, expiresAt: Date): InvitationMail {
  const validUntil = expiresAt.toLocaleDateString("pl-PL", { timeZone: "Europe/Warsaw" });
  const text = [
    `Cześć ${clientName}!`,
    "",
    `${trainerName} zaprasza Cię do HappyPies, aplikacji do raportów z treningów.`,
    "",
    `Otwórz link na telefonie z zainstalowaną aplikacją: ${link}`,
    `albo w aplikacji wybierz „Mam zaproszenie” i wpisz kod: ${code}`,
    "",
    `Zaproszenie jest ważne do ${validUntil}.`
  ].join("\n");
  const html = [
    `<p>Cześć ${escapeHtml(clientName)}!</p>`,
    `<p>${escapeHtml(trainerName)} zaprasza Cię do HappyPies, aplikacji do raportów z treningów.</p>`,
    `<p><a href="${link}">Załóż konto</a></p>`,
    `<p>Albo w aplikacji wybierz „Mam zaproszenie” i wpisz kod: <strong>${code}</strong></p>`,
    `<p>Zaproszenie jest ważne do ${validUntil}.</p>`
  ].join("\n");
  return { subject: `${trainerName} zaprasza Cię do HappyPies`, text, html };
}

function escapeHtml(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}