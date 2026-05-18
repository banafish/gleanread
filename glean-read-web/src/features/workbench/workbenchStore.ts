import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type {
  Excerpt,
  ExcerptTag,
  InboxFilter,
  KnowledgeTreeNode,
  RecentSearch,
  ThemeMode,
  Tag,
  WorkbenchViewState,
} from "@/shared/models";
import type { WorkspaceSnapshot } from "@/shared/models";
import { getPreference, savePreference } from "@/db/repositories/workspaceRepository";

export interface WorkbenchState extends WorkbenchViewState {
  userId: string | null;
  isHydrated: boolean;
  isLoading: boolean;
  nodes: KnowledgeTreeNode[];
  excerpts: Excerpt[];
  tags: Tag[];
  excerptTags: ExcerptTag[];
  recentSearches: RecentSearch[];
  hydrateWorkspace: (userId: string, snapshot: WorkspaceSnapshot) => void;
  setUserId: (userId: string | null) => void;
  setLoading: (value: boolean) => void;
  setSelectedNodeId: (id: string | null) => void;
  setSelectedExcerptId: (id: string | null) => void;
  setDrawerOpen: (value: boolean) => void;
  setDrawerFullscreen: (value: boolean) => void;
  setInboxFilter: (value: InboxFilter) => void;
  setSearchOpen: (value: boolean) => void;
  setSearchQuery: (value: string) => void;
  setThemeMode: (value: ThemeMode) => void;
  setLeftPanelWidth: (value: number) => void;
  setRightPanelWidth: (value: number) => void;
  setViewport: (value: WorkbenchViewState["viewport"]) => void;
  setHoveredNodeId: (value: string | null) => void;
  toggleNodeExpanded: (nodeId: string) => void;
  replaceData: (snapshot: WorkspaceSnapshot) => void;
  addRecentSearch: (query: string) => void;
  setRecentSearches: (searches: RecentSearch[]) => void;
  loadPreferences: (userId: string) => Promise<void>;
  persistPreference: (key: string, value: string) => Promise<void>;
  resetUiForUser: () => void;
}

const initialUiState = {
  selectedNodeId: null,
  selectedExcerptId: null,
  drawerOpen: true,
  drawerFullscreen: false,
  inboxFilter: "inbox" as InboxFilter,
  searchOpen: false,
  searchQuery: "",
  themeMode: "system" as ThemeMode,
  leftPanelWidth: 320,
  rightPanelWidth: 380,
  expandedNodeIds: {} as Record<string, boolean>,
  hoveredNodeId: null,
  viewport: { x: 0, y: 0, zoom: 1 },
  recentSearches: [],
};

