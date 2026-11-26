import { FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { defineSecret, defineString } from "firebase-functions/params";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { createTransport } from "nodemailer";
import { queuedMailOf } from "./mail.js";

const REGION = "europe-west1";

const smtpUser = defineString("SMTP_USER");
const smtpPassword = defineSecret("SMTP_PASSWORD");

export const sendQueuedMail = onDocumentCreated(
  { document: "mail/{mailId}", region: REGION, secrets: [smtpPassword] },
  async (event) => {
    const snapshot = event.data;
    if (snapshot === undefined) return;
    const mailId = event.params.mailId;

    try {
      const mail = queuedMailOf(snapshot.data());
      const transport = createTransport({
        host: "smtp.gmail.com",
        port: 465,
        secure: true,
        auth: { user: smtpUser.value(), pass: smtpPassword.value() }
      });
      const info = await transport.sendMail({ from: `HappyPies <${smtpUser.value()}>`, ...mail });
      await snapshot.ref.update({ delivery: { state: "SUCCESS", time: FieldValue.serverTimestamp(), messageId: info.messageId } });
      logger.info(`Mail ${mailId} sent`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      await snapshot.ref.update({ delivery: { state: "ERROR", time: FieldValue.serverTimestamp(), error: message } });
      logger.error(`Mail ${mailId} not sent`, { error: message });
    }
  }
);