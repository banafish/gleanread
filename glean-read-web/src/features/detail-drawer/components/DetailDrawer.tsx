import { useEffect, useMemo, useState } from "react";
import { Maximize2, Minimize2, PanelRightClose, Plus, RotateCcw, Tag as TagIcon, Trash2, X } from "lucide-react";
import {
  ensureTag,
  deleteExcerpt,
  deleteTag,
  moveExcerptToNode,
  moveNode,
  renameNode,
  updateExcerpt,
  updateNodeOutline,
} from "@/db/repositories/workspaceRepository";
import { useAuth } from "@/app/providers/AuthProvider";
import { NodeOutlineEditor } from "@/features/detail-drawer/editor/NodeOutlineEditor";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { getNodeFeed, getSelectedNode } from "@/features/workbench/workbenchSelectors";
import { Badge, Button, IconButton, Input, SectionTitle, Textarea } from "@/shared/components";
import type { ExcerptViewModel, Tag, WorkspaceSnapshot } from "@/shared/models";
import { cx, isDescendant } from "@/shared/utils";

function ExcerptThoughtEditor({
  excerpt,
  onSave,
}: {
  excerpt: ExcerptViewModel;
  onSave: (excerpt: ExcerptViewModel, userThought: string) => Promise<void>;
}) {
  const [value, setValue] = useState(excerpt.userThought ?? "");

  useEffect(() => {
    setValue(excerpt.userThought ?? "");
  }, [excerpt.id, excerpt.userThought]);

  return (
    <Textarea
      data-testid="excerpt-thought-input"
      value={value}
      rows={3}
      placeholder="我的思考"
      onChange={(event) => setValue(event.target.value)}
      onBlur={() => {
        if (value !== (excerpt.userThought ?? "")) {
          void onSave(excerpt, value);
        }
      }}
    />
  );
}

