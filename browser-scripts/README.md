# browser-scripts

浏览器 userscript 脚本集合。包含适用于主流浏览器（X浏览器、Edge、Firefox、狐猴浏览器、Kiwi 等）的网页划词摘录脚本。

[简体中文](./README.md) | [English](./README_EN.md)

---

## 📌 glean-web-capture.user.js — 划词摘录

在网页上长按或鼠标划词选中一段文本后，选区边缘将自动浮现**「摘」**悬浮圆钮。轻点按钮即可通过 Deep Link 直接拉起 Android 端 GleanRead 的快摘弹窗（FastCaptureActivity）。

### 🌟 核心优势

- **数据原子送达**：当前网页 URL（`location.href`）、页面标题（`document.title`）与选中文本随启动 Intent / Deep Link 一次性完整传递，无需等待无障碍服务后台抓取。
- **免打扰独立任务**：快摘弹窗以独立临时 Task（`taskAffinity=""` + `excludeFromRecents`）运行，摘录完毕关闭弹窗后立即返回当前浏览页面，不拉起后台 GleanRead 主界面，不打断阅读心流。
- **不怕进程冻结**：绕过 Android 部分机型（如荣耀、华为、小米等）对无障碍后台服务的激进冻结策略，纯靠系统 Deep Link 协议直达，稳定可靠。

---

## 🛠️ 各浏览器安装与使用指引

### 1. X浏览器 (Android)

X浏览器内置轻量级脚本管理器，无需额外安装扩展插件：

1. 打开 X浏览器 → 点击底栏菜单 → **工具箱 → 脚本**（部分版本显示为「用户脚本 / 拓展脚本」）。
2. 点击 **新建脚本**（或添加脚本），将 [`glean-web-capture.user.js`](./glean-web-capture.user.js) 的全部代码复制并粘贴进去，或者选择从本地文件导入。
3. 保存并确认已启用脚本，检查脚本匹配规则包含所有网址（`@match *://*/*`）。
4. 打开任意新闻或技术文章页面，长按选中文本 → 松手后按钮浮现在选区下方 → 点击「摘」即可拉起 GleanRead 快摘弹窗。

---

### 2. Microsoft Edge 浏览器

#### Android 移动端 Edge
1. 确保使用的是支持扩展的 Edge 版本（如 Edge Canary/Dev 或开启扩展功能的稳定版）。
2. 打开菜单 → **扩展 (Extensions)**，从应用商店搜索并安装 **Tampermonkey**（篡改猴）或 **Violentmonkey**（暴力猴）。
3. 点击 Tampermonkey 图标进入仪表盘，点击「+」添加新脚本，粘贴 `glean-web-capture.user.js` 代码并保存。
4. 在网页中选中文本，点击浮出的「摘」按钮。Edge 默认支持 Chromium 标准 intent URI（`useIntentUri: true`），能顺畅拉起 GleanRead。

