import test from "node:test";
import assert from "node:assert/strict";
import { markdownToHtml } from "../shared/markdown.ts";

test("Markdown 编辑内容会转换为标题、列表和行内样式", () => {
  const html = markdownToHtml("## 大纲\n\n- **重点**\n- `代码`\n\n> 引用");

  assert.match(html, /<h2>大纲<\/h2>/);
  assert.match(html, /<ul><li><strong>重点<\/strong><\/li><li><code>代码<\/code><\/li><\/ul>/);
  assert.match(html, /<blockquote><p>引用<\/p><\/blockquote>/);
});

test("Markdown 转换会转义原始 HTML", () => {
  const html = markdownToHtml("<script>alert('x')</script>");

  assert.equal(html.includes("<script>"), false);
  assert.match(html, /&lt;script&gt;alert/);
});
