type ReportData = { clientId?: unknown; media?: { storagePath?: unknown }[] } | undefined;

export function removedStoragePaths(before: ReportData, after: ReportData): string[] {
  const remaining = new Set(storagePathsOf(after));
  return storagePathsOf(before).filter((path) => !remaining.has(path));
}

export function isMediaOfClient(path: string, clientId: unknown): boolean {
  if (typeof clientId !== "string" || clientId === "" || clientId.includes("/")) return false;
  const prefix = `clients/${clientId}/media/`;
  const fileName = path.slice(prefix.length);
  return path.startsWith(prefix) && fileName !== "" && !fileName.includes("/");
}

function storagePathsOf(report: ReportData): string[] {
  return (report?.media ?? []).flatMap((item) => (typeof item.storagePath === "string" ? [item.storagePath] : []));
}