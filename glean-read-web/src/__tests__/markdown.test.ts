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

test("Markdown 预览支持扩展快捷语法", () => {
  const html = markdownToHtml("~~删除~~\n\n- [x] 已完成\n- [ ] 待处理\n\n---");

  assert.match(html, /<del>删除<\/del>/);
  assert.match(html, /<input type="checkbox" disabled checked> 已完成/);
  assert.match(html, /<input type="checkbox" disabled> 待处理/);
  assert.match(html, /<hr>/);
});
