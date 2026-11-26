export interface QueuedMail {
  to: string;
  subject: string;
  text: string;
  html: string;
}

export function queuedMailOf(data: unknown): QueuedMail {
  const document = data as { to?: unknown; message?: { subject?: unknown; text?: unknown; html?: unknown } } | undefined;
  const to = document?.to;
  const subject = document?.message?.subject;
  const text = document?.message?.text;
  const html = document?.message?.html;
  if (!isFilled(to)) throw new Error("Mail without recipient");
  if (!isFilled(subject)) throw new Error("Mail without subject");
  if (!isFilled(text) || !isFilled(html)) throw new Error("Mail without text or html body");
  return { to, subject, text, html };
}

function isFilled(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}