export const useWorkbenchStore = create<WorkbenchState>()(
  persist(
    (set, get) => ({
      userId: null,
      isHydrated: false,
      isLoading: true,
      nodes: [],
      excerpts: [],
      tags: [],
      excerptTags: [],
      ...initialUiState,
      hydrateWorkspace: (userId, snapshot) => {
        const expandedNodeIds = Object.keys(get().expandedNodeIds).length
          ? get().expandedNodeIds
          : snapshot.nodes.reduce<Record<string, boolean>>((acc, node) => {
              if (node.parentId === null) {
                acc[node.id] = true;
              }
              return acc;
            }, {});
        const selectedNodeId =
          get().selectedNodeId ?? snapshot.nodes.find((node) => node.parentId !== null)?.id ?? snapshot.nodes[0]?.id ?? null;
        set({
          userId,
          isHydrated: true,
          isLoading: false,
          nodes: snapshot.nodes,
          excerpts: snapshot.excerpts,
          tags: snapshot.tags,
          excerptTags: snapshot.excerptTags,
          recentSearches: snapshot.recentSearches,
          selectedNodeId,
          expandedNodeIds,
          selectedExcerptId: get().selectedExcerptId,
          drawerOpen: selectedNodeId !== null,
        });
      },
      setUserId: (userId) => set({ userId }),
      setLoading: (value) => set({ isLoading: value }),
      setSelectedNodeId: (id) =>
        set({
          selectedNodeId: id,
          selectedExcerptId: null,
          drawerOpen: id !== null ? true : get().drawerOpen,
        }),
      setSelectedExcerptId: (id) => set({ selectedExcerptId: id }),
      setDrawerOpen: (value) => set({ drawerOpen: value }),
      setDrawerFullscreen: (value) => set({ drawerFullscreen: value }),
      setInboxFilter: (value) => set({ inboxFilter: value }),
      setSearchOpen: (value) => set({ searchOpen: value }),
      setSearchQuery: (value) => set({ searchQuery: value }),
      setThemeMode: (value) => set({ themeMode: value }),
      setLeftPanelWidth: (value) => set({ leftPanelWidth: value }),
      setRightPanelWidth: (value) => set({ rightPanelWidth: value }),
      setViewport: (value) => set({ viewport: value }),
      setHoveredNodeId: (value) => set({ hoveredNodeId: value }),
      toggleNodeExpanded: (nodeId) =>
        set((state) => ({
          expandedNodeIds: {
            ...state.expandedNodeIds,
            [nodeId]: !state.expandedNodeIds[nodeId],
          },
        })),
      replaceData: (snapshot) =>
        set({
          nodes: snapshot.nodes,
          excerpts: snapshot.excerpts,
          tags: snapshot.tags,
          excerptTags: snapshot.excerptTags,
          recentSearches: snapshot.recentSearches,
        }),
      addRecentSearch: (query) =>
        set((state) => {
          const trimmed = query.trim();
          if (!trimmed) {
            return state;
          }
          const next = [
            { id: `recent-${Date.now()}`, userId: state.userId ?? "local-user", query: trimmed, createTime: Date.now() },
            ...state.recentSearches.filter((item) => item.query !== trimmed),
          ].slice(0, 8);
          void (async () => {
            if (state.userId) {
              await savePreference(state.userId, "last-search", trimmed);
            }
          })();
          return { recentSearches: next };
        }),
      setRecentSearches: (searches) => set({ recentSearches: searches }),
      loadPreferences: async (userId) => {
        const entries = await Promise.all([
          getPreference(userId, "theme-mode"),
          getPreference(userId, "left-panel-width"),
          getPreference(userId, "right-panel-width"),
          getPreference(userId, "expanded-node-ids"),
          getPreference(userId, "viewport"),
          getPreference(userId, "inbox-filter"),
          getPreference(userId, "drawer-open"),
        ]);
        const [themeMode, leftPanelWidth, rightPanelWidth, expandedNodeIds, viewport, inboxFilter, drawerOpen] = entries;
        const nextState: Partial<WorkbenchState> = {};
        if (themeMode === "light" || themeMode === "dark" || themeMode === "system") {
          nextState.themeMode = themeMode;
        }
        if (leftPanelWidth) {
          const parsed = Number(leftPanelWidth);
          if (!Number.isNaN(parsed)) {
            nextState.leftPanelWidth = parsed;
          }
        }
        if (rightPanelWidth) {
          const parsed = Number(rightPanelWidth);
          if (!Number.isNaN(parsed)) {
            nextState.rightPanelWidth = parsed;
          }
        }
        if (expandedNodeIds) {
          try {
            nextState.expandedNodeIds = JSON.parse(expandedNodeIds) as Record<string, boolean>;
          } catch {
            // ignore malformed preference
          }
        }
        if (viewport) {
          try {
            nextState.viewport = JSON.parse(viewport) as WorkbenchViewState["viewport"];
          } catch {
            // ignore malformed preference
          }
        }
        if (inboxFilter === "inbox" || inboxFilter === "all") {
          nextState.inboxFilter = inboxFilter;
        }
        if (drawerOpen === "true" || drawerOpen === "false") {
          nextState.drawerOpen = drawerOpen === "true";
        }
        set(nextState);
      },
      persistPreference: async (key, value) => {
        const userId = get().userId;
        if (!userId) {
          return;
        }
        await savePreference(userId, key, value);
      },
      resetUiForUser: () =>
        set({
          ...initialUiState,
          isHydrated: false,
          isLoading: true,
          nodes: [],
          excerpts: [],
          tags: [],
          excerptTags: [],
          recentSearches: [],
          userId: null,
        }),
    }),
    {
      name: "glean-read-web-ui",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        selectedNodeId: state.selectedNodeId,
        selectedExcerptId: state.selectedExcerptId,
        drawerOpen: state.drawerOpen,
        drawerFullscreen: state.drawerFullscreen,
        inboxFilter: state.inboxFilter,
        searchOpen: state.searchOpen,
        searchQuery: state.searchQuery,
        themeMode: state.themeMode,
        leftPanelWidth: state.leftPanelWidth,
        rightPanelWidth: state.rightPanelWidth,
        expandedNodeIds: state.expandedNodeIds,
        hoveredNodeId: state.hoveredNodeId,
        viewport: state.viewport,
        recentSearches: state.recentSearches,
      }),
    }
  )
);
