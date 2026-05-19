import { expect, type Locator, type Page } from "@playwright/test";

export interface TestAccount {
  email: string;
  password: string;
}

const defaultPassword = process.env.GLEANREAD_E2E_PASSWORD ?? "gleanread";

export const seededUser: TestAccount = {
  email: process.env.GLEANREAD_E2E_SEEDED_EMAIL ?? "test@qq.com",
  password: process.env.GLEANREAD_E2E_SEEDED_PASSWORD ?? defaultPassword,
};

export const emptyUser: TestAccount = {
  email: process.env.GLEANREAD_E2E_EMPTY_EMAIL ?? "test2@qq.com",
  password: process.env.GLEANREAD_E2E_EMPTY_PASSWORD ?? defaultPassword,
};

export function testPrefix(label: string): string {
  return `e2e-${label}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

export async function loginWithPassword(page: Page, account: TestAccount = seededUser): Promise<void> {
  await page.goto("/login");
  await page.locator('input[type="email"]').fill(account.email);
  await page.locator('input[type="password"]').first().fill(account.password);
  await page.getByTestId("login-submit").click();
  await Promise.race([
    page.waitForURL(/\/app(?:\/)?$/, { timeout: 30_000 }),
    page.getByTestId("auth-error").waitFor({ state: "visible", timeout: 30_000 }).then(async () => {
      throw new Error(`登录失败：${await page.getByTestId("auth-error").innerText()}`);
    }),
  ]);
  await waitForWorkbench(page);
}

export async function signOut(page: Page): Promise<void> {
  await page.getByTestId("account-menu-trigger").click();
  await Promise.all([
    page.waitForURL(/\/login(?:\/)?$/, { timeout: 15_000 }),
    page.getByRole("button", { name: "退出登录" }).click(),
  ]);
}

export async function waitForWorkbench(page: Page): Promise<void> {
  await expect(page.getByTestId("app-shell")).toBeVisible();
  await expect(page.getByTestId("inbox-sidebar")).toBeVisible();
  await expect(page.getByTestId("knowledge-tree-canvas")).toBeAttached();
}

export async function waitForSeededWorkspace(page: Page): Promise<void> {
  await waitForWorkbench(page);
  await expect
    .poll(async () => page.locator('[data-testid^="tree-node-"]').count(), { timeout: 30_000 })
    .toBeGreaterThan(1);
}

export async function expectNoSeedExampleContent(page: Page): Promise<void> {
  await expect(page.getByText("前端工程化")).toHaveCount(0);
  await expect(page.getByText("React Flow 适合把关系结构做成可视化画布。")).toHaveCount(0);
  await expect(page.getByText("未分类摘录应该始终留在 Inbox 中，等待拖拽整理。")).toHaveCount(0);
}

export function firstRealNode(page: Page): Locator {
  return page.locator('[data-testid^="tree-node-"]').nth(1);
}

export async function selectFirstRealNode(page: Page): Promise<Locator> {
  const node = firstRealNode(page);
  await expect(node).toBeVisible();
  await node.click();
  await expect(page.getByTestId("detail-drawer")).toBeVisible();
  return node;
}

export async function openSearch(page: Page): Promise<void> {
  await page.getByTestId("global-search-trigger").click();
  await expect(page.getByTestId("search-dialog")).toBeVisible();
}

export async function seedWorkspace(page: Page, prefix: string): Promise<void> {
  await page.evaluate(async (seedPrefix) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });

    const getCurrentSession = async () =>
      new Promise<{ userId: string; email: string }>((resolve, reject) => {
        const transaction = db.transaction(["sessions"], "readonly");
        const request = transaction.objectStore("sessions").get("current");
        request.onerror = () => reject(request.error);
        request.onsuccess = () => {
          if (!request.result) {
            reject(new Error("Missing current session"));
            return;
          }
          resolve(request.result as { userId: string; email: string });
        };
      });

    const session = await getCurrentSession();
    const timestamp = Date.now();
    const deviceId = "e2e-device";
    const rootId = `${seedPrefix}-root`;
    const childId = `${seedPrefix}-child`;
    const emptyId = `${seedPrefix}-empty`;
    const tagId = `${seedPrefix}-tag`;
    const inboxExcerptId = `${seedPrefix}-inbox-excerpt`;
    const mountedExcerptId = `${seedPrefix}-mounted-excerpt`;
    const fallbackExcerptId = `${seedPrefix}-fallback-excerpt`;

    const syncFields = {
      userId: session.userId,
      isDeleted: false,
      deviceId,
      syncStatus: "synced",
      syncError: null,
      retryCount: 0,
      localDirtyTime: null,
      lastSyncTime: timestamp,
    };

    const nodes = [
      {
        ...syncFields,
        id: rootId,
        parentId: null,
        nodeTitle: `${seedPrefix} Root`,
        outlineMarkdown: "# Existing outline",
        createTime: timestamp - 6_000,
        updateTime: timestamp - 6_000,
        sortOrder: 65_536,
      },
      {
        ...syncFields,
        id: childId,
        parentId: rootId,
        nodeTitle: `${seedPrefix} Child`,
        outlineMarkdown: "",
        createTime: timestamp - 5_000,
        updateTime: timestamp - 5_000,
        sortOrder: 65_536,
      },
      {
        ...syncFields,
        id: emptyId,
        parentId: rootId,
        nodeTitle: `${seedPrefix} Empty`,
        outlineMarkdown: "",
        createTime: timestamp - 4_000,
        updateTime: timestamp - 4_000,
        sortOrder: 131_072,
      },
    ];

    const tags = [
      {
        ...syncFields,
        id: tagId,
        tagName: `${seedPrefix}-tag`,
        colorIcon: "●",
        heatWeight: 3,
        createTime: timestamp - 3_000,
        updateTime: timestamp - 3_000,
      },
    ];

    const excerpts = [
      {
        ...syncFields,
        id: inboxExcerptId,
        content: `${seedPrefix} inbox excerpt waiting for mount`,
        url: null,
        sourceTitle: `${seedPrefix} Inbox Source`,
        userThought: null,
        treeNodeId: null,
        createTime: timestamp - 3_000,
        updateTime: timestamp - 3_000,
      },
      {
        ...syncFields,
        id: mountedExcerptId,
        content: `${seedPrefix} mounted excerpt for detail drawer`,
        url: "https://example.com/mounted",
        sourceTitle: `${seedPrefix} Mounted Source`,
        userThought: `${seedPrefix} initial thought`,
        treeNodeId: childId,
        createTime: timestamp - 2_000,
        updateTime: timestamp - 2_000,
      },
      {
        ...syncFields,
        id: fallbackExcerptId,
        content: `${seedPrefix} url fallback excerpt`,
        url: "https://example.com/fallback",
        sourceTitle: null,
        userThought: null,
        treeNodeId: null,
        createTime: timestamp - 1_000,
        updateTime: timestamp - 1_000,
      },
    ];

    const excerptTags = [
      {
        ...syncFields,
        id: `${seedPrefix}-excerpt-tag`,
        excerptId: mountedExcerptId,
        tagId,
        createTime: timestamp - 1_000,
        updateTime: timestamp - 1_000,
      },
    ];

    await new Promise<void>((resolve, reject) => {
      const transaction = db.transaction(["nodes", "excerpts", "tags", "excerptTags"], "readwrite");
      transaction.onerror = () => reject(transaction.error);
      transaction.oncomplete = () => resolve();
      for (const node of nodes) {
        transaction.objectStore("nodes").put(node);
      }
      for (const tag of tags) {
        transaction.objectStore("tags").put(tag);
      }
      for (const excerpt of excerpts) {
        transaction.objectStore("excerpts").put(excerpt);
      }
      for (const relation of excerptTags) {
        transaction.objectStore("excerptTags").put(relation);
      }
    });

    db.close();
  }, prefix);

  await page.reload();
  await waitForSeededWorkspace(page);
  await expect(page.getByTestId(`tree-node-${prefix}-root`)).toBeAttached();
}

export async function cleanupWorkspace(page: Page, prefix: string): Promise<void> {
  await page.evaluate(async (seedPrefix) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    await new Promise<void>((resolve, reject) => {
      const transaction = db.transaction(["nodes", "excerpts", "tags", "excerptTags", "preferences", "recentSearches"], "readwrite");
      transaction.onerror = () => reject(transaction.error);
      transaction.oncomplete = () => resolve();
      for (const storeName of ["nodes", "excerpts", "tags", "excerptTags", "preferences", "recentSearches"]) {
        const store = transaction.objectStore(storeName);
        const request = store.openCursor();
        request.onsuccess = () => {
          const cursor = request.result;
          if (!cursor) {
            return;
          }
          const value = cursor.value as { id?: string; query?: string; key?: string };
          if (value.id?.includes(seedPrefix) || value.query?.includes(seedPrefix) || value.key?.includes(seedPrefix)) {
            cursor.delete();
          }
          cursor.continue();
        };
      }
    });
    db.close();
  }, prefix);
}
