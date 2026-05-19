import { expect, test, type Page } from "@playwright/test";
import {
  cleanupWorkspace,
  emptyUser,
  expectNoSeedExampleContent,
  firstRealNode,
  loginWithPassword,
  openSearch,
  seededUser,
  seedWorkspace,
  signOut,
  testPrefix,
  waitForSeededWorkspace,
  waitForWorkbench,
} from "./fixtures";

function nodeByTitle(page: Page, title: string) {
  return page.locator('[data-testid^="tree-node-"]', { hasText: title }).first();
}

async function loginAndSeed(page: Page, title: string): Promise<string> {
  const prefix = testPrefix(title.replace(/[^a-z0-9]+/gi, "-").toLowerCase());
  await loginWithPassword(page, seededUser);
  await seedWorkspace(page, prefix);
  return prefix;
}

async function getMountedExcerptThought(page: Page, prefix: string): Promise<string | null> {
  return page.evaluate(async (excerptId) => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("glean-read-web");
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    try {
      return await new Promise<string | null>((resolve, reject) => {
        const transaction = db.transaction(["excerpts"], "readonly");
        const request = transaction.objectStore("excerpts").get(excerptId);
        request.onerror = () => reject(request.error);
        request.onsuccess = () => {
          const result = request.result as { userThought?: string | null } | undefined;
          resolve(result?.userThought ?? null);
        };
      });
    } finally {
      db.close();
    }
  }, `${prefix}-mounted-excerpt`);
}

test("E2E-01 未登录访问 /app 会跳转登录页", async ({ page }) => {
  await page.goto("/app");
  await page.waitForURL(/\/login(?:\/)?$/);
  await expect(page.getByTestId("app-shell")).toHaveCount(0);
  await expect(page.getByText("欢迎回来，继续你的知识构建")).toBeVisible();
});

test("E2E-02 登录后进入工作台", async ({ page }) => {
  await loginWithPassword(page, seededUser);
  await waitForWorkbench(page);
  await expect(page.getByTestId("global-search-trigger")).toBeVisible();
  await expect(page.getByTestId("inbox-sidebar")).toBeVisible();
  await expect(page.getByTestId("knowledge-tree-canvas")).toBeVisible();
});

test("E2E-03 已登录用户访问 /login 自动回到 /app", async ({ page }) => {
  await loginWithPassword(page, seededUser);
  await page.goto("/login");
  await page.waitForURL(/\/app(?:\/)?$/);
  await expect(page.getByTestId("app-shell")).toBeVisible();
});

test("E2E-04 test@qq.com 能加载云端示例数据", async ({ page }) => {
  await loginWithPassword(page, seededUser);
  await waitForSeededWorkspace(page);
  await expect(firstRealNode(page)).toBeVisible();
});

test("E2E-05 test2@qq.com 保持真实空态", async ({ page }) => {
  await loginWithPassword(page, emptyUser);
  await waitForWorkbench(page);
  await expect(page.getByTestId("inbox-empty")).toBeVisible();
  await expect(page.locator('[data-testid^="tree-node-"]')).toHaveCount(1);
  await expectNoSeedExampleContent(page);
});

test("E2E-06 两个账号不会继承彼此的工作台状态或数据", async ({ page }) => {
  const prefix = await loginAndSeed(page, "account-isolation");
  try {
    await openSearch(page);
    await page.getByPlaceholder("搜索节点、摘录或 #标签").fill(prefix);
    await page.getByTestId("search-dialog").getByRole("button").filter({ hasText: `${prefix} Root` }).first().click();
    await signOut(page);

    await loginWithPassword(page, emptyUser);
    await waitForWorkbench(page);
    await expect(page.getByText(`${prefix} Root`)).toHaveCount(0);
    await expect(page.getByTestId("inbox-empty")).toBeVisible();
  } finally {
    await page.keyboard.press("Escape").catch(() => undefined);
    if (page.url().includes("/app")) {
      await signOut(page).catch(() => undefined);
    }
    await loginWithPassword(page, seededUser).catch(() => undefined);
    await cleanupWorkspace(page, prefix).catch(() => undefined);
    await signOut(page).catch(() => undefined);
  }
});

