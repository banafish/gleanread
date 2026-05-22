export function resolveAvatarUrlFromMetadata(metadata: Record<string, unknown> | null | undefined): string | null {
  const candidates = [metadata?.avatar_url, metadata?.avatarUrl, metadata?.picture];

  for (const value of candidates) {
    if (typeof value !== "string") {
      continue;
    }
    const trimmed = value.trim();
    if (trimmed.length > 0) {
      return trimmed;
    }
  }

  return null;
}
