import { useEffect, useMemo, type ReactNode } from "react";
import { ReactFlowProvider } from "reactflow";
import { CheckCircle2, GitBranch, Monitor, Route } from "lucide-react";
import { WorkbenchTopBar } from "@/features/workbench/components/WorkbenchTopBar";
import { InboxSidebar } from "@/features/inbox/components/InboxSidebar";
import { KnowledgeTreeGraph } from "@/features/knowledge-tree/components/KnowledgeTreeGraph";
import { DetailDrawer } from "@/features/detail-drawer/components/DetailDrawer";
import { TreeNodeCard } from "@/features/knowledge-tree/components/TreeNodeCard";
import { buildKnowledgeGraph } from "@/features/knowledge-tree/treeAdapters";
import { resolveLoginRoute, resolveProtectedRoute } from "@/app/routes/routePolicy";
import { previewSnapshot } from "@/app/previews/previewData";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";
import { Badge, SectionTitle } from "@/shared/components";
import type { KnowledgeGraphNodeData } from "@/features/knowledge-tree/treeAdapters";

function PreviewStateLoader() {
  useEffect(() => {
    useWorkbenchStore.setState({
      userId: "preview-user",
      isHydrated: true,
      isLoading: false,
      nodes: previewSnapshot.nodes,
      excerpts: previewSnapshot.excerpts,
      tags: previewSnapshot.tags,
      excerptTags: previewSnapshot.excerptTags,
      recentSearches: previewSnapshot.recentSearches,
      selectedNodeId: "node-offline",
      selectedExcerptId: "excerpt-offline",
      drawerOpen: true,
      drawerFullscreen: false,
      inboxFilter: "inbox",
      searchOpen: false,
      searchQuery: "",
      trashOpen: false,
      themeMode: "dark",
      leftPanelWidth: 280,
      rightPanelWidth: 400,
      expandedNodeIds: {
        "node-product": true,
        "node-state": true,
      },
      hoveredNodeId: "node-offline",
      viewport: { x: 0, y: 0, zoom: 1 },
    });
  }, []);

  return null;
}

function PreviewSection({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-app-border bg-app-surface p-4 shadow-panel">
      <div className="mb-4 flex items-start justify-between gap-3">
        <SectionTitle title={title} subtitle={subtitle} />
        <Badge>Preview</Badge>
      </div>
      {children}
    </section>
  );
}

function RoutePreviewGrid() {
  const routeStates = [
    { path: "/", label: "Landing", decision: { kind: "render" as const } },
    { path: "/login", label: "Login", decision: resolveLoginRoute(false, false) },
    { path: "/login", label: "Logged in", decision: resolveLoginRoute(true, false) },
    { path: "/app", label: "Loading", decision: resolveProtectedRoute(false, true) },
    { path: "/app", label: "Guest", decision: resolveProtectedRoute(false, false) },
    { path: "/app", label: "Session", decision: resolveProtectedRoute(true, false) },
  ];

  return (
    <div className="grid gap-3 md:grid-cols-3">
      {routeStates.map((item) => (
        <div key={`${item.path}-${item.label}`} className="rounded-panel border border-app-border bg-app-bg p-3">
          <div className="flex items-center gap-2 text-sm font-semibold text-app-text">
            <Route size={15} />
            {item.path}
          </div>
          <div className="mt-2 text-xs text-app-muted">{item.label}</div>
          <div className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-app-surface2 px-2 py-1 text-xs text-app-muted">
            <CheckCircle2 size={13} />
            {item.decision.kind === "redirect" ? `redirect ${item.decision.to}` : item.decision.kind}
          </div>
        </div>
      ))}
    </div>
  );
}

function NodeCardPreview() {
  const graph = useMemo(
    () =>
      buildKnowledgeGraph(previewSnapshot, { "node-product": true, "node-state": true }, {
        selectedNodeId: "node-offline",
        hoveredNodeId: "node-offline",
        editingNodeId: null,
        draggedNodeId: null,
        nodeDropPreview: null,
        onSelect: () => undefined,
        onToggleExpanded: () => undefined,
        onStartEditing: () => undefined,
        onCancelEditing: () => undefined,
        onCommitTitle: () => undefined,
      }),
    []
  );
  const node = graph.nodes.find((item) => item.id === "node-offline")!;
  const props = {
    id: node.id,
    type: node.type,
    data: node.data,
    selected: true,
    isConnectable: false,
    xPos: node.position.x,
    yPos: node.position.y,
    zIndex: 1,
    dragging: false,
  } as Parameters<typeof TreeNodeCard>[0] & { data: KnowledgeGraphNodeData };

  return (
    <ReactFlowProvider>
      <div className="flex min-h-36 items-center justify-center rounded-panel border border-dashed border-app-border bg-app-bg p-6">
        <TreeNodeCard {...props} />
      </div>
    </ReactFlowProvider>
  );
}

function WorkbenchPreviewFrame() {
  return (
    <div className="h-[680px] overflow-hidden rounded-2xl border border-app-border bg-app-bg">
      <WorkbenchTopBar syncMessage="本地优先 Preview" />
      <div className="grid h-[calc(680px-3.5rem)] min-h-0 grid-cols-[280px_minmax(0,1fr)_400px]">
        <InboxSidebar />
        <KnowledgeTreeGraph />
        <DetailDrawer />
      </div>
    </div>
  );
}

export function PreviewRoute() {
  return (
    <main className="min-h-screen space-y-6 bg-app-bg p-6 text-app-text">
      <PreviewStateLoader />
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-app-accent text-white">
          <Monitor size={20} />
        </div>
        <div>
          <h1 className="text-xl font-semibold">GleanRead Web Preview</h1>
          <p className="text-sm text-app-muted">关键路由、工作台骨架、节点、收件箱与详情抽屉的静态验收入口。</p>
        </div>
      </div>

      <PreviewSection title="关键路由" subtitle="登录守卫、登录页回跳和工作台加载状态">
        <RoutePreviewGrid />
      </PreviewSection>

      <PreviewSection title="画布节点" subtitle="选中、高亮、大纲和摘录数量状态">
        <NodeCardPreview />
      </PreviewSection>

      <PreviewSection title="工作台骨架" subtitle="三栏布局、收件箱、知识树画布和右侧详情抽屉">
        <div className="mb-3 inline-flex items-center gap-2 text-xs text-app-muted">
          <GitBranch size={14} />
          使用固定样例数据，操作按钮不会写入真实数据库。
        </div>
        <WorkbenchPreviewFrame />
      </PreviewSection>
    </main>
  );
}
