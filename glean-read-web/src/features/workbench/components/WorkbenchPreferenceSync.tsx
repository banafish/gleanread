import { useEffect, useMemo } from "react";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";

export function WorkbenchPreferenceSync() {
  const userId = useWorkbenchStore((state) => state.userId);
  const themeMode = useWorkbenchStore((state) => state.themeMode);
  const leftPanelWidth = useWorkbenchStore((state) => state.leftPanelWidth);
  const rightPanelWidth = useWorkbenchStore((state) => state.rightPanelWidth);
  const expandedNodeIds = useWorkbenchStore((state) => state.expandedNodeIds);
  const viewport = useWorkbenchStore((state) => state.viewport);
  const inboxFilter = useWorkbenchStore((state) => state.inboxFilter);
  const drawerOpen = useWorkbenchStore((state) => state.drawerOpen);
  const persistPreference = useWorkbenchStore((state) => state.persistPreference);
  const expandedJson = useMemo(() => JSON.stringify(expandedNodeIds), [expandedNodeIds]);
  const viewportJson = useMemo(() => JSON.stringify(viewport), [viewport]);

  useEffect(() => {
    if (!userId) {
      return;
    }
    void persistPreference("theme-mode", themeMode);
  }, [persistPreference, themeMode, userId]);

  useEffect(() => {
    if (!userId) {
      return;
    }
    void persistPreference("left-panel-width", String(leftPanelWidth));
  }, [leftPanelWidth, persistPreference, userId]);

  useEffect(() => {
    if (!userId) {
      return;
    }
    void persistPreference("right-panel-width", String(rightPanelWidth));
  }, [persistPreference, rightPanelWidth, userId]);

  useEffect(() => {
    if (!userId) {
      return;
    }
    void persistPreference("expanded-node-ids", expandedJson);
  }, [expandedJson, persistPreference, userId]);

  useEffect(() => {
    if (!userId) {
      return;
    }
    void persistPreference("viewport", viewportJson);
  }, [persistPreference, userId, viewportJson]);

  useEffect(() => {
    if (!userId) {
      return;
    }
    void persistPreference("inbox-filter", inboxFilter);
  }, [inboxFilter, persistPreference, userId]);

  useEffect(() => {
    if (!userId) {
      return;
    }
    void persistPreference("drawer-open", String(drawerOpen));
  }, [drawerOpen, persistPreference, userId]);

  return null;
}
