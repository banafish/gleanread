import test from "node:test";
import assert from "node:assert/strict";
import { resolveAvatarUrlFromMetadata } from "../supabase/authProfile.ts";

test("resolveAvatarUrlFromMetadata reads Supabase avatar_url first", () => {
  assert.equal(
    resolveAvatarUrlFromMetadata({
      avatar_url: " https://example.com/avatar.jpg?t=1 ",
      picture: "https://example.com/oauth.jpg",
    }),
    "https://example.com/avatar.jpg?t=1"
  );
});

test("resolveAvatarUrlFromMetadata falls back to OAuth picture", () => {
  assert.equal(
    resolveAvatarUrlFromMetadata({
      avatar_url: " ",
      picture: "https://example.com/oauth.jpg",
    }),
    "https://example.com/oauth.jpg"
  );
});

test("resolveAvatarUrlFromMetadata ignores invalid metadata values", () => {
  assert.equal(resolveAvatarUrlFromMetadata({ avatar_url: 42 }), null);
});
