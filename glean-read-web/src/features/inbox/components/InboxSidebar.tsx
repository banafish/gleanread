import { useDraggable } from "@dnd-kit/core";
import { GripVertical, Inbox, ListFilter, RotateCcw, Tag, Trash2 } from "lucide-react";
import { deleteExcerpt, moveExcerptToNode } from "@/db/repositories/workspaceRepository";
import { useAuth } from "@/app/providers/AuthProvider";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { getInboxExcerpts } from "@/features/workbench/workbenchSelectors";
import { Badge, Button, SectionTitle } from "@/shared/components";
import type { ExcerptViewModel, WorkspaceSnapshot } from "@/shared/models";
import { cx } from "@/shared/utils";

export function InboxSidebar({ collapsed = false }: { collapsed?: boolean }) {
  const { session, refreshWorkspace } = useAuth();
  const nodes = useWorkbenchStore((state) => state.nodes);
  const excerpts = useWorkbenchStore((state) => state.excerpts);
  const tags = useWorkbenchStore((state) => state.tags);
  const excerptTags = useWorkbenchStore((state) => state.excerptTags);
  const recentSearches = useWorkbenchStore((state) => state.recentSearches);
  const inboxFilter = useWorkbenchStore((state) => state.inboxFilter);
  const setInboxFilter = useWorkbenchStore((state) => state.setInboxFilter);
  const setSelectedNodeId = useWorkbenchStore((state) => state.setSelectedNodeId);
  const setSelectedExcerptId = useWorkbenchStore((state) => state.setSelectedExcerptId);
  const setTrashOpen = useWorkbenchStore((state) => state.setTrashOpen);

  const snapshot: WorkspaceSnapshot = { nodes, excerpts, tags, excerptTags, recentSearches };
  const items = getInboxExcerpts(snapshot, inboxFilter);
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));

  const handleMoveToInbox = async (excerptId: string) => {
    if (!session?.userId) {
      return;
    }
    await moveExcerptToNode(session.userId, excerptId, null);
    await refreshWorkspace();
  };

  const handleDeleteExcerpt = async (excerptId: string) => {
    if (!session?.userId) {
      return;
    }
    await deleteExcerpt(session.userId, excerptId);
    if (useWorkbenchStore.getState().selectedExcerptId === excerptId) {
      setSelectedExcerptId(null);
    }
    await refreshWorkspace();
  };

  if (collapsed) {
    return (
      <aside className="flex h-full flex-col items-center border-r border-app-border bg-app-surface py-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-app-surface2 text-app-muted">
          <Inbox size={18} />
        </div>
        <div className="mt-3 text-xs font-semibold text-app-muted">{items.length}</div>
        <button
          type="button"
          className="mt-auto inline-flex h-10 w-10 items-center justify-center rounded-xl border border-app-border bg-app-surface2 text-app-muted transition hover:text-app-text"
          title="垃圾篓"
          aria-label="打开垃圾篓"
          onClick={() => setTrashOpen(true)}
        >
          <Trash2 size={16} />
        </button>
      </aside>
    );
  }

  return (
    <aside className="flex h-full min-h-0 flex-col border-r border-app-border bg-app-surface">
      <div className="border-b border-app-border px-4 py-4">
        <div className="flex items-start justify-between gap-3">
          <SectionTitle title="摘录收件箱" subtitle="未分类摘录会在这里等待整理" />
          <Badge>{items.length}</Badge>
        </div>
        <div className="mt-4 grid grid-cols-2 gap-2">
          <Button
            type="button"
            variant={inboxFilter === "inbox" ? "primary" : "secondary"}
            className="px-3"
            onClick={() => setInboxFilter("inbox")}
          >
            <Inbox size={15} />
            未挂载
          </Button>
          <Button
            type="button"
            variant={inboxFilter === "all" ? "primary" : "secondary"}
            className="px-3"
            onClick={() => setInboxFilter("all")}
          >
            <ListFilter size={15} />
            全部
          </Button>
        </div>
      </div>

      <div className="min-h-0 flex-1 space-y-3 overflow-auto p-3">
        {items.length === 0 ? (
          <div className="rounded-panel border border-dashed border-app-border bg-app-surface2 p-4 text-sm text-app-muted">
            当前没有待整理摘录。
          </div>
        ) : null}

        {items.map((excerpt) => (
          <ExcerptCard
            key={excerpt.id}
            excerpt={excerpt}
            mountedNodeTitle={excerpt.treeNodeId ? nodeMap.get(excerpt.treeNodeId)?.nodeTitle ?? "已挂载" : null}
            onDelete={() => void handleDeleteExcerpt(excerpt.id)}
            onMoveToInbox={excerpt.treeNodeId ? () => void handleMoveToInbox(excerpt.id) : undefined}
            onSelect={() => {
              setSelectedExcerptId(excerpt.id);
              if (excerpt.treeNodeId) {
                setSelectedNodeId(excerpt.treeNodeId);
              }
            }}
          />
        ))}
      </div>
      <div className="border-t border-app-border p-3">
        <Button type="button" variant="secondary" className="h-10 w-full" onClick={() => setTrashOpen(true)}>
          <Trash2 size={16} />
          垃圾篓
        </Button>
      </div>
    </aside>
  );
}