#### 桌面端 Edge (Windows / macOS)
1. 访问 [Edge 扩展商店](https://microsoftedge.microsoft.com/addons) 安装 **Tampermonkey** 或 **Violentmonkey**。
2. 导入并启用 `glean-web-capture.user.js`。
3. 在电脑网页中用鼠标划词选中文本，点击浮出的「摘」按钮。
4. 此时浏览器会通过自定义协议 `gleanread://capture` 唤起外部应用。如果配合 Windows Subsystem for Android (WSA) 或配置了跨端链路，可直接拉起对应快摘；或在手机浏览器中获得完整移动快摘体验。

---

### 3. Mozilla Firefox 浏览器

#### Android 移动端 Firefox
Firefox 移动版对扩展生态具有原生级支持，兼容性极佳：

1. 打开 Firefox for Android → 点击右上角/右下角三个点菜单 → **附加组件 (Add-ons)**。
2. 找到并安装 **Tampermonkey** 或 **Violentmonkey**。
3. 打开扩展管理界面，新建脚本并粘贴 `glean-web-capture.user.js` 内容，保存并启用。
4. **跳转形式调优（重要）**：
   - Firefox 内核对 `intent://` 语法的处理与 Chromium 略有不同。如果点击「摘」按钮后 Firefox 提示“无法打开此地址”或无响应，请编辑脚本代码顶部配置项：
     ```javascript
     var CONFIG = {
         useIntentUri: false, // 改为 false，启用 gleanread:// scheme 直跳
         ...
     };
     ```
   - 改为 `false` 后，点击按钮 Firefox 会直接识别并提示“在外部应用中打开”或直接由系统分发至 GleanRead。

#### 桌面端 Firefox
1. 访问 [Firefox 附加组件商店](https://addons.mozilla.org) 安装 **Tampermonkey**。
2. 导入并保存脚本。划词选中后点击「摘」，Firefox 会提示允许打开 `gleanread://` 协议链接。

---

### 4. 狐猴浏览器 (Lemur Browser) / Kiwi Browser (Android)

这两款移动浏览器完整支持 Chrome Web Store 扩展，推荐追求桌面级扩展体验的用户使用：

1. 打开应用自带的扩展商店或访问 Chrome Web Store，搜索并安装 **Tampermonkey**。
2. 打开 Tampermonkey 仪表盘，新建脚本并填入 `glean-web-capture.user.js`。
3. 打开网页选中任意正文，即可稳定弹出「摘」并唤起快摘。

---

## ⚙️ deep link 触达形式（intent vs scheme）

脚本顶部 `CONFIG.useIntentUri` 用于控制调起 Android 应用的协议格式：

| 触达形式 | 配置值 | 适用场景与说明 |
|---|---|---|
| **intent URI（默认）** | `useIntentUri: true` | Chromium 标准语法：`intent://capture?url=...#Intent;scheme=gleanread;package=com.gleanread.android;end`<br>包含 `package=` 声明防止被恶意应用劫持，Chromium 系浏览器（Chrome、Edge、Kiwi、X浏览器等）优先推荐。 |
| **scheme 直跳** | `useIntentUri: false` | 标准自定义 Scheme：`location.href = "gleanread://capture?..."`<br>当浏览器对 `intent:` 语法解析异常、报错“无效网址”或在 **Firefox** 浏览器下无法拉起时，切换为本模式。 |

> **建议**：默认保持 `useIntentUri: true`。若点击按钮无任何反应或浏览器提示网址无效，改为 `useIntentUri: false` 保存后刷新重试。

---

## 🔔 「是否允许打开外部应用」提示说明

- **安全机制**：现代 Android 浏览器对网页脚本发起的外部应用跳转通常设有系统级确认提示（如底部弹出“是否允许打开外部应用：GleanRead”）。
- **正常操作**：点击「摘」按钮后，在浏览器弹窗上选择“允许”即可。部分浏览器支持勾选“对此网站始终允许”或“记住我的选择”。
- **X 浏览器特性**：X 浏览器对页面触发的应用跳转默认每次均弹提示且不记录白名单，点击一下「允许」即可完成收集，不影响摘录数据与功能完整性。

---

## 🔧 脚本自定义参数 (`CONFIG`)

打开脚本源代码，在文件顶部可调整以下参数：

```javascript
var CONFIG = {
    useIntentUri: true,       // true: intent URI; false: gleanread:// scheme
    packageName: 'com.gleanread.android', // 目标应用包名
    maxTextLength: 6000,      // 选中文本最大长度截断（防止超长 URL 溢出）
    buttonOffsetY: 24,        // 按钮与选区下边缘的距离 (px)，避免被系统选择菜单遮挡
    debounceMillis: 250,      // 选区变动防抖间隔 (ms)
    debug: false,             // 控制台调试日志开关（排查问题时设为 true）
};
```

---

## ❓ 常见问题与排查步骤

若在普通网页中选中文字后按钮未正常出现，请按以下顺序排查：

1. **确认脚本已成功注入**：
   - 打开浏览器控制台（Console），若能看到 `[GleanRead] script v... installed` 日志，或在控制台执行 `window.__gleanWebCaptureInstalled` 返回 `true`，代表脚本已运行。
   - 若返回 `undefined`：检查扩展管理器中脚本是否处于「启用」状态，匹配规则是否包含当前网址。
   - 注：浏览器内置的“阅读模式”、小说模式或本地 `file://` 页面部分浏览器禁止运行脚本，请在常规 `https://` 网页验证。
2. **确认选区事件通道正常**：
   - 长按选中文本后，控制台应输出 `[GleanRead] first selection-related event received`。
   - 若内容位于 `<iframe>` 框架内，出于跨域安全机制，脚本默认不在非顶层 iframe 内弹窗。
3. **点击按钮后无反应**：
   - 控制台观察 `[GleanRead] navigate: ...` 日志输出。
   - 若报语法错误或网址无效，请将 `CONFIG.useIntentUri` 切换为 `false`。
4. **已知限制**：
   - 网页页面滚动时按钮自动隐藏，重新微调选区后恢复浮现。
   - 表单输入框（`input`、`textarea`）及 `contenteditable` 富文本编辑框内选中文本不触发摘录按钮。
