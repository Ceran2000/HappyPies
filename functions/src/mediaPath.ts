const MEDIA_PATH = /^clients\/([^/]+)\/media\/[^/]+$/;

export function clientIdOfMediaPath(path: string): string | undefined {
  return MEDIA_PATH.exec(path)?.[1];
}

export function isMediaOfClient(path: string, clientId: unknown): boolean {
  return typeof clientId === "string" && clientIdOfMediaPath(path) === clientId;
}