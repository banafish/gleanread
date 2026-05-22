import { db } from "../dexie.ts";
import { createSeedData } from "../seed.ts";
import type {
  AuthSession,
  Excerpt,
  ExcerptTag,
  KnowledgeTreeNode,
  RecentSearch,
  Tag,
  WorkspacePreference,
  WorkspaceSnapshot,
} from "../../shared/models.ts";
import { createId, getSubtreeIds, isDescendant, now, sortByOrderAndTime, trimOrNull } from "../../shared/utils.ts";

const DEVICE_ID = "local-device";
const SORT_ORDER_STEP = 65_536;

async function getCurrentUserId(): Promise<string> {
  const session = await db.sessions.get("current");
  return session?.userId ?? "local-user";
}

async function ensureSeeded(userId: string): Promise<void> {
  const existingCount = await db.nodes.where("userId").equals(userId).count();
  if (existingCount > 0) {
    return;
  }
  const seed = createSeedData(userId);
  await db.transaction("rw", db.nodes, db.excerpts, db.tags, db.excerptTags, async () => {
    await db.nodes.bulkAdd(seed.nodes);
    await db.excerpts.bulkAdd(seed.excerpts);
    await db.tags.bulkAdd(seed.tags);
    await db.excerptTags.bulkAdd(seed.excerptTags);
  });
}

export async function ensureWorkspaceSeed(userId: string): Promise<void> {
  await ensureSeeded(userId);
}

export async function getWorkspaceSnapshot(
  userId: string,
  options: { seedIfEmpty?: boolean } = {}
): Promise<WorkspaceSnapshot> {
  if (options.seedIfEmpty) {
    await ensureSeeded(userId);
  }
  const [nodes, excerpts, tags, excerptTags] = await Promise.all([
    db.nodes.where("userId").equals(userId).toArray(),
    db.excerpts.where("userId").equals(userId).toArray(),
    db.tags.where("userId").equals(userId).toArray(),
    db.excerptTags.where("userId").equals(userId).toArray(),
  ]);
  const recentSearches = (await db.recentSearches.where("userId").equals(userId).sortBy("createTime")).reverse();

  return {
    nodes,
    excerpts,
    tags,
    excerptTags,
    recentSearches: recentSearches.slice(0, 8),
  };
}

export async function getCurrentSnapshot(): Promise<WorkspaceSnapshot> {
  return getWorkspaceSnapshot(await getCurrentUserId());
}

export async function savePreference(userId: string, key: string, value: string): Promise<void> {
  await db.preferences.put({
    id: `${userId}:${key}`,
    userId,
    key,
    value,
    updateTime: now(),
  });
}

export async function loadPreferences(userId: string): Promise<Record<string, string>> {
  const rows = await db.preferences.where("userId").equals(userId).toArray();
  return Object.fromEntries(rows.map((row) => [row.key, row.value]));
}

export async function addRecentSearch(userId: string, query: string): Promise<void> {
  const trimmed = query.trim();
  if (!trimmed) {
    return;
  }
  const existing = await db.recentSearches.where({ userId, query: trimmed }).first();
  if (existing) {
    await db.recentSearches.delete(existing.id);
  }
  await db.recentSearches.add({
    id: createId("search"),
    userId,
    query: trimmed,
    createTime: now(),
  });
}

export async function createChildNode(userId: string, parentId: string | null, title: string): Promise<KnowledgeTreeNode> {
  const snapshot = await getWorkspaceSnapshot(userId);
  const siblings = snapshot.nodes.filter((node) => node.parentId === parentId);
  const sortOrder = (Math.max(0, ...siblings.map((node) => node.sortOrder)) + SORT_ORDER_STEP) || SORT_ORDER_STEP;
  const node: KnowledgeTreeNode = {
    id: createId("node"),
    userId,
    parentId,
    nodeTitle: title.trim(),
    outlineMarkdown: "",
    createTime: now(),
    updateTime: now(),
    isDeleted: false,
    deviceId: DEVICE_ID,
    sortOrder,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: now(),
    lastSyncTime: null,
  };
  await db.nodes.add(node);
  return node;
}