function ExcerptTagEditor({
  excerpt,
  tags,
  onSetTags,
  onDeleteTag,
}: {
  excerpt: ExcerptViewModel;
  tags: Tag[];
  onSetTags: (excerpt: ExcerptViewModel, tagIds: string[], newTagName?: string) => Promise<void>;
  onDeleteTag: (tagId: string) => Promise<void>;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const selectedIds = new Set(excerpt.tags.map((tag) => tag.id));
  const suggestions = tags
    .filter((tag) => !tag.isDeleted && !selectedIds.has(tag.id))
    .filter((tag) => !query.trim() || tag.tagName.toLowerCase().includes(query.trim().toLowerCase()))
    .sort((a, b) => b.heatWeight - a.heatWeight)
    .slice(0, 5);

  const submitNewTag = async () => {
    const trimmed = query.trim();
    if (!trimmed) {
      return;
    }
    await onSetTags(excerpt, excerpt.tags.map((tag) => tag.id), trimmed);
    setQuery("");
    setOpen(false);
  };

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-1.5">
        {excerpt.tags.map((tag) => (
          <span
            key={tag.id}
            className="inline-flex items-center gap-1 rounded-full border border-app-border bg-app-surface2 px-2 py-1 text-xs text-app-muted"
          >
            <span>{tag.colorIcon ?? <TagIcon size={11} />}</span>
            {tag.tagName}
            <button
              type="button"
              className="text-app-muted hover:text-app-danger"
              title="移除标签"
              aria-label="移除标签"
              onClick={() => void onSetTags(excerpt, excerpt.tags.filter((item) => item.id !== tag.id).map((item) => item.id))}
            >
              <X size={12} />
            </button>
            <button
              type="button"
              className="text-app-muted hover:text-app-danger"
              title="删除标签"
              aria-label="删除标签"
              onClick={() => {
                void onDeleteTag(tag.id);
              }}
            >
              <Trash2 size={12} />
            </button>
          </span>
        ))}
        <button
          type="button"
          data-testid="tag-add-toggle"
          className="inline-flex h-7 w-7 items-center justify-center rounded-full border border-app-border text-app-muted hover:bg-app-surface2 hover:text-app-text"
          title="添加标签"
          aria-label="添加标签"
          onClick={() => setOpen((value) => !value)}
        >
          <Plus size={13} />
        </button>
      </div>
      {open ? (
        <div className="rounded-xl border border-app-border bg-app-surface p-2 shadow-panel">
          <Input
            value={query}
            data-testid="tag-query-input"
            placeholder="搜索或创建标签"
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                void submitNewTag();
              }
            }}
          />
          <div className="mt-2 flex flex-wrap gap-1.5">
            {suggestions.map((tag) => (
              <button
                key={tag.id}
                type="button"
                className="rounded-full border border-app-border bg-app-surface2 px-2 py-1 text-xs text-app-muted hover:text-app-text"
                onClick={() => {
                  void onSetTags(excerpt, [...excerpt.tags.map((item) => item.id), tag.id]);
                  setOpen(false);
                  setQuery("");
                }}
              >
                {tag.colorIcon ?? ""} {tag.tagName}
              </button>
            ))}
            {query.trim() ? (
              <button
                type="button"
                className="rounded-full border border-app-accent bg-app-accent/10 px-2 py-1 text-xs font-medium text-app-accent"
                onClick={() => void submitNewTag()}
              >
                创建 {query.trim()}
              </button>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}

export function DetailDrawer() {
  const { session, refreshWorkspace } = useAuth();
  const nodes = useWorkbenchStore((state) => state.nodes);
  const excerpts = useWorkbenchStore((state) => state.excerpts);
  const tags = useWorkbenchStore((state) => state.tags);
  const excerptTags = useWorkbenchStore((state) => state.excerptTags);
  const recentSearches = useWorkbenchStore((state) => state.recentSearches);
  const selectedNodeId = useWorkbenchStore((state) => state.selectedNodeId);
  const drawerFullscreen = useWorkbenchStore((state) => state.drawerFullscreen);
  const setDrawerOpen = useWorkbenchStore((state) => state.setDrawerOpen);
  const setDrawerFullscreen = useWorkbenchStore((state) => state.setDrawerFullscreen);
  const setSelectedExcerptId = useWorkbenchStore((state) => state.setSelectedExcerptId);
  const [titleDraft, setTitleDraft] = useState("");

  const snapshot = useMemo<WorkspaceSnapshot>(
    () => ({ nodes, excerpts, tags, excerptTags, recentSearches }),
    [excerptTags, excerpts, nodes, recentSearches, tags]
  );
  const selectedNode = getSelectedNode(snapshot, selectedNodeId);
  const feed = getNodeFeed(snapshot, selectedNodeId);
  const moveTargets = useMemo(
    () =>
      selectedNode
        ? snapshot.nodes
            .filter((node) => !node.isDeleted && node.id !== selectedNode.id && !isDescendant(snapshot.nodes, selectedNode.id, node.id))
            .map((node) => ({
              id: node.id,
              title: node.nodeTitle,
              depth: snapshot.nodes.filter((candidate) => candidate.id === node.id || isDescendant(snapshot.nodes, candidate.id, node.id)).length,
            }))
        : [],
    [selectedNode, snapshot.nodes]
  );

  useEffect(() => {
    setTitleDraft(selectedNode?.nodeTitle ?? "");
  }, [selectedNode?.id, selectedNode?.nodeTitle]);

  const saveTitle = async () => {
    if (!session?.userId || !selectedNode || titleDraft.trim() === selectedNode.nodeTitle) {
      return;
    }
    await renameNode(session.userId, selectedNode.id, titleDraft.trim());
    await refreshWorkspace();
  };

  const saveThought = async (excerpt: ExcerptViewModel, userThought: string) => {
    if (!session?.userId) {
      return;
    }
    await updateExcerpt(session.userId, excerpt.id, {
      content: excerpt.content,
      userThought,
      sourceTitle: excerpt.sourceTitle,
      url: excerpt.url,
      treeNodeId: excerpt.treeNodeId,
      tagIds: excerpt.tags.map((tag) => tag.id),
    });
    await refreshWorkspace();
  };

  const setExcerptTags = async (excerpt: ExcerptViewModel, tagIds: string[], newTagName?: string) => {
    if (!session?.userId) {
      return;
    }
    let nextTagIds = tagIds;
    if (newTagName) {
      const tag = await ensureTag(session.userId, newTagName, "●");
      if (tag) {
        nextTagIds = [...new Set([...tagIds, tag.id])];
      }
    }
    await updateExcerpt(session.userId, excerpt.id, {
      content: excerpt.content,
      userThought: excerpt.userThought,
      sourceTitle: excerpt.sourceTitle,
      url: excerpt.url,
      treeNodeId: excerpt.treeNodeId,
      tagIds: nextTagIds,
    });
    await refreshWorkspace();
  };

  const deleteExcerptCard = async (excerptId: string) => {
    if (!session?.userId) {
      return;
    }
    await deleteExcerpt(session.userId, excerptId);
    if (useWorkbenchStore.getState().selectedExcerptId === excerptId) {
      setSelectedExcerptId(null);
    }
    await refreshWorkspace();
  };

  const deleteTagEntity = async (tagId: string) => {
    if (!session?.userId) {
      return;
    }
    await deleteTag(session.userId, tagId);
    await refreshWorkspace();
  };

  const moveToInbox = async (excerptId: string) => {
    if (!session?.userId) {
      return;
    }
    await moveExcerptToNode(session.userId, excerptId, null);
    setSelectedExcerptId(excerptId);
    await refreshWorkspace();
  };

  const content = (
    <aside className="flex h-full min-h-0 flex-col border-l border-app-border bg-app-surface" data-testid="detail-drawer">
      <div className="flex items-center justify-between border-b border-app-border px-4 py-3">
        <div className="min-w-0 flex-1">
          {selectedNode ? (
            <Input
              value={titleDraft}
              data-testid="detail-node-title-input"
              className="border-transparent bg-transparent px-0 text-base font-semibold focus:border-transparent focus:ring-0"
              onChange={(event) => setTitleDraft(event.target.value)}
              onBlur={() => void saveTitle()}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.currentTarget.blur();
                }
              }}
            />
          ) : (
            <div className="text-base font-semibold text-app-text">节点详情</div>
          )}
        </div>
        <div className="flex items-center gap-1">
          <IconButton
            type="button"
            data-testid="drawer-fullscreen-toggle"
            title={drawerFullscreen ? "退出专注写作" : "专注写作"}
            aria-label={drawerFullscreen ? "退出专注写作" : "专注写作"}
            onClick={() => setDrawerFullscreen(!drawerFullscreen)}
          >
            {drawerFullscreen ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
          </IconButton>
          <IconButton type="button" data-testid="drawer-close" title="关闭抽屉" aria-label="关闭抽屉" onClick={() => setDrawerOpen(false)}>
            <PanelRightClose size={17} />
          </IconButton>
        </div>
      </div>

      <div className="min-h-0 flex-1 space-y-4 overflow-auto p-4">
        {!selectedNode ? (
          <div className="rounded-panel border border-dashed border-app-border bg-app-surface2 p-5 text-sm text-app-muted">
            选择一个知识节点后，沉淀区会显示对应大纲和摘录。
          </div>
        ) : (
          <>
            <NodeOutlineEditor
              key={selectedNode.id}
              nodeId={selectedNode.id}
              value={selectedNode.outlineMarkdown}
              excerpts={feed.map((e) => e.content)}
              onSave={async (markdown) => {
                if (!session?.userId) {
                  return;
                }
                await updateNodeOutline(session.userId, selectedNode.id, markdown);
                await refreshWorkspace();
              }}
            />

            <label className="block space-y-1 rounded-panel border border-app-border bg-app-bg p-3 shadow-panel">
              <span className="text-xs font-medium text-app-muted">父节点</span>
              <select
                data-testid="node-parent-select"
                className="w-full rounded-xl border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none transition focus:border-app-accent focus:ring-2 focus:ring-app-accent/20"
                value={selectedNode.parentId ?? ""}
                onChange={(event) => {
                  if (!session?.userId) {
                    return;
                  }
                  void (async () => {
                    await moveNode(session.userId, selectedNode.id, event.target.value || null);
                    await refreshWorkspace();
                  })();
                }}
              >
                <option value="">知识体系</option>
                {moveTargets.map((node) => (
                  <option key={node.id} value={node.id}>
                    {`${"·".repeat(Math.max(0, node.depth - 1))} ${node.title}`}
                  </option>
                ))}
              </select>
            </label>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <SectionTitle title="已挂载摘录" subtitle={`${feed.length} 条摘录`} />
                {feed.length > 0 ? <Badge>{feed.length}</Badge> : null}
              </div>

              {feed.length === 0 ? (
                <div className="rounded-panel border border-dashed border-app-border bg-app-surface2 p-4 text-sm text-app-muted" data-testid="detail-feed-empty">
                  当前节点还没有挂载摘录。
                </div>
              ) : null}

              {feed.map((excerpt) => (
                <article key={excerpt.id} className="space-y-3 rounded-panel border border-app-border bg-app-bg p-3 shadow-panel" data-testid="detail-excerpt-card">
                  <div>
                    <p className="text-sm leading-6 text-app-text">{excerpt.content}</p>
                    <div className="mt-2 text-xs text-app-muted">{excerpt.sourceTitle ?? excerpt.url ?? "未记录来源"}</div>
                  </div>
                  <ExcerptThoughtEditor excerpt={excerpt} onSave={saveThought} />
                  <div className="flex items-center justify-between gap-2">
                    <div className="text-xs text-app-muted" />
                    <Button
                      type="button"
                      variant="secondary"
                      className="h-8 px-3 text-xs"
                      onClick={() => void deleteExcerptCard(excerpt.id)}
                    >
                      <Trash2 size={14} />
                      删除摘录
                    </Button>
                  </div>
                  <ExcerptTagEditor excerpt={excerpt} tags={tags} onSetTags={setExcerptTags} onDeleteTag={deleteTagEntity} />
                  <label className="block space-y-1">
                    <span className="text-xs font-medium text-app-muted">挂载位置</span>
                    <select
                      data-testid="excerpt-mount-select"
                      className="w-full rounded-xl border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none transition focus:border-app-accent focus:ring-2 focus:ring-app-accent/20"
                      value={excerpt.treeNodeId ?? ""}
                      onChange={(event) => {
                        if (!session?.userId) {
                          return;
                        }
                        void (async () => {
                          await moveExcerptToNode(session.userId, excerpt.id, event.target.value || null);
                          await refreshWorkspace();
                        })();
                      }}
                    >
                      <option value="">收件箱</option>
                      {nodes
                        .filter((node) => !node.isDeleted)
                        .map((node) => (
                          <option key={node.id} value={node.id}>
                            {node.nodeTitle}
                          </option>
                        ))}
                    </select>
                  </label>
                  <Button
                    type="button"
                    variant="secondary"
                    className="h-8 px-3 text-xs"
                    onClick={() => void moveToInbox(excerpt.id)}
                  >
                    <RotateCcw size={14} />
                    移回未分类
                  </Button>
                </article>
              ))}
            </div>
          </>
        )}
      </div>
    </aside>
  );

  if (drawerFullscreen) {
    return (
      <div className="fixed inset-4 z-40 overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-2xl">
        {content}
      </div>
    );
  }

  return content;
}
