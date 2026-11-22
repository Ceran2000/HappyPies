type ReportData = { media?: { storagePath?: unknown }[] } | undefined;

export function removedStoragePaths(before: ReportData, after: ReportData): string[] {
  const remaining = new Set(storagePathsOf(after));
  return storagePathsOf(before).filter((path) => !remaining.has(path));
}

export function storagePathsOf(report: ReportData): string[] {
  return (report?.media ?? []).flatMap((item) => (typeof item.storagePath === "string" ? [item.storagePath] : []));
}