export async function createSiblingNode(userId: string, nodeId: string, title: string): Promise<KnowledgeTreeNode | null> {
  const snapshot = await getWorkspaceSnapshot(userId);
  const sourceNode = snapshot.nodes.find((item) => item.id === nodeId && item.userId === userId);
  if (!sourceNode) {
    return null;
  }
  const timestamp = now();
  const node: KnowledgeTreeNode = {
    id: createId("node"),
    userId,
    parentId: sourceNode.parentId,
    nodeTitle: title.trim(),
    outlineMarkdown: "",
    createTime: timestamp,
    updateTime: timestamp,
    isDeleted: false,
    deviceId: DEVICE_ID,
    sortOrder: sourceNode.sortOrder + SORT_ORDER_STEP,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: timestamp,
    lastSyncTime: null,
  };
  const siblings = sortByOrderAndTime(snapshot.nodes.filter((item) => item.parentId === sourceNode.parentId));
  const currentIndex = siblings.findIndex((item) => item.id === sourceNode.id);
  const nextOrder = [...siblings];
  nextOrder.splice(currentIndex + 1, 0, node);

  await db.transaction("rw", db.nodes, async () => {
    for (let index = 0; index < nextOrder.length; index += 1) {
      const sibling = nextOrder[index];
      const sortOrder = (index + 1) * SORT_ORDER_STEP;
      if (sibling.id === node.id) {
        await db.nodes.add({ ...node, sortOrder });
        continue;
      }
      if (sibling.sortOrder === sortOrder) {
        continue;
      }
      const row = await db.nodes.get(sibling.id);
      if (!row) {
        continue;
      }
      await db.nodes.put({
        ...row,
        sortOrder,
        updateTime: timestamp,
        deviceId: DEVICE_ID,
        syncStatus: "pending",
        syncError: null,
        retryCount: 0,
        localDirtyTime: timestamp,
      });
    }
  });
  return node;
}

export async function moveNode(userId: string, nodeId: string, targetParentId: string | null): Promise<void> {
  const node = await db.nodes.get(nodeId);
  if (!node) {
    return;
  }
  const snapshot = await getWorkspaceSnapshot(userId);
  if (targetParentId === node.id || (targetParentId && isDescendant(snapshot.nodes, node.id, targetParentId))) {
    return;
  }
  const siblings = snapshot.nodes.filter((item) => item.parentId === targetParentId && item.id !== nodeId);
  const sortOrder = (Math.max(0, ...siblings.map((item) => item.sortOrder)) + SORT_ORDER_STEP) || SORT_ORDER_STEP;
  await db.nodes.put({
    ...node,
    parentId: targetParentId,
    sortOrder,
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: now(),
  });
}

export async function moveNodeToPosition(
  userId: string,
  nodeId: string,
  targetParentId: string | null,
  targetIndex: number
): Promise<boolean> {
  const snapshot = await getWorkspaceSnapshot(userId);
  const node = snapshot.nodes.find((item) => item.id === nodeId && item.userId === userId);
  if (!node || targetParentId === node.id || (targetParentId && isDescendant(snapshot.nodes, node.id, targetParentId))) {
    return false;
  }

  const siblings = sortByOrderAndTime(snapshot.nodes.filter((item) => item.parentId === targetParentId && item.id !== nodeId));
  const boundedIndex = Math.max(0, Math.min(targetIndex, siblings.length));
  const nextOrder = [...siblings];
  nextOrder.splice(boundedIndex, 0, { ...node, parentId: targetParentId });
  const timestamp = now();

  await db.transaction("rw", db.nodes, async () => {
    for (let index = 0; index < nextOrder.length; index += 1) {
      const sibling = nextOrder[index];
      const row = sibling.id === node.id ? node : await db.nodes.get(sibling.id);
      if (!row) {
        continue;
      }
      const sortOrder = (index + 1) * SORT_ORDER_STEP;
      if (row.parentId === targetParentId && row.sortOrder === sortOrder) {
        continue;
      }
      await db.nodes.put({
        ...row,
        parentId: targetParentId,
        sortOrder,
        updateTime: timestamp,
        deviceId: DEVICE_ID,
        syncStatus: "pending",
        syncError: null,
        retryCount: 0,
        localDirtyTime: timestamp,
      });
    }
  });

  return true;
}

