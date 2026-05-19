import { expect, test } from "@playwright/test";
import { cleanupWorkspace, loginWithPassword, seededUser, seedWorkspace, testPrefix, waitForWorkbench } from "./fixtures";

test.use({ viewport: { width: 390, height: 844 } });

test("窄屏 smoke：登录后的核心工作台不是空白页", async ({ page }) => {
  const prefix = testPrefix("mobile-smoke");
  await loginWithPassword(page, seededUser);
  await seedWorkspace(page, prefix);
  await waitForWorkbench(page);
  await expect(page.getByTestId("global-search-trigger")).toBeVisible();
  await expect(page.getByTestId(`tree-node-${prefix}-root`)).toBeAttached();
  await cleanupWorkspace(page, prefix);
});
