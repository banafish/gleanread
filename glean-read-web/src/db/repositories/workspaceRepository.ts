import { db } from "@/db/dexie";
import { createSeedData } from "@/db/seed";
import type {
  AuthSession,
  Excerpt,
  ExcerptTag,
  KnowledgeTreeNode,
  RecentSearch,
  Tag,
  WorkspacePreference,
  WorkspaceSnapshot,
} from "@/shared/models";
import { createId, getSubtreeIds, now, trimOrNull } from "@/shared/utils";

const DEVICE_ID = "local-device";

async function getCurrentUserId(): Promise<string> {
  const session = await db.sessions.orderBy("email").first();
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

export async function getWorkspaceSnapshot(userId: string): Promise<WorkspaceSnapshot> {
  await ensureSeeded(userId);
  const [nodes, excerpts, tags, excerptTags] = await Promise.all([
    db.nodes.where("userId").equals(userId).and((node) => !node.isDeleted).toArray(),
    db.excerpts.where("userId").equals(userId).and((excerpt) => !excerpt.isDeleted).toArray(),
    db.tags.where("userId").equals(userId).and((tag) => !tag.isDeleted).toArray(),
    db.excerptTags.where("userId").equals(userId).and((relation) => !relation.isDeleted).toArray(),
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
  const sortOrder = (Math.max(0, ...siblings.map((node) => node.sortOrder)) + 65_536) || 65_536;
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
  const node = await db.nodes.get(nodeId);
  if (!node) {
    return null;
  }
  return createChildNode(userId, node.parentId, title);
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
  await db.transaction("rw", db.excerpts, db.excerptTags, async () => {
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
    const currentTagIds = new Set(currentRelations.map((relation) => relation.tagId));
    const nextTagIds = new Set(payload.tagIds);

    for (const relation of currentRelations) {
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
      if (!currentTagIds.has(tagId)) {
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
  return session ? { userId: session.userId, email: session.email, accessToken: session.accessToken, refreshToken: session.refreshToken, provider: session.provider } : null;
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
  await ensureSeeded(session.userId);
}