export async function reorderNodeSibling(userId: string, nodeId: string, direction: "up" | "down"): Promise<void> {
  const snapshot = await getWorkspaceSnapshot(userId);
  const node = snapshot.nodes.find((item) => item.id === nodeId && item.userId === userId);
  if (!node) {
    return;
  }
  const siblings = sortByOrderAndTime(snapshot.nodes.filter((item) => item.parentId === node.parentId));
  const currentIndex = siblings.findIndex((item) => item.id === nodeId);
  const targetIndex = direction === "up" ? currentIndex - 1 : currentIndex + 1;
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= siblings.length) {
    return;
  }

  const nextOrder = [...siblings];
  const [moved] = nextOrder.splice(currentIndex, 1);
  nextOrder.splice(targetIndex, 0, moved);
  const timestamp = now();

  await db.transaction("rw", db.nodes, async () => {
    for (let index = 0; index < nextOrder.length; index += 1) {
      const sibling = nextOrder[index];
      const sortOrder = (index + 1) * SORT_ORDER_STEP;
      if (sibling.sortOrder === sortOrder) {
        continue;
      }
      const row = await db.nodes.get(sibling.id);
      if (!row) {
        continue;
      }
      await db.nodes.put({
        ...row,
        sortOrder,
        updateTime: timestamp,
        deviceId: DEVICE_ID,
        syncStatus: "pending",
        syncError: null,
        retryCount: 0,
        localDirtyTime: timestamp,
      });
    }
  });
}

export async function renameNode(userId: string, nodeId: string, title: string): Promise<void> {
  const node = await db.nodes.get(nodeId);
  if (!node) {
    return;
  }
  await db.nodes.put({
    ...node,
    nodeTitle: title.trim(),
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    localDirtyTime: now(),
  });
}

export async function saveOutline(userId: string, nodeId: string, outlineMarkdown: string): Promise<void> {
  const node = await db.nodes.get(nodeId);
  if (!node) {
    return;
  }
  await db.nodes.put({
    ...node,
    outlineMarkdown,
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    localDirtyTime: now(),
  });
}

export async function deleteNodeSubtree(userId: string, nodeId: string): Promise<void> {
  const snapshot = await getWorkspaceSnapshot(userId);
  const ids = getSubtreeIds(snapshot.nodes, nodeId);
  await db.transaction("rw", db.nodes, db.excerpts, db.excerptTags, async () => {
    for (const id of ids) {
      const node = await db.nodes.get(id);
      if (node) {
        await db.nodes.put({
          ...node,
          isDeleted: true,
          updateTime: now(),
          deviceId: DEVICE_ID,
          syncStatus: "pending",
          localDirtyTime: now(),
        });
      }
    }
    const affectedExcerpts = snapshot.excerpts.filter((excerpt) => excerpt.treeNodeId && ids.includes(excerpt.treeNodeId));
    for (const excerpt of affectedExcerpts) {
      await db.excerpts.put({
        ...excerpt,
        treeNodeId: null,
        updateTime: now(),
        deviceId: DEVICE_ID,
        syncStatus: "pending",
        localDirtyTime: now(),
      });
    }
    const affectedRelations = snapshot.excerptTags.filter((relation) => {
      const excerpt = snapshot.excerpts.find((item) => item.id === relation.excerptId);
      const treeNodeId = excerpt?.treeNodeId;
      return typeof treeNodeId === "string" && ids.includes(treeNodeId);
    });
    for (const relation of affectedRelations) {
      await db.excerptTags.put({
        ...relation,
        isDeleted: true,
        updateTime: now(),
        deviceId: DEVICE_ID,
        syncStatus: "pending",
        localDirtyTime: now(),
      });
    }
  });
}

