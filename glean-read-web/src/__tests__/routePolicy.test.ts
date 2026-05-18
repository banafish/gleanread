import test from "node:test";
import assert from "node:assert/strict";
import { resolveLoginRoute, resolveProtectedRoute } from "../app/routes/routePolicy.ts";

test("受保护工作台路由会区分加载、未登录和已登录状态", () => {
  assert.deepEqual(resolveProtectedRoute(false, true), { kind: "loading" });
  assert.deepEqual(resolveProtectedRoute(false, false), { kind: "redirect", to: "/login", replace: true });
  assert.deepEqual(resolveProtectedRoute(true, false), { kind: "render" });
});

test("登录页在已有会话时回跳工作台", () => {
  assert.deepEqual(resolveLoginRoute(false, false), { kind: "render" });
  assert.deepEqual(resolveLoginRoute(true, false), { kind: "redirect", to: "/app", replace: true });
});
