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

export async function generateOutline(excerpts: string[]): Promise<{ title: string; markdown: string }> {
  if (excerpts.length === 0) {
    return { title: "AI 总结", markdown: "" };
  }

  const config = getAiConfig();
  const excerptText = excerpts
    .slice(0, 20)
    .map((text, index) => `${index + 1}. ${text.trim()}`)
    .join("\n");

  const messages = [
    {
      role: "system",
      content: "你是一个帮助用户整理阅读摘录的知识管理助手。请用中文输出 Markdown 大纲，内容要可直接编辑并挂载到知识树节点。",
    },
    {
      role: "user",
      content: `请基于以下摘录生成一份结构清晰的知识大纲：\n\n${excerptText}\n\n要求：\n- 使用 Markdown 标题和项目符号。\n- 先给出一个简短主题标题。\n- 提炼核心观点、关系和可行动的后续整理建议。\n- 不要编造摘录中没有的事实。`,
    },
  ];

  const markdown = await requestOpenAi(config, messages, 1200);
  const title = extractTitleFromMarkdown(markdown);

  return { title, markdown };
}