function ExcerptCard({
  excerpt,
  mountedNodeTitle,
  onSelect,
  onDelete,
  onMoveToInbox,
}: {
  excerpt: ExcerptViewModel;
  mountedNodeTitle: string | null;
  onSelect: () => void;
  onDelete: () => void;
  onMoveToInbox?: () => void;
}) {
  const { attributes, isDragging, listeners, setActivatorNodeRef, setNodeRef, transform } = useDraggable({
    id: excerpt.id,
    data: {
      type: "excerpt",
      excerptId: excerpt.id,
    },
  });

  const style = transform
    ? {
        transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
      }
    : undefined;

  return (
    <article
      ref={setNodeRef}
      style={style}
      className={cx(
        "rounded-panel border border-app-border bg-app-bg p-3 shadow-panel transition",
        "hover:border-app-accent/60 hover:bg-app-surface",
        isDragging && "opacity-50 shadow-2xl ring-2 ring-app-accent/20"
      )}
      onClick={onSelect}
    >
      <div className="flex items-start justify-between gap-2">
        <button
          ref={setActivatorNodeRef}
          type="button"
          className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-app-muted transition hover:bg-app-surface2 hover:text-app-text active:cursor-grabbing"
          title="拖拽摘录"
          aria-label="拖拽摘录"
          onClick={(event) => event.stopPropagation()}
          style={{ touchAction: "none" }}
          {...listeners}
          {...attributes}
        >
          <GripVertical size={14} />
        </button>
        <button
          type="button"
          className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-app-muted hover:bg-app-surface2 hover:text-app-danger"
          title="删除摘录"
          aria-label="删除摘录"
          onClick={(event) => {
            event.stopPropagation();
            onDelete();
          }}
        >
          <Trash2 size={14} />
        </button>
      </div>

      <p className="mt-3 line-clamp-4 text-sm leading-6 text-app-text">{excerpt.content}</p>
      <div className="mt-3 text-xs text-app-muted">{excerpt.sourceTitle ?? excerpt.url ?? "未记录来源"}</div>

      <div className="mt-3 flex items-center justify-between gap-2">
        <div className="text-xs text-app-muted" />
        {onMoveToInbox ? (
          <button
            type="button"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-app-muted hover:text-app-text"
            onClick={(event) => {
              event.stopPropagation();
              onMoveToInbox();
            }}
          >
            <RotateCcw size={13} />
            移回未分类
          </button>
        ) : null}
      </div>

      {excerpt.userThought ? (
        <div className="mt-2 rounded-lg bg-app-surface2 px-3 py-2 text-xs leading-5 text-app-muted">{excerpt.userThought}</div>
      ) : null}

      <div className="mt-3 flex flex-wrap items-center gap-1.5">
        {excerpt.tags.map((tag) => (
          <Badge key={tag.id} className="gap-1">
            <span>{tag.colorIcon ?? <Tag size={11} />}</span>
            {tag.tagName}
          </Badge>
        ))}
        {mountedNodeTitle ? <Badge>{mountedNodeTitle}</Badge> : null}
      </div>
    </article>
  );
}
