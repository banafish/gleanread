import { useEffect, useState } from "react";
import { AlertCircle, CheckCircle2, Eye, EyeOff, Loader2 } from "lucide-react";
import { Button, Dialog, Input } from "@/shared/components";
import { getAiConfig, saveAiConfig, testAiConnection } from "../utils/aiConfig";

interface AiConfigDialogProps {
  open: boolean;
  onClose: () => void;
}

export function AiConfigDialog({ open, onClose }: AiConfigDialogProps) {
  const [baseUrl, setBaseUrl] = useState("");
  const [token, setToken] = useState("");
  const [model, setModel] = useState("");
  const [showToken, setShowToken] = useState(false);

  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // 打开弹窗时重新载入最新的配置
  useEffect(() => {
    if (open) {
      const config = getAiConfig();
      setBaseUrl(config.baseUrl);
      setToken(config.token);
      setModel(config.model);
      setTestResult(null);
      setShowToken(false);
    }
  }, [open]);

  const handleTest = async () => {
    setTesting(true);
    setTestResult(null);
    try {
      await testAiConnection({
        baseUrl,
        token,
        model,
      });
      setTestResult({
        success: true,
        message: "连接成功！配置有效。",
      });
    } catch (error: any) {
      setTestResult({
        success: false,
        message: error?.message || "连接失败，请检查配置或网络。",
      });
    } finally {
      setTesting(false);
    }
  };

  const handleSave = () => {
    saveAiConfig({
      baseUrl: baseUrl.trim(),
      token: token.trim(),
      model: model.trim(),
    });
    onClose();
  };

  return (
    <Dialog open={open} title="AI 配置" onClose={onClose} testId="ai-config-dialog">
      <div className="mx-auto max-w-lg space-y-5">
        <p className="text-xs text-app-muted leading-relaxed">
          此处配置保存在您本地的浏览器缓存中。请在此配置 OpenAI 兼容格式的接口（例如 DeepSeek、OneAPI、或官方 OpenAI 接口）。
        </p>

        {/* API 基础路径 */}
        <label className="block space-y-1.5">
          <span className="text-xs font-semibold text-app-text flex items-center gap-1">
            API 基础路径 (Base URL)
            <span className="text-app-danger">*</span>
          </span>
          <Input
            data-testid="ai-config-baseurl"
            value={baseUrl}
            placeholder="例如: api.deepseek.com 或 api.openai.com"
            onChange={(e) => setBaseUrl(e.target.value)}
          />
          <span className="block text-[11px] text-app-muted">
            接口的基础路径，将自动请求其子路径 <code className="bg-app-surface2 px-1 py-0.5 rounded font-mono">/v1/chat/completions</code>
          </span>
        </label>

        {/* API Key */}
        <label className="block space-y-1.5">
          <span className="text-xs font-semibold text-app-text flex items-center gap-1">
            API Key
            <span className="text-app-danger">*</span>
          </span>
          <div className="relative">
            <input
              type={showToken ? "text" : "password"}
              data-testid="ai-config-token"
              className="w-full rounded-xl border border-app-border bg-app-surface pl-3 pr-10 py-2 text-sm text-app-text outline-none transition placeholder:text-app-muted focus:border-app-accent focus:ring-2 focus:ring-app-accent/20"
              value={token}
              placeholder="请输入您的 API Key (Bearer Token)"
              onChange={(e) => setToken(e.target.value)}
            />
            <button
              type="button"
              className="absolute right-3 top-1/2 -translate-y-1/2 text-app-muted hover:text-app-text transition"
              title={showToken ? "隐藏 API Key" : "显示 API Key"}
              aria-label={showToken ? "隐藏 API Key" : "显示 API Key"}
              onClick={() => setShowToken(!showToken)}
            >
              {showToken ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
        </label>

        {/* 模型名称 */}
        <label className="block space-y-1.5">
          <span className="text-xs font-semibold text-app-text flex items-center gap-1">
            模型名称 (Model)
            <span className="text-app-danger">*</span>
          </span>
          <Input
            data-testid="ai-config-model"
            value={model}
            placeholder="例如: deepseek-chat, gpt-4o 等"
            onChange={(e) => setModel(e.target.value)}
          />
        </label>

        {/* 测试连接结果显示 */}
        {testResult && (
          <div
            className={`flex items-start gap-2.5 rounded-xl border p-3.5 text-xs transition-all ${
              testResult.success
                ? "border-green-500/20 bg-green-500/5 text-green-600 dark:text-green-400"
                : "border-red-500/20 bg-red-500/5 text-red-600 dark:text-red-400"
            }`}
          >
            {testResult.success ? (
              <CheckCircle2 size={16} className="shrink-0 mt-0.5 text-green-500" />
            ) : (
              <AlertCircle size={16} className="shrink-0 mt-0.5 text-red-500" />
            )}
            <div className="flex-1 break-all leading-relaxed">
              <span className="font-semibold block mb-0.5">{testResult.success ? "连接测试通过" : "连接测试失败"}</span>
              {testResult.message}
            </div>
          </div>
        )}

        {/* 底部操作按钮 */}
        <div className="flex items-center justify-between border-t border-app-border pt-4 mt-2">
          <Button
            type="button"
            variant="secondary"
            className="h-9 px-3.5 text-xs flex items-center gap-1.5 hover:border-app-accent hover:text-app-accent transition"
            disabled={testing || !baseUrl.trim() || !token.trim() || !model.trim()}
            onClick={handleTest}
          >
            {testing && <Loader2 size={13} className="animate-spin text-app-accent" />}
            {testing ? "测试中..." : "测试连接"}
          </Button>

          <div className="flex items-center gap-2">
            <Button type="button" variant="ghost" className="h-9 px-4 text-xs" onClick={onClose}>
              取消
            </Button>
            <Button
              type="button"
              variant="primary"
              className="h-9 px-5 text-xs bg-app-accent font-semibold text-white shadow-md hover:brightness-105"
              disabled={!baseUrl.trim() || !token.trim() || !model.trim()}
              onClick={handleSave}
            >
              保存配置
            </Button>
          </div>
        </div>
      </div>
    </Dialog>
  );
}