test.describe("登录后的工作台功能", () => {
  let prefix = "";

  test.afterEach(async ({ page }) => {
    if (prefix) {
      await cleanupWorkspace(page, prefix).catch(() => undefined);
      prefix = "";
    }
  });

  test("E2E-07 收件箱筛选区分未挂载和全部摘录", async ({ page }) => {
    prefix = await loginAndSeed(page, "inbox-filter");

    await expect(page.getByTestId("excerpt-card").filter({ hasText: `${prefix} mounted excerpt` })).toHaveCount(0);
    await expect(page.getByTestId("excerpt-card").filter({ hasText: `${prefix} inbox excerpt` })).toHaveCount(1);
    await page.getByTestId("inbox-filter-all").click();
    await expect(page.getByTestId("excerpt-card").filter({ hasText: `${prefix} mounted excerpt` })).toHaveCount(1);
  });

  test("E2E-08 摘录卡片展示正文、来源、思考和标签兜底", async ({ page }) => {
    prefix = await loginAndSeed(page, "excerpt-card");
    await page.getByTestId("inbox-filter-all").click();

    await expect(page.getByText(`${prefix} inbox excerpt waiting for mount`)).toBeVisible();
    await expect(page.getByText(`${prefix} Inbox Source`)).toBeVisible();
    await expect(page.getByText("https://example.com/fallback")).toBeVisible();
    await expect(page.getByText("暂无思考").first()).toBeVisible();
    await expect(page.getByTestId("inbox-sidebar").getByText(`${prefix}-tag`)).toBeVisible();
  });

  test("E2E-09 摘录可以拖拽挂载到知识树节点", async ({ page }) => {
    prefix = await loginAndSeed(page, "drag-mount");
    const beforeCount = await page.getByTestId("excerpt-card").count();
    const card = page.getByTestId("excerpt-card").filter({ hasText: `${prefix} inbox excerpt` });
    const target = nodeByTitle(page, `${prefix} Child`);

    await card.getByTestId("excerpt-drag-handle").dragTo(target);
    await expect.poll(async () => page.getByTestId("excerpt-card").count()).toBe(beforeCount - 1);
    await expect(target.getByTestId(`tree-node-excerpt-count-${prefix}-child`)).toBeVisible();
  });

  test("E2E-10 摘录拖拽到无效区域时保持原挂载状态", async ({ page }) => {
    prefix = await loginAndSeed(page, "invalid-drop");
    const beforeCount = await page.getByTestId("excerpt-card").count();
    const card = page.getByTestId("excerpt-card").filter({ hasText: `${prefix} inbox excerpt` });
    await card.getByTestId("excerpt-drag-handle").dragTo(page.getByTestId("knowledge-tree-canvas"), {
      targetPosition: { x: 20, y: 20 },
    });

    await expect(page.getByTestId("excerpt-card")).toHaveCount(beforeCount);
    await expect(page.getByText(`${prefix} inbox excerpt waiting for mount`)).toBeVisible();
  });

  test("E2E-11 已挂载摘录可以移回未分类", async ({ page }) => {
    prefix = await loginAndSeed(page, "move-back");
    await page.getByTestId("inbox-filter-all").click();
    const card = page.getByTestId("excerpt-card").filter({ hasText: `${prefix} mounted excerpt` });
    await card.getByTestId("excerpt-move-to-inbox").click();
    await page.getByTestId("inbox-filter-inbox").click();
    await expect(page.getByTestId("excerpt-card").filter({ hasText: `${prefix} mounted excerpt` })).toHaveCount(1);
  });

  test("E2E-12 选择不同节点会联动右侧抽屉上下文", async ({ page }) => {
    prefix = await loginAndSeed(page, "drawer-context");
    await nodeByTitle(page, `${prefix} Child`).click();
    await expect(page.getByTestId("detail-node-title-input")).toHaveValue(`${prefix} Child`);
    await nodeByTitle(page, `${prefix} Empty`).click();
    await expect(page.getByTestId("detail-node-title-input")).toHaveValue(`${prefix} Empty`);
  });

  test("E2E-13 节点标题编辑后刷新仍保留", async ({ page }) => {
    prefix = await loginAndSeed(page, "rename-node");
    const nextTitle = `${prefix} renamed`;
    await nodeByTitle(page, `${prefix} Child`).dblclick();
    await page.locator(`[data-node-edit-input="${prefix}-child"]`).fill(nextTitle);
    await page.keyboard.press("Enter");
    await expect(nodeByTitle(page, nextTitle)).toBeVisible();
    await page.reload();
    await waitForSeededWorkspace(page);
    await expect(nodeByTitle(page, nextTitle)).toBeVisible();
  });

  test("E2E-14 Tab 和 Enter 可以键盘建树", async ({ page }) => {
    prefix = await loginAndSeed(page, "keyboard-create");
    const childTitle = `${prefix} keyboard child`;
    const siblingTitle = `${prefix} keyboard sibling`;

    await nodeByTitle(page, `${prefix} Child`).click();
    await page.evaluate(() => {
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
      }
    });
    await page.keyboard.press("Tab");
    await expect(page.locator("[data-node-edit-input]")).toBeVisible();
    await page.locator("[data-node-edit-input]").fill(childTitle);
    await page.keyboard.press("Enter");
    await expect(nodeByTitle(page, childTitle)).toBeVisible();

    await nodeByTitle(page, childTitle).click();
    await page.evaluate(() => {
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
      }
    });
    await page.keyboard.press("Enter");
    await expect(page.locator("[data-node-edit-input]")).toBeVisible();
    await page.locator("[data-node-edit-input]").fill(siblingTitle);
    await page.keyboard.press("Enter");
    await expect(nodeByTitle(page, siblingTitle)).toBeVisible();
  });

  test("E2E-15 方向键按树关系切换选中节点", async ({ page }) => {
    prefix = await loginAndSeed(page, "keyboard-nav");
    await nodeByTitle(page, `${prefix} Child`).click();
    await page.keyboard.press("ArrowDown");
    await expect(page.getByTestId("detail-node-title-input")).toHaveValue(`${prefix} Empty`);
    await page.keyboard.press("ArrowUp");
    await expect(page.getByTestId("detail-node-title-input")).toHaveValue(`${prefix} Child`);
  });

  test("E2E-16 Space 可以切换详情抽屉", async ({ page }) => {
    prefix = await loginAndSeed(page, "space-drawer");
    await nodeByTitle(page, `${prefix} Child`).click();
    await page.keyboard.press("Space");
    await expect(page.getByTestId("detail-drawer")).toHaveCount(0);
    await page.keyboard.press("Space");
    await expect(page.getByTestId("detail-drawer")).toBeVisible();
  });

  test("E2E-17 节点展开折叠状态刷新后恢复", async ({ page }) => {
    prefix = await loginAndSeed(page, "expand-restore");
    await page.getByTestId(`tree-node-toggle-${prefix}-root`).click();
    await expect(nodeByTitle(page, `${prefix} Child`)).toHaveCount(0);
    await page.waitForTimeout(800);
    await page.reload();
    await waitForWorkbench(page);
    await expect(nodeByTitle(page, `${prefix} Child`)).toHaveCount(0);
  });

  test("E2E-18 画布缩放和平移不丢失节点", async ({ page }) => {
    prefix = await loginAndSeed(page, "canvas-controls");
    await nodeByTitle(page, `${prefix} Child`).click();
    await page.locator(".react-flow__controls-zoomin").click();
    const canvas = await page.getByTestId("knowledge-tree-canvas").boundingBox();
    expect(canvas).toBeTruthy();
    await page.mouse.move(canvas!.x + 560, canvas!.y + 320);
    await page.mouse.down();
    await page.mouse.move(canvas!.x + 480, canvas!.y + 320, { steps: 8 });
    await page.mouse.up();
    await expect(nodeByTitle(page, `${prefix} Child`)).toBeVisible();
    await expect(page.getByTestId("detail-node-title-input")).toHaveValue(`${prefix} Child`);
  });

  test("E2E-19 大纲编辑器自动保存并刷新恢复", async ({ page }) => {
    prefix = await loginAndSeed(page, "outline-save");
    const outline = `${prefix} saved outline`;
    await nodeByTitle(page, `${prefix} Child`).click();
    const editor = page.locator('[data-testid="node-outline-editor"] .ProseMirror');
    await editor.click();
    await page.keyboard.press(process.platform === "darwin" ? "Meta+A" : "Control+A");
    await page.keyboard.type(outline);
    await expect(editor).toContainText(outline);
    await page.waitForTimeout(1_200);
    await expect(page.getByTestId("outline-save-status")).toHaveText("已就绪", { timeout: 5_000 });
    await page.reload();
    await waitForSeededWorkspace(page);
    await nodeByTitle(page, `${prefix} Child`).click();
    await expect(page.locator('[data-testid="node-outline-editor"] .ProseMirror')).toContainText(outline);
  });

  test("E2E-20 大纲格式按钮可以生成格式化内容", async ({ page }) => {
    prefix = await loginAndSeed(page, "outline-format");
    await nodeByTitle(page, `${prefix} Child`).click();
    const editor = page.locator('[data-testid="node-outline-editor"] .ProseMirror');
    await editor.click();
    await page.getByLabel("加粗").click();
    await page.keyboard.type("bold text");
    await expect(editor.locator("strong")).toContainText("bold text");
    await page.getByLabel("无序列表").click();
    await page.keyboard.type("list item");
    await expect(editor.locator("ul")).toContainText("list item");
  });

  test("E2E-21 Slash 菜单可以插入常见块", async ({ page }) => {
    prefix = await loginAndSeed(page, "slash-menu");
    await nodeByTitle(page, `${prefix} Child`).click();
    const editor = page.locator('[data-testid="node-outline-editor"] .ProseMirror');
    await editor.click();
    await page.keyboard.type("/");
    await expect(page.getByText("Slash 菜单")).toBeVisible();
    await page.getByRole("button", { name: "标题" }).click();
    await expect(page.getByText("Slash 菜单")).toHaveCount(0);
  });

  test("E2E-22 我的思考编辑后刷新保留", async ({ page }) => {
    prefix = await loginAndSeed(page, "thought-save");
    const thought = `${prefix} updated thought`;
    await nodeByTitle(page, `${prefix} Child`).click();
    await page.getByTestId("excerpt-thought-input").fill(thought);
    await page.getByTestId("detail-node-title-input").click();
    await expect.poll(async () => getMountedExcerptThought(page, prefix), { timeout: 5_000 }).toBe(thought);
    await page.reload();
    await waitForSeededWorkspace(page);
    await nodeByTitle(page, `${prefix} Child`).click();
    await expect(page.getByTestId("excerpt-thought-input")).toHaveValue(thought);
  });

  test("E2E-23 可以创建新标签并通过搜索定位", async ({ page }) => {
    prefix = await loginAndSeed(page, "tag-create");
    const tagName = `${prefix}-new-tag`;
    await nodeByTitle(page, `${prefix} Child`).click();
    await page.getByTestId("tag-add-toggle").click();
    await page.getByTestId("tag-query-input").fill(tagName);
    await page.getByRole("button", { name: `创建 ${tagName}` }).click();
    await expect(page.getByText(tagName)).toBeVisible();
    await openSearch(page);
    await page.getByPlaceholder("搜索节点、摘录或 #标签").fill(`#${tagName}`);
    await expect(page.getByTestId("search-dialog").getByText(`#${tagName}`)).toBeVisible();
  });

  test("E2E-24 标签删除后进入垃圾篓", async ({ page }) => {
    prefix = await loginAndSeed(page, "tag-delete");
    await nodeByTitle(page, `${prefix} Child`).click();
    await page.getByLabel("删除标签").first().click();
    await page.getByTestId("account-menu-trigger").click();
    await page.getByRole("banner").getByRole("button", { name: "垃圾篓" }).click();
    await expect(page.getByTestId("trash-dialog").getByText(`${prefix}-tag`)).toBeVisible();
  });

  test("E2E-25 挂载位置选择器可以移动摘录", async ({ page }) => {
    prefix = await loginAndSeed(page, "mount-select");
    await nodeByTitle(page, `${prefix} Child`).click();
    await page.getByTestId("excerpt-mount-select").selectOption(`${prefix}-empty`);
    await expect(page.getByTestId("detail-feed-empty")).toBeVisible();
    await nodeByTitle(page, `${prefix} Empty`).click();
    await expect(page.getByText(`${prefix} mounted excerpt for detail drawer`)).toBeVisible();
  });

  test("E2E-26 全局搜索可以打开和关闭", async ({ page }) => {
    prefix = await loginAndSeed(page, "search-open");
    await openSearch(page);
    await expect(page.getByText("全局搜索")).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(page.getByText("全局搜索")).toHaveCount(0);
  });

  test("E2E-27 搜索能返回节点、摘录和标签结果", async ({ page }) => {
    prefix = await loginAndSeed(page, "search-results");
    await openSearch(page);
    await page.getByPlaceholder("搜索节点、摘录或 #标签").fill(prefix);
    await expect(page.getByTestId("search-dialog").getByText(`${prefix} Child`)).toBeVisible();
    await expect(page.getByTestId("search-dialog").getByText(`${prefix} Mounted Source`)).toBeVisible();
    await expect(page.getByTestId("search-dialog").getByText(`#${prefix}-tag`)).toBeVisible();
    await page.getByTestId("search-dialog").getByText(`${prefix} Child`).click();
    await expect(page.getByTestId("detail-node-title-input")).toHaveValue(`${prefix} Child`);
  });

  test("E2E-28 最近搜索会在刷新后保留", async ({ page }) => {
    prefix = await loginAndSeed(page, "recent-search");
    await openSearch(page);
    await page.getByPlaceholder("搜索节点、摘录或 #标签").fill(prefix);
    await page.getByTestId("search-dialog").getByText(`${prefix} Child`).click();
    await expect(page.getByTestId("search-dialog")).toHaveCount(0);
    await page.waitForTimeout(500);
    await page.reload();
    await waitForSeededWorkspace(page);
    await openSearch(page);
    await expect(page.getByTestId("search-dialog").getByText(prefix)).toBeVisible();
  });

  test("E2E-29 主题切换即时生效并刷新保留", async ({ page }) => {
    prefix = await loginAndSeed(page, "theme-toggle");
    const before = await page.evaluate(() => document.documentElement.dataset.theme);
    await page.getByTestId("theme-toggle").click();
    const after = await page.evaluate(() => document.documentElement.dataset.theme);
    expect(after).not.toBe(before);
    await page.waitForTimeout(500);
    await page.reload();
    await waitForWorkbench(page);
    await expect.poll(() => page.evaluate(() => document.documentElement.dataset.theme)).toBe(after);
  });

  test("E2E-30 面板折叠和拖拽宽度刷新后恢复", async ({ page }) => {
    prefix = await loginAndSeed(page, "panel-width");
    await page.getByLabel("折叠收件箱").click();
    await expect.poll(async () => (await page.getByTestId("inbox-sidebar").boundingBox())?.width ?? 0).toBeLessThanOrEqual(96);
    await page.getByLabel("展开收件箱").click();
    const separator = page.getByRole("separator", { name: "调整收件箱宽度" });
    const box = await separator.boundingBox();
    expect(box).toBeTruthy();
    await page.mouse.move(box!.x + 1, box!.y + 20);
    await page.mouse.down();
    await page.mouse.move(box!.x + 80, box!.y + 20, { steps: 8 });
    await page.mouse.up();
    const width = (await page.getByTestId("inbox-sidebar").boundingBox())?.width ?? 0;
    expect(width).toBeGreaterThan(300);
    await page.waitForTimeout(500);
    await page.reload();
    await waitForWorkbench(page);
    await expect.poll(async () => (await page.getByTestId("inbox-sidebar").boundingBox())?.width ?? 0).toBeGreaterThan(300);
  });

  test("E2E-31 垃圾篓入口展示节点、摘录和标签分区", async ({ page }) => {
    prefix = await loginAndSeed(page, "trash-entry");
    await page.getByTestId("account-menu-trigger").click();
    await page.getByRole("banner").getByRole("button", { name: "垃圾篓" }).click();
    await expect(page.getByTestId("trash-dialog").getByText("已删除内容")).toBeVisible();
    await expect(page.getByTestId("trash-dialog").getByText("节点", { exact: true })).toBeVisible();
    await expect(page.getByTestId("trash-dialog").getByText("摘录", { exact: true })).toBeVisible();
    await expect(page.getByTestId("trash-dialog").getByText("标签", { exact: true })).toBeVisible();
  });

  test("E2E-32 摘录删除后可从垃圾篓恢复", async ({ page }) => {
    prefix = await loginAndSeed(page, "restore-excerpt");
    const card = page.getByTestId("excerpt-card").filter({ hasText: `${prefix} inbox excerpt` });
    await card.getByTestId("excerpt-delete").click();
    await expect(card).toHaveCount(0);
    await page.getByRole("button", { name: "垃圾篓" }).click();
    await expect(page.getByTestId("trash-dialog").getByText(`${prefix} inbox excerpt waiting for mount`)).toBeVisible();
    await page.getByRole("button", { name: "恢复" }).first().click();
    await page.getByTestId("trash-dialog").getByLabel("关闭").click();
    await expect(page.getByTestId("excerpt-card").filter({ hasText: `${prefix} inbox excerpt` })).toHaveCount(1);
  });

  test("E2E-33 节点子树删除后可从垃圾篓恢复", async ({ page }) => {
    prefix = await loginAndSeed(page, "restore-node");
    await nodeByTitle(page, `${prefix} Empty`).click();
    await page.keyboard.press("Delete");
    await expect(nodeByTitle(page, `${prefix} Empty`)).toHaveCount(0);
    await page.getByTestId("account-menu-trigger").click();
    await page.getByRole("banner").getByRole("button", { name: "垃圾篓" }).click();
    await expect(page.getByTestId("trash-dialog").getByText(`${prefix} Empty`)).toBeVisible();
    await page.getByRole("button", { name: "恢复" }).first().click();
    await expect(page.getByTestId("trash-dialog").getByText(`${prefix} Empty`)).toHaveCount(0);
    await page.getByTestId("trash-dialog").getByLabel("关闭").click();
    await page.reload();
    await waitForSeededWorkspace(page);
    await expect(nodeByTitle(page, `${prefix} Empty`)).toBeVisible();
  });

  test("E2E-34 离线时显示离线可用且本地内容仍可见", async ({ page }) => {
    prefix = await loginAndSeed(page, "offline");
    await page.evaluate(() => {
      Object.defineProperty(navigator, "onLine", { value: false, configurable: true });
      window.dispatchEvent(new Event("offline"));
    });
    await waitForWorkbench(page);
    await expect(page.getByText("离线可用")).toBeVisible();
    await expect(page.getByTestId(`tree-node-${prefix}-root`)).toBeAttached();
    await page.evaluate(() => {
      Object.defineProperty(navigator, "onLine", { value: true, configurable: true });
      window.dispatchEvent(new Event("online"));
    });
  });

  test("E2E-35 恢复网络后同步状态离开离线提示", async ({ page }) => {
    prefix = await loginAndSeed(page, "online-restore");
    await page.evaluate(() => {
      Object.defineProperty(navigator, "onLine", { value: false, configurable: true });
      window.dispatchEvent(new Event("offline"));
    });
    await expect(page.getByText("离线可用")).toBeVisible();
    await page.evaluate(() => {
      Object.defineProperty(navigator, "onLine", { value: true, configurable: true });
      window.dispatchEvent(new Event("online"));
    });
    await waitForWorkbench(page);
    await expect(page.getByText("离线可用")).toHaveCount(0);
  });

  test("E2E-36 退出登录后重新保护 /app", async ({ page }) => {
    prefix = await loginAndSeed(page, "sign-out");
    await signOut(page);
    await page.goto("/app");
    await page.waitForURL(/\/login(?:\/)?$/);
    await expect(page.getByTestId("app-shell")).toHaveCount(0);
  });
});
