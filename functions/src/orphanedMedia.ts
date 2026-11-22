import { clientIdOfMediaPath } from "./mediaPath.js";

export type StoredFile = { path: string; createdAt: Date };

export function orphanCandidates(files: StoredFile[], now: Date, minAgeMs: number): Map<string, string[]> {
  const candidates = new Map<string, string[]>();
  for (const file of files) {
    const clientId = clientIdOfMediaPath(file.path);
    if (clientId === undefined || now.getTime() - file.createdAt.getTime() < minAgeMs) continue;
    candidates.set(clientId, [...(candidates.get(clientId) ?? []), file.path]);
  }
  return candidates;
}

export function orphansOf(candidates: string[], referencedPaths: Set<string>): string[] {
  return candidates.filter((path) => !referencedPaths.has(path));
}