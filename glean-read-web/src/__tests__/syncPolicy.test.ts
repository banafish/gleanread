import test from "node:test";
import assert from "node:assert/strict";
import { advancePullCursor, isSelfEcho, shouldApplyRemoteChange } from "../supabase/syncPolicy.ts";

test("同步策略会过滤当前设备产生的 Realtime 回音", () => {
  assert.equal(isSelfEcho("device-a", "device-a"), true);
  assert.equal(isSelfEcho("device-b", "device-a"), false);
  assert.equal(isSelfEcho(null, "device-a"), false);
});

test("远端增量只覆盖本地旧数据或已同步数据", () => {
  assert.equal(shouldApplyRemoteChange(undefined, { updateTime: 20, deviceId: "device-b" }), true);
  assert.equal(shouldApplyRemoteChange({ updateTime: 10, syncStatus: "pending" }, { updateTime: 20, deviceId: "device-b" }), true);
  assert.equal(shouldApplyRemoteChange({ updateTime: 30, syncStatus: "pending" }, { updateTime: 20, deviceId: "device-b" }), false);
  assert.equal(shouldApplyRemoteChange({ updateTime: 30, syncStatus: "synced" }, { updateTime: 20, deviceId: "device-b" }), true);
});

test("拉取游标始终前进到最大 update_time", () => {
  assert.equal(advancePullCursor(100, 90), 100);
  assert.equal(advancePullCursor(100, 120), 120);
});
