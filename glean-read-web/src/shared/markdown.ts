function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function applyInlineMarkdown(text: string): string {
  const escaped = escapeHtml(text);
  return escaped
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/(^|[^*])\*(?!\s)(.+?)(?!\s)\*(?!\*)/g, "$1<em>$2</em>")
    .replace(/`(.+?)`/g, "<code>$1</code>")
    .replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" rel="noreferrer noopener" target="_blank">$1</a>');
}

export function markdownToHtml(markdown: string): string {
  const lines = markdown.replaceAll("\r\n", "\n").split("\n");
  const chunks: string[] = [];
  let paragraph: string[] = [];
  let listType: "ul" | "ol" | null = null;
  let listItems: string[] = [];
  let codeLines: string[] = [];
  let inCodeBlock = false;

  const flushParagraph = () => {
    if (paragraph.length > 0) {
      chunks.push(`<p>${applyInlineMarkdown(paragraph.join(" ").trim())}</p>`);
      paragraph = [];
    }
  };

  const flushList = () => {
    if (listType && listItems.length > 0) {
      const items = listItems.map((item) => `<li>${applyInlineMarkdown(item.trim())}</li>`).join("");
      chunks.push(`<${listType}>${items}</${listType}>`);
    }
    listType = null;
    listItems = [];
  };

  const flushCode = () => {
    if (codeLines.length > 0) {
      chunks.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
      codeLines = [];
    }
  };

  for (const rawLine of lines) {
    const line = rawLine.trimEnd();
    const trimmed = line.trim();

    if (trimmed.startsWith("```")) {
      if (inCodeBlock) {
        flushCode();
        inCodeBlock = false;
      } else {
        flushParagraph();
        flushList();
        inCodeBlock = true;
      }
      continue;
    }

    if (inCodeBlock) {
      codeLines.push(line);
      continue;
    }

    if (!trimmed) {
      flushParagraph();
      flushList();
      continue;
    }

    const headingMatch = trimmed.match(/^(#{1,6})\s+(.+)$/);
    if (headingMatch) {
      flushParagraph();
      flushList();
      const level = headingMatch[1].length;
      chunks.push(`<h${level}>${applyInlineMarkdown(headingMatch[2].trim())}</h${level}>`);
      continue;
    }

    const quoteMatch = trimmed.match(/^>\s+(.+)$/);
    if (quoteMatch) {
      flushParagraph();
      flushList();
      chunks.push(`<blockquote><p>${applyInlineMarkdown(quoteMatch[1].trim())}</p></blockquote>`);
      continue;
    }

    const orderedMatch = trimmed.match(/^\d+\.\s+(.+)$/);
    const bulletMatch = trimmed.match(/^[-*+]\s+(.+)$/);
    if (orderedMatch || bulletMatch) {
      flushParagraph();
      const nextListType = orderedMatch ? "ol" : "ul";
      if (listType && listType !== nextListType) {
        flushList();
      }
      listType = nextListType;
      listItems.push((orderedMatch ?? bulletMatch)?.[1] ?? "");
      continue;
    }

    if (listType) {
      flushList();
    }
    paragraph.push(trimmed);
  }

  flushParagraph();
  flushList();
  flushCode();

  return chunks.join("");
}

function serializeChildren(node: ChildNode, depth: number): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return node.textContent ?? "";
  }
  if (node.nodeType !== Node.ELEMENT_NODE) {
    return "";
  }
  const element = node as HTMLElement;
  const tag = element.tagName.toLowerCase();
  const childText = Array.from(element.childNodes)
    .map((child) => serializeChildren(child, depth))
    .join("");

  if (tag === "strong" || tag === "b") {
    return `**${childText}**`;
  }
  if (tag === "em" || tag === "i") {
    return `*${childText}*`;
  }
  if (tag === "code" && element.parentElement?.tagName.toLowerCase() !== "pre") {
    return `\`${childText}\``;
  }
  if (tag === "a") {
    const href = element.getAttribute("href") ?? "";
    return `[${childText}](${href})`;
  }
  if (tag === "br") {
    return "  \n";
  }
  if (tag === "h1" || tag === "h2" || tag === "h3" || tag === "h4" || tag === "h5" || tag === "h6") {
    const level = Number(tag.slice(1));
    return `${"#".repeat(level)} ${childText.trim()}\n\n`;
  }
  if (tag === "p") {
    return `${childText.trim()}\n\n`;
  }
  if (tag === "blockquote") {
    const quoted = childText
      .trim()
      .split("\n")
      .map((line) => `> ${line}`)
      .join("\n");
    return `${quoted}\n\n`;
  }
  if (tag === "pre") {
    return `\`\`\`\n${element.textContent?.replace(/\n$/, "") ?? ""}\n\`\`\`\n\n`;
  }
  if (tag === "ul" || tag === "ol") {
    const ordered = tag === "ol";
    return Array.from(element.children)
      .filter((child) => child.tagName.toLowerCase() === "li")
      .map((li, index) => {
        const prefix = ordered ? `${index + 1}. ` : "- ";
        const content = Array.from(li.childNodes)
          .map((child) => serializeChildren(child, depth + 1))
          .join("")
          .trim();
        const indent = "  ".repeat(depth);
        return `${indent}${prefix}${content}`;
      })
      .join("\n") + "\n\n";
  }
  return childText;
}

export function htmlToMarkdown(html: string): string {
  if (typeof DOMParser === "undefined") {
    return html;
  }
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  const markdown = Array.from(doc.body.childNodes)
    .map((node) => serializeChildren(node, 0))
    .join("");
  return markdown
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}
