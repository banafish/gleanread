import { useCallback, useEffect, useMemo, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import {
  Bold,
  Code,
  Eye,
  Heading2,
  Italic,
  Link2,
  List,
  ListChecks,
  ListOrdered,
  Minus,
  PencilLine,
  Quote,
  Strikethrough,
} from "lucide-react";
import { markdownToHtml } from "@/shared/markdown";
import { cx } from "@/shared/utils";

interface NodeOutlineEditorProps {
  nodeId: string;
  value: string;
  onSave: (markdown: string) => Promise<void>;
}

type EditorMode = "preview" | "edit";

interface TextPatch {
  next: string;
  selectionStart: number;
  selectionEnd: number;
}

function EditorButton({
  active,
  label,
  title = label,
  testId,
  onClick,
  children,
}: {
  active?: boolean;
  label: string;
  title?: string;
  testId?: string;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      data-testid={testId}
      className={cx(
        "inline-flex h-8 w-8 items-center justify-center rounded-lg border border-transparent text-app-muted transition",
        "hover:border-app-border hover:bg-app-surface2 hover:text-app-text",
        active && "border-app-accent bg-app-accent/10 text-app-accent"
      )}
      title={title}
      aria-label={label}
      aria-pressed={active}
      onMouseDown={(event) => event.preventDefault()}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

function getLineStart(content: string, index: number): number {
  return content.lastIndexOf("\n", Math.max(0, index - 1)) + 1;
}

function wrapSelection(
  content: string,
  start: number,
  end: number,
  prefix: string,
  suffix: string,
  placeholder: string
): TextPatch {
  const selected = content.slice(start, end);
  const body = selected || placeholder;
  const replacement = `${prefix}${body}${suffix}`;
  const next = `${content.slice(0, start)}${replacement}${content.slice(end)}`;
  const bodyStart = start + prefix.length;
  return {
    next,
    selectionStart: bodyStart,
    selectionEnd: bodyStart + body.length,
  };
}

function prefixLines(content: string, start: number, end: number, getPrefix: (index: number) => string): TextPatch {
  const lineStart = getLineStart(content, start);
  const block = content.slice(lineStart, end);
  const lines = block.length > 0 ? block.split("\n") : [""];
  const replacement = lines.map((line, index) => `${getPrefix(index)}${line}`).join("\n");
  const next = `${content.slice(0, lineStart)}${replacement}${content.slice(end)}`;
  return {
    next,
    selectionStart: lineStart,
    selectionEnd: lineStart + replacement.length,
  };
}

function insertBlock(content: string, start: number, end: number, block: string, selectOffset = 0, selectLength = 0): TextPatch {
  const before = content.slice(0, start);
  const after = content.slice(end);
  const prefix = before.length === 0 || before.endsWith("\n\n") ? "" : before.endsWith("\n") ? "\n" : "\n\n";
  const suffix = after.length === 0 || after.startsWith("\n\n") ? "" : after.startsWith("\n") ? "\n" : "\n\n";
  const replacement = `${prefix}${block}${suffix}`;
  const next = `${before}${replacement}${after}`;
  const blockStart = start + prefix.length + selectOffset;
  return {
    next,
    selectionStart: blockStart,
    selectionEnd: blockStart + selectLength,
  };
}

export function NodeOutlineEditor({ nodeId, value, onSave }: NodeOutlineEditorProps) {
  const [draft, setDraft] = useState(value);
  const [saving, setSaving] = useState(false);
  const [slashOpen, setSlashOpen] = useState(false);
  const [mode, setMode] = useState<EditorMode>("preview");
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const loadedNodeId = useRef(nodeId);
  const lastSavedValue = useRef(value);
  const previewHtml = useMemo(() => markdownToHtml(draft), [draft]);

  useEffect(() => {
    if (loadedNodeId.current !== nodeId) {
      loadedNodeId.current = nodeId;
      lastSavedValue.current = value;
      setDraft(value);
      setMode("preview");
      setSlashOpen(false);
      return;
    }

    if (value !== lastSavedValue.current) {
      const previousValue = lastSavedValue.current;
      lastSavedValue.current = value;
      setDraft((currentDraft) => currentDraft === previousValue ? value : currentDraft);
    }
  }, [nodeId, value]);

  useEffect(() => {
    if (mode !== "edit") {
      return;
    }
    const frame = window.requestAnimationFrame(() => {
      textareaRef.current?.focus();
    });
    return () => window.cancelAnimationFrame(frame);
  }, [mode, nodeId]);

  useEffect(() => {
    if (draft === value) {
      return;
    }
    const handle = window.setTimeout(() => {
      void (async () => {
        setSaving(true);
        try {
          await onSave(draft);
        } finally {
          setSaving(false);
        }
      })();
    }, 700);
    return () => window.clearTimeout(handle);
  }, [draft, onSave, value]);

  const applyMarkdownPatch = useCallback(
    (patcher: (content: string, start: number, end: number) => TextPatch, options?: { removeSlashTrigger?: boolean }) => {
      const textarea = textareaRef.current;
      if (!textarea) {
        return;
      }
      let content = draft;
      let start = textarea.selectionStart;
      let end = textarea.selectionEnd;

      if (options?.removeSlashTrigger && start > 0 && content[start - 1] === "/") {
        content = `${content.slice(0, start - 1)}${content.slice(start)}`;
        start -= 1;
        end = Math.max(start, end - 1);
      }

      const patch = patcher(content, start, end);
      setDraft(patch.next);
      window.requestAnimationFrame(() => {
        textarea.focus();
        textarea.setSelectionRange(patch.selectionStart, patch.selectionEnd);
      });
    },
    [draft]
  );

  const insertWrapped = useCallback(
    (prefix: string, suffix: string, placeholder: string) => {
      applyMarkdownPatch((content, start, end) => wrapSelection(content, start, end, prefix, suffix, placeholder));
    },
    [applyMarkdownPatch]
  );

  const insertLinePrefix = useCallback(
    (getPrefix: (index: number) => string, options?: { removeSlashTrigger?: boolean }) => {
      applyMarkdownPatch((content, start, end) => prefixLines(content, start, end, getPrefix), options);
    },
    [applyMarkdownPatch]
  );

  const insertCodeBlock = useCallback(
    (options?: { removeSlashTrigger?: boolean }) => {
      applyMarkdownPatch((content, start, end) => {
        const selected = content.slice(start, end);
        const body = selected || "代码";
        return insertBlock(content, start, end, `\`\`\`\n${body}\n\`\`\``, 4, body.length);
      }, options);
    },
    [applyMarkdownPatch]
  );

  const insertDivider = useCallback(() => {
    applyMarkdownPatch((content, start, end) => insertBlock(content, start, end, "---"));
  }, [applyMarkdownPatch]);

  const insertLink = useCallback(() => {
    applyMarkdownPatch((content, start, end) => {
      const selected = content.slice(start, end);
      const label = selected || "链接文字";
      const replacement = `[${label}](https://example.com)`;
      const next = `${content.slice(0, start)}${replacement}${content.slice(end)}`;
      const hrefStart = start + label.length + 3;
      return {
        next,
        selectionStart: hrefStart,
        selectionEnd: hrefStart + "https://example.com".length,
      };
    });
  }, [applyMarkdownPatch]);

  const handleSourceKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "/") {
      setSlashOpen(true);
    }
    if (event.key === "Escape") {
      setSlashOpen(false);
      return;
    }

    const modifier = event.metaKey || event.ctrlKey;
    if (!modifier) {
      return;
    }

    const key = event.key.toLowerCase();
    if (key === "b") {
      event.preventDefault();
      insertWrapped("**", "**", "重点");
      return;
    }
    if (key === "i") {
      event.preventDefault();
      insertWrapped("*", "*", "重点");
      return;
    }
    if (key === "k") {
      event.preventDefault();
      insertLink();
      return;
    }
    if (event.shiftKey && key === "x") {
      event.preventDefault();
      insertWrapped("~~", "~~", "删除内容");
      return;
    }
    if (event.code === "Backquote") {
      event.preventDefault();
      insertWrapped("`", "`", "代码");
      return;
    }
    if (event.altKey && event.code === "Digit2") {
      event.preventDefault();
      insertLinePrefix(() => "## ");
      return;
    }
    if (event.shiftKey && event.code === "Digit8") {
      event.preventDefault();
      insertLinePrefix(() => "- ");
      return;
    }
    if (event.shiftKey && event.code === "Digit7") {
      event.preventDefault();
      insertLinePrefix((index) => `${index + 1}. `);
    }
  };

  const runSlashAction = (action: () => void) => {
    action();
    setSlashOpen(false);
  };

  return (
    <div
      className="relative overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-panel"
      data-skip-hotkeys="true"
      data-testid="node-outline-editor"
    >
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-app-border px-3 py-2">
        <div className="flex min-w-0 flex-wrap items-center gap-1">
          {mode === "edit" ? (
            <>
              <EditorButton label="标题" title="标题 (Ctrl+Alt+2)" onClick={() => insertLinePrefix(() => "## ")}>
                <Heading2 size={15} />
              </EditorButton>
              <EditorButton label="加粗" title="加粗 (Ctrl+B)" onClick={() => insertWrapped("**", "**", "重点")}>
                <Bold size={15} />
              </EditorButton>
              <EditorButton label="斜体" title="斜体 (Ctrl+I)" onClick={() => insertWrapped("*", "*", "重点")}>
                <Italic size={15} />
              </EditorButton>
              <EditorButton label="删除线" title="删除线 (Ctrl+Shift+X)" onClick={() => insertWrapped("~~", "~~", "删除内容")}>
                <Strikethrough size={15} />
              </EditorButton>
              <EditorButton label="行内代码" title="行内代码 (Ctrl+`)" onClick={() => insertWrapped("`", "`", "代码")}>
                <Code size={15} />
              </EditorButton>
              <EditorButton label="链接" title="链接 (Ctrl+K)" onClick={insertLink}>
                <Link2 size={15} />
              </EditorButton>
              <EditorButton label="无序列表" title="无序列表 (Ctrl+Shift+8)" onClick={() => insertLinePrefix(() => "- ")}>
                <List size={15} />
              </EditorButton>
              <EditorButton label="有序列表" title="有序列表 (Ctrl+Shift+7)" onClick={() => insertLinePrefix((index) => `${index + 1}. `)}>
                <ListOrdered size={15} />
              </EditorButton>
              <EditorButton label="任务项" title="任务项" onClick={() => insertLinePrefix(() => "- [ ] ")}>
                <ListChecks size={15} />
              </EditorButton>
              <EditorButton label="引用" title="引用" onClick={() => insertLinePrefix(() => "> ")}>
                <Quote size={15} />
              </EditorButton>
              <EditorButton label="代码块" title="代码块" onClick={() => insertCodeBlock()}>
                <Code size={15} />
              </EditorButton>
              <EditorButton label="分割线" title="分割线" onClick={insertDivider}>
                <Minus size={15} />
              </EditorButton>
            </>
          ) : (
            <span className="px-1 text-xs font-medium text-app-muted">Markdown 预览</span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-app-muted" data-testid="outline-save-status">
            {saving ? "保存中" : "已就绪"}
          </span>
          <EditorButton
            label={mode === "preview" ? "编辑" : "预览"}
            title={mode === "preview" ? "编辑 Markdown 源码" : "预览 Markdown"}
            testId={mode === "preview" ? "outline-edit-toggle" : "outline-preview-toggle"}
            active={mode === "preview"}
            onClick={() => {
              setMode((currentMode) => (currentMode === "preview" ? "edit" : "preview"));
              setSlashOpen(false);
            }}
          >
            {mode === "preview" ? <PencilLine size={15} /> : <Eye size={15} />}
          </EditorButton>
        </div>
      </div>
      {slashOpen ? (
        <div
          className="absolute right-3 top-12 z-10 w-44 rounded-xl border border-app-border bg-app-surface p-2 shadow-2xl"
          data-testid="outline-slash-menu"
        >
          <div className="mb-2 text-[11px] font-medium uppercase tracking-wide text-app-muted">Slash 菜单</div>
          <div className="space-y-1">
            <button
              type="button"
              className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-app-surface2"
              onClick={() => {
                runSlashAction(() => insertLinePrefix(() => "## ", { removeSlashTrigger: true }));
              }}
            >
              标题
            </button>
            <button
              type="button"
              className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-app-surface2"
              onClick={() => {
                runSlashAction(() => insertLinePrefix(() => "- ", { removeSlashTrigger: true }));
              }}
            >
              列表
            </button>
            <button
              type="button"
              className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-app-surface2"
              onClick={() => {
                runSlashAction(() => insertCodeBlock({ removeSlashTrigger: true }));
              }}
            >
              代码块
            </button>
          </div>
        </div>
      ) : null}
      {mode === "preview" ? (
        <div
          className={cx(
            "min-h-[220px] rounded-b-panel bg-app-surface px-4 py-3 text-sm leading-6 text-app-text",
            "[&_a]:text-app-accent [&_a]:underline [&_blockquote]:border-l-2 [&_blockquote]:border-app-border [&_blockquote]:pl-3 [&_blockquote]:text-app-muted",
            "[&_code]:rounded-md [&_code]:bg-app-surface2 [&_code]:px-1 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-[0.9em]",
            "[&_del]:text-app-muted [&_h1]:mb-2 [&_h1]:text-lg [&_h1]:font-semibold [&_h2]:mb-2 [&_h2]:text-base [&_h2]:font-semibold",
            "[&_h3]:mb-2 [&_h3]:text-sm [&_h3]:font-semibold [&_hr]:my-3 [&_hr]:border-app-border",
            "[&_li]:my-1 [&_ol]:ml-5 [&_ol]:list-decimal [&_p]:my-2 [&_pre]:overflow-auto [&_pre]:rounded-xl [&_pre]:bg-app-surface2 [&_pre]:p-3",
            "[&_pre_code]:bg-transparent [&_pre_code]:p-0 [&_ul]:ml-5 [&_ul]:list-disc"
          )}
          data-testid="node-outline-preview"
          onDoubleClick={() => setMode("edit")}
        >
          {draft.trim() ? (
            <div dangerouslySetInnerHTML={{ __html: previewHtml }} />
          ) : (
            <div className="text-app-muted">沉淀这个节点的结构化理解...</div>
          )}
        </div>
      ) : (
        <textarea
          ref={textareaRef}
          className="min-h-[220px] w-full resize-y rounded-b-panel border-0 bg-app-surface px-4 py-3 font-mono text-[13px] leading-6 text-app-text outline-none placeholder:text-app-muted focus:ring-0"
          data-testid="node-outline-source"
          placeholder="沉淀这个节点的结构化理解..."
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={handleSourceKeyDown}
        />
      )}
    </div>
  );
}
