import { useMemo } from "react";
import { FileText, Pin, RotateCcw, Tag as TagIcon, Trash2 } from "lucide-react";
import { useAuth } from "@/app/providers/AuthProvider";
import {
  restoreExcerpt,
  restoreNodeSubtree,
  restoreTag,
} from "@/db/repositories/workspaceRepository";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { getTrashExcerpts, getTrashNodes, getTrashTags } from "@/features/workbench/workbenchSelectors";
import { Badge, Button, Dialog, SectionTitle } from "@/shared/components";
import { getSubtreeIds } from "@/shared/utils";
import type { WorkspaceSnapshot } from "@/shared/models";

function EmptyState({ label }: { label: string }) {
  return (
    <div className="rounded-panel border border-dashed border-app-border bg-app-surface2 p-4 text-sm text-app-muted">
      {label}
    </div>
  );
}

export function TrashDialog() {
  const { session, refreshWorkspace } = useAuth();
  const open = useWorkbenchStore((state) => state.trashOpen);
  const setTrashOpen = useWorkbenchStore((state) => state.setTrashOpen);
  const nodes = useWorkbenchStore((state) => state.nodes);
  const excerpts = useWorkbenchStore((state) => state.excerpts);
  const tags = useWorkbenchStore((state) => state.tags);
  const excerptTags = useWorkbenchStore((state) => state.excerptTags);
  const recentSearches = useWorkbenchStore((state) => state.recentSearches);

  const snapshot = useMemo<WorkspaceSnapshot>(
    () => ({ nodes, excerpts, tags, excerptTags, recentSearches }),
    [excerptTags, excerpts, nodes, recentSearches, tags]
  );

  const deletedNodes = getTrashNodes(snapshot);
  const deletedExcerpts = getTrashExcerpts(snapshot);
  const deletedTags = getTrashTags(snapshot);

  const restore = async (task: () => Promise<void>) => {
    if (!session?.userId) {
      return;
    }
    await task();
    await refreshWorkspace();
  };

  return (
    <Dialog open={open} title="垃圾篓" onClose={() => setTrashOpen(false)}>
      <div className="space-y-5">
        <SectionTitle title="已删除内容" subtitle="恢复节点会一并恢复其子树；摘录与标签可单独恢复。" />

        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-sm font-semibold text-app-text">节点</div>
            <Badge>{deletedNodes.length}</Badge>
          </div>
          {deletedNodes.length === 0 ? (
            <EmptyState label="暂无已删除节点。" />
          ) : (
            <div className="space-y-2">
              {deletedNodes.map((node) => {
                const subtreeCount = getSubtreeIds(snapshot.nodes, node.id).length;
                return (
                  <div key={node.id} className="rounded-panel border border-app-border bg-app-bg p-3 shadow-panel">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2 text-sm font-medium text-app-text">
                          <Trash2 size={14} className="text-app-danger" />
                          <span className="truncate">{node.nodeTitle}</span>
                        </div>
                        <div className="mt-1 text-xs text-app-muted">子树 {subtreeCount} 项</div>
                      </div>
                      <Button
                        type="button"
                        variant="secondary"
                        className="h-8 px-3 text-xs"
                        onClick={() => {
                          void restore(async () => restoreNodeSubtree(session!.userId, node.id));
                        }}
                      >
                        <RotateCcw size={13} />
                        恢复
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-sm font-semibold text-app-text">摘录</div>
            <Badge>{deletedExcerpts.length}</Badge>
          </div>
          {deletedExcerpts.length === 0 ? (
            <EmptyState label="暂无已删除摘录。" />
          ) : (
            <div className="space-y-2">
              {deletedExcerpts.map((excerpt) => (
                <div key={excerpt.id} className="rounded-panel border border-app-border bg-app-bg p-3 shadow-panel">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1 space-y-2">
                      <div className="flex items-center gap-2 text-sm font-medium text-app-text">
                        <FileText size={14} className="text-app-accent" />
                        <span className="truncate">{excerpt.sourceTitle ?? excerpt.url ?? "未记录来源"}</span>
                      </div>
                      <p className="line-clamp-3 text-sm leading-6 text-app-text">{excerpt.content}</p>
                      <div className="flex flex-wrap items-center gap-2 text-xs text-app-muted">
                        {excerpt.treeNodeId ? (
                          <span className="inline-flex items-center gap-1">
                            <Pin size={12} />
                            已挂载
                          </span>
                        ) : (
                          <span>当前在收件箱</span>
                        )}
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="secondary"
                      className="h-8 px-3 text-xs"
                      onClick={() => {
                        void restore(async () => restoreExcerpt(session!.userId, excerpt.id));
                      }}
                    >
                      <RotateCcw size={13} />
                      恢复
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-sm font-semibold text-app-text">标签</div>
            <Badge>{deletedTags.length}</Badge>
          </div>
          {deletedTags.length === 0 ? (
            <EmptyState label="暂无已删除标签。" />
          ) : (
            <div className="flex flex-wrap gap-2">
              {deletedTags.map((tag) => (
                <div key={tag.id} className="inline-flex items-center gap-2 rounded-full border border-app-border bg-app-bg px-3 py-2 text-sm text-app-text shadow-panel">
                  <span>{tag.colorIcon ?? <TagIcon size={12} />}</span>
                  <span className="max-w-40 truncate">{tag.tagName}</span>
                  <span className="text-xs text-app-muted">{tag.heatWeight}</span>
                  <button
                    type="button"
                    className="ml-1 inline-flex h-6 w-6 items-center justify-center rounded-full text-app-muted hover:text-app-text"
                    aria-label={`恢复标签 ${tag.tagName}`}
                    title="恢复"
                    onClick={() => {
                      void restore(async () => restoreTag(session!.userId, tag.id));
                    }}
                  >
                    <RotateCcw size={12} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </Dialog>
  );
}
