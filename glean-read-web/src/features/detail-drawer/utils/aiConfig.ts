export interface AiConfig {
  baseUrl: string;
  token: string;
  model: string;
}

const STORAGE_KEY = "glean_read_ai_config";

const DEFAULT_CONFIG: AiConfig = {
  baseUrl: "",
  token: "",
  model: "",
};

export function getAiConfig(): AiConfig {
  try {
    const data = localStorage.getItem(STORAGE_KEY);
    if (!data) return DEFAULT_CONFIG;
    return { ...DEFAULT_CONFIG, ...JSON.parse(data) };
  } catch (error) {
    console.error("Failed to read AI config from localStorage", error);
    return DEFAULT_CONFIG;
  }
}

export function saveAiConfig(config: AiConfig): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
  } catch (error) {
    console.error("Failed to save AI config to localStorage", error);
  }
}

export function normalizeAiBaseUrl(value: string): string {
  let normalized = value.trim();
  if (!normalized) return "";
  if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
    normalized = `https://${normalized}`;
  }
  normalized = normalized.replace(/\/+$/, "");
  if (normalized.toLowerCase().endsWith("/v1")) {
    normalized = normalized.substring(0, normalized.length - 3).replace(/\/+$/, "");
  }
  return normalized;
}

export function buildOpenAiChatCompletionsUrl(baseUrl: string): string {
  const normalized = normalizeAiBaseUrl(baseUrl);
  return normalized ? `${normalized}/v1/chat/completions` : "";
}

export interface ChatErrorResponse {
  error?: {
    message?: string;
  };
}

export interface ChatResponse {
  choices: Array<{
    message: {
      content: string;
    };
  }>;
}

async function requestOpenAi(
  config: AiConfig,
  messages: Array<{ role: string; content: string }>,
  maxTokens: number
): Promise<string> {
  const { baseUrl, token, model } = config;
  if (!baseUrl.trim() || !token.trim() || !model.trim()) {
    throw new Error("请先配置 AI 接口");
  }

  const url = buildOpenAiChatCompletionsUrl(baseUrl);
  if (!url) {
    throw new Error("无效的 API 基础路径");
  }

  let response: Response;
  try {
    response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token.trim()}`,
      },
      body: JSON.stringify({
        model: model.trim(),
        messages,
        temperature: 0.2,
        max_tokens: maxTokens,
      }),
    });
  } catch (error: any) {
    throw new Error(`AI 接口连接失败：${error?.message || "网络请求失败"}`);
  }

  const responseText = await response.text();
  if (!response.ok) {
    let errorMessage = `HTTP ${response.status}`;
    try {
      const errorJson = JSON.parse(responseText) as ChatErrorResponse;
      if (errorJson?.error?.message) {
        errorMessage = errorJson.error.message;
      }
    } catch {
      // 忽略 JSON 解析错误，使用默认状态文本
    }
    throw new Error(`AI 接口请求失败：${errorMessage}`);
  }

  try {
    const data = JSON.parse(responseText) as ChatResponse;
    const content = data.choices?.[0]?.message?.content?.trim();
    if (!content) {
      throw new Error("AI 接口未返回可用的大纲内容");
    }
    return content;
  } catch (error: any) {
    if (error?.message) throw error;
    throw new Error("AI 接口返回数据解析失败");
  }
}

export async function testAiConnection(config: AiConfig): Promise<void> {
  await requestOpenAi(
    config,
    [
      {
        role: "user",
        content: "请只回复 OK，用于测试连接。",
      },
    ],
    16
  );
}

function extractTitleFromMarkdown(markdown: string): string {
  const lines = markdown.split("\n");
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed) {
      // 剥离前面的 # 符号并去除首尾空格
      const title = trimmed.replace(/^#+\s*/, "").trim();
      if (title) {
        return title.substring(0, 30);
      }
    }
  }
  return "AI 总结";
}

export async function generateOutline(
  excerpts: Array<{
    content: string;
    userThought?: string | null;
    sourceTitle?: string | null;
    url?: string | null;
  }>
): Promise<{ title: string; markdown: string }> {
  if (excerpts.length === 0) {
    return { title: "AI 总结", markdown: "" };
  }

  const config = getAiConfig();
  const excerptText = excerpts
    .slice(0, 20)
    .map((e, index) => {
      let item = `【摘录 ${index + 1}】\n原文：${e.content.trim()}`;
      if (e.userThought?.trim()) {
        item += `\n思考：${e.userThought.trim()}`;
      }
      if (e.sourceTitle?.trim() || e.url?.trim()) {
        const source = [e.sourceTitle?.trim(), e.url?.trim()].filter(Boolean).join(" - ");
        item += `\n来源：${source}`;
      }
      return item;
    })
    .join("\n\n");

  const messages = [
    {
      role: "system",
      content:
        "你是一个极其专业且敏锐的知识提取与大纲整理专家，担任用户的“第二大脑”知识管理助手。请用中文输出 Markdown 大纲，内容必须结构清晰、见解深刻、排版优雅，可直接编辑并挂载到知识树节点。",
    },
    {
      role: "user",
      content: `请基于以下阅读摘录（部分摘录附带了用户的【思考】和【来源】）生成一份结构清晰的知识大纲：\n\n[摘录列表]\n${excerptText}\n[/摘录列表]\n\n【生成规则与要求】：\n1. **结构化层次**：使用标准的 Markdown 标题（#、##、###）和项目符号（-）来展现清晰的逻辑大纲。\n2. **大纲结构框架**：\n   - **# 主题标题**：提炼一个能概括这批摘录核心本质的简明标题。\n   - **## 核心概念与背景**：简要阐述这批摘录所涉及的核心概念、定义或基本背景。\n   - **## 关键观点与深度提炼**：将零散观点进行分类合并，归纳为 2-3 个清晰的主题论点。在每个论点下，提炼核心见解，并结合摘录原文中的具体细节或论据进行结构化展开。**必须充分结合并呼应用户的【思考】**，把用户的感悟、痛点和思考火花融入到对应的论点分析中。\n   - **## 知识关联与启发**（可选）：指出摘录之间的递进、因果、对比或潜在冲突等逻辑联系，并对【思考】中提出的疑问进行提炼和解答。\n   - **## 下一步行动建议**：为用户提供 1-2 条切实可行的后续整理、学习或实践的“行动指南”。\n3. **知识溯源（可选）**：在大纲的具体观点或细节展开处，如适用，可使用简洁的括号或引用，在末尾简要标注其【来源】（如：来源：xxx）。\n4. **严谨性**：仅基于上述摘录内容与思考进行提炼和推导，严禁编造任何未提及的外部事实。\n5. **纯净输出**：直接以 Markdown 一级标题（# ）开始输出大纲内容，绝对不要包含任何前言（如“好的，这是为您整理的...”）、问候语、结语或解释性文段。`,
    },
  ];

  const markdown = await requestOpenAi(config, messages, 1200);
  const title = extractTitleFromMarkdown(markdown);

  return { title, markdown };
}
