import { useEffect, useRef, useState, type ReactNode } from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Link from "@tiptap/extension-link";
import Placeholder from "@tiptap/extension-placeholder";
import { Bold, Code, Italic, List, ListOrdered, Quote } from "lucide-react";
import { htmlToMarkdown, markdownToHtml } from "@/shared/markdown";
import { cx } from "@/shared/utils";

interface NodeOutlineEditorProps {
  nodeId: string;
  value: string;
  onSave: (markdown: string) => Promise<void>;
}

function EditorButton({
  active,
  title,
  onClick,
  children,
}: {
  active?: boolean;
  title: string;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      className={cx(
        "inline-flex h-8 w-8 items-center justify-center rounded-lg border border-transparent text-app-muted transition",
        "hover:border-app-border hover:bg-app-surface2 hover:text-app-text",
        active && "border-app-accent bg-app-accent/10 text-app-accent"
      )}
      title={title}
      aria-label={title}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

export function NodeOutlineEditor({ nodeId, value, onSave }: NodeOutlineEditorProps) {
  const [draft, setDraft] = useState(value);
  const [saving, setSaving] = useState(false);
  const [slashOpen, setSlashOpen] = useState(false);
  const loadedNodeId = useRef(nodeId);

  const editor = useEditor({
    extensions: [
      StarterKit,
      Link.configure({
        openOnClick: false,
      }),
      Placeholder.configure({
        placeholder: "沉淀这个节点的结构化理解...",
      }),
    ],
    content: markdownToHtml(value) || "<p></p>",
    editorProps: {
      attributes: {
        class:
          "prose prose-sm max-w-none min-h-[220px] rounded-b-panel bg-app-surface px-4 py-3 text-app-text outline-none dark:prose-invert prose-headings:text-app-text prose-p:text-app-text prose-strong:text-app-text prose-code:text-app-text",
      },
      handleKeyDown: (_, event) => {
        if (event.key === "/") {
          setSlashOpen(true);
        }
        if (event.key === "Escape") {
          setSlashOpen(false);
        }
        return false;
      },
    },
    onUpdate: ({ editor: nextEditor }) => {
      setDraft(htmlToMarkdown(nextEditor.getHTML()));
    },
  });

  useEffect(() => {
    if (!editor) {
      return;
    }
    if (loadedNodeId.current !== nodeId) {
      loadedNodeId.current = nodeId;
      setDraft(value);
      editor.commands.setContent(markdownToHtml(value) || "<p></p>", false);
    }
  }, [editor, nodeId, value]);

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

  return (
    <div className="relative overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-panel" data-skip-hotkeys="true">
      <div className="flex items-center justify-between border-b border-app-border px-3 py-2">
        <div className="flex items-center gap-1">
          <EditorButton
            title="加粗"
            active={editor?.isActive("bold")}
            onClick={() => editor?.chain().focus().toggleBold().run()}
          >
            <Bold size={15} />
          </EditorButton>
          <EditorButton
            title="斜体"
            active={editor?.isActive("italic")}
            onClick={() => editor?.chain().focus().toggleItalic().run()}
          >
            <Italic size={15} />
          </EditorButton>
          <EditorButton
            title="无序列表"
            active={editor?.isActive("bulletList")}
            onClick={() => editor?.chain().focus().toggleBulletList().run()}
          >
            <List size={15} />
          </EditorButton>
          <EditorButton
            title="有序列表"
            active={editor?.isActive("orderedList")}
            onClick={() => editor?.chain().focus().toggleOrderedList().run()}
          >
            <ListOrdered size={15} />
          </EditorButton>
          <EditorButton
            title="引用"
            active={editor?.isActive("blockquote")}
            onClick={() => editor?.chain().focus().toggleBlockquote().run()}
          >
            <Quote size={15} />
          </EditorButton>
          <EditorButton
            title="代码块"
            active={editor?.isActive("codeBlock")}
            onClick={() => editor?.chain().focus().toggleCodeBlock().run()}
          >
            <Code size={15} />
          </EditorButton>
        </div>
        <span className="text-xs text-app-muted">{saving ? "保存中" : "已就绪"}</span>
      </div>
      {slashOpen ? (
        <div className="absolute right-3 top-12 z-10 w-44 rounded-xl border border-app-border bg-app-surface p-2 shadow-2xl">
          <div className="mb-2 text-[11px] font-medium uppercase tracking-wide text-app-muted">Slash 菜单</div>
          <div className="space-y-1">
            <button
              type="button"
              className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-app-surface2"
              onClick={() => {
                editor?.chain().focus().toggleHeading({ level: 2 }).run();
                setSlashOpen(false);
              }}
            >
              标题
            </button>
            <button
              type="button"
              className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-app-surface2"
              onClick={() => {
                editor?.chain().focus().toggleBulletList().run();
                setSlashOpen(false);
              }}
            >
              列表
            </button>
            <button
              type="button"
              className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-app-surface2"
              onClick={() => {
                editor?.chain().focus().toggleCodeBlock().run();
                setSlashOpen(false);
              }}
            >
              代码块
            </button>
          </div>
        </div>
      ) : null}
      <EditorContent editor={editor} />
    </div>
  );
}
