import { useEffect, useMemo, useRef } from "react";
import { Clock, FileText, Pin, Search, Tag } from "lucide-react";
import { addRecentSearch as persistRecentSearch } from "@/db/repositories/workspaceRepository";
import { useAuth } from "@/app/providers/AuthProvider";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { searchWorkspace } from "@/features/workbench/workbenchSelectors";
import { Dialog, Input } from "@/shared/components";
import type { SearchResult, WorkspaceSnapshot } from "@/shared/models";

function ResultIcon({ type }: { type: SearchResult["type"] }) {
  if (type === "node") {
    return <FileText size={16} />;
  }
  if (type === "tag") {
    return <Tag size={16} />;
  }
  return <Pin size={16} />;
}

export function SearchDialog() {
  const { session, refreshWorkspace } = useAuth();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const open = useWorkbenchStore((state) => state.searchOpen);
  const query = useWorkbenchStore((state) => state.searchQuery);
  const nodes = useWorkbenchStore((state) => state.nodes);
  const excerpts = useWorkbenchStore((state) => state.excerpts);
  const tags = useWorkbenchStore((state) => state.tags);
  const excerptTags = useWorkbenchStore((state) => state.excerptTags);
  const recentSearches = useWorkbenchStore((state) => state.recentSearches);
  const setSearchOpen = useWorkbenchStore((state) => state.setSearchOpen);
  const setSearchQuery = useWorkbenchStore((state) => state.setSearchQuery);
  const setSelectedNodeId = useWorkbenchStore((state) => state.setSelectedNodeId);
  const setSelectedExcerptId = useWorkbenchStore((state) => state.setSelectedExcerptId);
  const setDrawerOpen = useWorkbenchStore((state) => state.setDrawerOpen);
  const addRecentSearch = useWorkbenchStore((state) => state.addRecentSearch);

  const snapshot = useMemo<WorkspaceSnapshot>(
    () => ({ nodes, excerpts, tags, excerptTags, recentSearches }),
    [excerptTags, excerpts, nodes, recentSearches, tags]
  );
  const results = searchWorkspace(snapshot, query);

  useEffect(() => {
    if (open) {
      const frame = requestAnimationFrame(() => inputRef.current?.focus());
      return () => cancelAnimationFrame(frame);
    }
    return undefined;
  }, [open]);

  const close = () => {
    setSearchOpen(false);
    setSearchQuery("");
  };

  const rememberQuery = async () => {
    const trimmed = query.trim();
    if (!trimmed || !session?.userId) {
      return;
    }
    addRecentSearch(trimmed);
    await persistRecentSearch(session.userId, trimmed);
    await refreshWorkspace();
  };

  const selectResult = async (result: SearchResult) => {
    await rememberQuery();
    if (result.targetNodeId) {
      setSelectedNodeId(result.targetNodeId);
      setDrawerOpen(true);
    }
    if (result.excerptId) {
      setSelectedExcerptId(result.excerptId);
    }
    close();
  };

  return (
    <Dialog open={open} title="全局搜索" onClose={close}>
      <div className="space-y-4">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-app-muted" size={18} />
          <Input
            ref={inputRef}
            value={query}
            className="pl-10"
            placeholder="搜索节点、摘录或 #标签"
            onChange={(event) => setSearchQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Escape") {
                close();
              }
            }}
          />
        </div>

        {!query.trim() && recentSearches.length > 0 ? (
          <div className="space-y-2">
            <div className="text-xs font-semibold uppercase tracking-wide text-app-muted">最近搜索</div>
            <div className="flex flex-wrap gap-2">
              {recentSearches.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className="inline-flex items-center gap-1.5 rounded-full border border-app-border bg-app-surface2 px-3 py-1.5 text-sm text-app-muted hover:text-app-text"
                  onClick={() => setSearchQuery(item.query)}
                >
                  <Clock size={14} />
                  {item.query}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        <div className="space-y-2">
          {query.trim() && results.length === 0 ? (
            <div className="rounded-panel border border-dashed border-app-border bg-app-surface2 p-4 text-sm text-app-muted">
              没有找到匹配结果。
            </div>
          ) : null}
          {results.map((result) => (
            <button
              key={`${result.type}:${result.id}`}
              type="button"
              className="flex w-full items-center gap-3 rounded-panel border border-app-border bg-app-surface px-3 py-3 text-left transition hover:border-app-accent hover:bg-app-surface2"
              onClick={() => void selectResult(result)}
            >
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-app-accent/10 text-app-accent">
                <ResultIcon type={result.type} />
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium text-app-text">{result.title}</span>
                <span className="block truncate text-xs text-app-muted">{result.subtitle}</span>
              </span>
            </button>
          ))}
        </div>
      </div>
    </Dialog>
  );
}