export async function restoreNodeSubtree(userId: string, nodeId: string): Promise<void> {
  const snapshot = await getWorkspaceSnapshot(userId);
  const ids = getSubtreeIds(snapshot.nodes, nodeId);
  await db.transaction("rw", db.nodes, async () => {
    for (const id of ids) {
      const node = await db.nodes.get(id);
      if (node) {
        await db.nodes.put({
          ...node,
          isDeleted: false,
          updateTime: now(),
          deviceId: DEVICE_ID,
          syncStatus: "pending",
          syncError: null,
          retryCount: 0,
          localDirtyTime: now(),
        });
      }
    }
  });
}

export async function moveExcerptToNode(userId: string, excerptId: string, treeNodeId: string | null): Promise<void> {
  const excerpt = await db.excerpts.get(excerptId);
  if (!excerpt) {
    return;
  }
  await db.excerpts.put({
    ...excerpt,
    treeNodeId,
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    localDirtyTime: now(),
  });
}

export async function deleteExcerpt(userId: string, excerptId: string): Promise<void> {
  const excerpt = await db.excerpts.get(excerptId);
  if (!excerpt) {
    return;
  }
  await db.excerpts.put({
    ...excerpt,
    isDeleted: true,
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: now(),
  });
}

export async function restoreExcerpt(userId: string, excerptId: string): Promise<void> {
  const excerpt = await db.excerpts.get(excerptId);
  if (!excerpt) {
    return;
  }
  await db.excerpts.put({
    ...excerpt,
    isDeleted: false,
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: now(),
  });
}

export async function updateExcerpt(
  userId: string,
  excerptId: string,
  payload: {
    content: string;
    userThought: string | null;
    sourceTitle: string | null;
    url: string | null;
    treeNodeId: string | null;
    tagIds: string[];
  }
): Promise<void> {
  const excerpt = await db.excerpts.get(excerptId);
  if (!excerpt) {
    return;
  }
  const cleanContent = payload.content.trim();
  if (!cleanContent) {
    return;
  }
  await db.transaction("rw", db.excerpts, db.excerptTags, db.tags, async () => {
    await db.excerpts.put({
      ...excerpt,
      content: cleanContent,
      userThought: trimOrNull(payload.userThought ?? "") ?? null,
      sourceTitle: trimOrNull(payload.sourceTitle ?? "") ?? null,
      url: trimOrNull(payload.url ?? "") ?? null,
      treeNodeId: payload.treeNodeId,
      updateTime: now(),
      deviceId: DEVICE_ID,
      syncStatus: "pending",
      localDirtyTime: now(),
    });

    const currentRelations = await db.excerptTags.where({ excerptId }).toArray();
    const currentRelationMap = new Map(currentRelations.map((relation) => [relation.tagId, relation]));
    const activeRelations = currentRelations.filter((relation) => !relation.isDeleted);
    const currentTagIds = new Set(activeRelations.map((relation) => relation.tagId));
    const nextTagIds = new Set(payload.tagIds);

    for (const relation of activeRelations) {
      if (!nextTagIds.has(relation.tagId)) {
        await db.excerptTags.put({
          ...relation,
          isDeleted: true,
          updateTime: now(),
          deviceId: DEVICE_ID,
          syncStatus: "pending",
          localDirtyTime: now(),
        });
      }
    }

    for (const tagId of nextTagIds) {
      const existingRelation = currentRelationMap.get(tagId);
      if (existingRelation?.isDeleted) {
        await db.excerptTags.put({
          ...existingRelation,
          isDeleted: false,
          updateTime: now(),
          deviceId: DEVICE_ID,
          syncStatus: "pending",
          syncError: null,
          retryCount: 0,
          localDirtyTime: now(),
        });
        await updateTagHeat(userId, tagId, 1);
      } else if (!currentTagIds.has(tagId)) {
        await db.excerptTags.add({
          id: createId("excerpt-tag"),
          userId,
          excerptId,
          tagId,
          createTime: now(),
          updateTime: now(),
          isDeleted: false,
          deviceId: DEVICE_ID,
          syncStatus: "pending",
          syncError: null,
          retryCount: 0,
          localDirtyTime: now(),
          lastSyncTime: null,
        });
        await updateTagHeat(userId, tagId, 1);
      }
    }
  });
}

