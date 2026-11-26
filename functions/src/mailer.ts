import { defineSecret, defineString } from "firebase-functions/params";
import { createTransport } from "nodemailer";
import { InvitationMail } from "./invitation.js";

const SMTP_TIMEOUT_MS = 10_000;

export const smtpPassword = defineSecret("SMTP_PASSWORD");
const smtpUser = defineString("SMTP_USER");

export async function sendMail(to: string, mail: InvitationMail): Promise<void> {
  const transport = createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true,
    auth: { user: smtpUser.value(), pass: smtpPassword.value() },
    connectionTimeout: SMTP_TIMEOUT_MS,
    greetingTimeout: SMTP_TIMEOUT_MS,
    socketTimeout: SMTP_TIMEOUT_MS
  });
  await transport.sendMail({ from: `HappyPies <${smtpUser.value()}>`, to, ...mail });
}