export async function deleteTag(userId: string, tagId: string): Promise<void> {
  const tag = await db.tags.get(tagId);
  if (!tag) {
    return;
  }
  await db.tags.put({
    ...tag,
    isDeleted: true,
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: now(),
  });
}

export async function restoreTag(userId: string, tagId: string): Promise<void> {
  const tag = await db.tags.get(tagId);
  if (!tag) {
    return;
  }
  await db.tags.put({
    ...tag,
    isDeleted: false,
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: now(),
  });
}

export async function updateNodeOutline(userId: string, nodeId: string, outlineMarkdown: string): Promise<void> {
  await saveOutline(userId, nodeId, outlineMarkdown);
}

export async function saveWorkspaceSnapshot(userId: string, snapshot: WorkspaceSnapshot): Promise<void> {
  await db.transaction("rw", db.nodes, db.excerpts, db.tags, db.excerptTags, async () => {
    await db.nodes.bulkPut(snapshot.nodes);
    await db.excerpts.bulkPut(snapshot.excerpts);
    await db.tags.bulkPut(snapshot.tags);
    await db.excerptTags.bulkPut(snapshot.excerptTags);
  });
}

export async function updateTagHeat(userId: string, tagId: string, delta = 1): Promise<void> {
  const tag = await db.tags.get(tagId);
  if (!tag) {
    return;
  }
  await db.tags.put({
    ...tag,
    heatWeight: Math.max(0, tag.heatWeight + delta),
    updateTime: now(),
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    localDirtyTime: now(),
  });
}

export async function ensureTag(userId: string, tagName: string, colorIcon: string | null = null): Promise<Tag | null> {
  const trimmed = trimOrNull(tagName);
  if (!trimmed) {
    return null;
  }
  const existing = await db.tags.where({ userId, tagName: trimmed }).first();
  if (existing) {
    if (existing.isDeleted) {
      const revived: Tag = {
        ...existing,
        isDeleted: false,
        colorIcon: existing.colorIcon ?? colorIcon,
        updateTime: now(),
        deviceId: DEVICE_ID,
        syncStatus: "pending",
        syncError: null,
        retryCount: 0,
        localDirtyTime: now(),
      };
      await db.tags.put(revived);
      return revived;
    }
    return existing;
  }
  const tag: Tag = {
    id: createId("tag"),
    userId,
    tagName: trimmed,
    colorIcon,
    heatWeight: 1,
    createTime: now(),
    updateTime: now(),
    isDeleted: false,
    deviceId: DEVICE_ID,
    syncStatus: "pending",
    syncError: null,
    retryCount: 0,
    localDirtyTime: now(),
    lastSyncTime: null,
  };
  await db.tags.add(tag);
  return tag;
}

export async function setCurrentSession(session: AuthSession | null): Promise<void> {
  if (!session) {
    await db.sessions.clear();
    return;
  }
  await db.sessions.put({ ...session, id: "current" });
}

export async function getCurrentSession(): Promise<AuthSession | null> {
  const session = await db.sessions.get("current");
  return session
    ? {
        userId: session.userId,
        email: session.email,
        avatarUrl: session.avatarUrl ?? null,
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        provider: session.provider,
      }
    : null;
}

export async function upsertPreference(userId: string, key: string, value: string): Promise<void> {
  await db.preferences.put({ id: `${userId}:${key}`, userId, key, value, updateTime: now() });
}

export async function getPreference(userId: string, key: string): Promise<string | null> {
  const row = await db.preferences.get(`${userId}:${key}`);
  return row?.value ?? null;
}

export async function loadAllUserData(userId: string): Promise<WorkspaceSnapshot> {
  return getWorkspaceSnapshot(userId);
}

export async function ensureSessionSeed(session: AuthSession): Promise<void> {
  await setCurrentSession(session);
  if (session.provider === "local") {
    await ensureSeeded(session.userId);
  }
}
