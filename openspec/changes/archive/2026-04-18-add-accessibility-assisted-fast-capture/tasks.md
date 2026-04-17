## 1. 基础链路

- [x] 1.1 新增 `PageContextSnapshot`、`PageContextStore` 与 `CaptureSeedResolver`，实现 `Intent` 来源与最近页面上下文缓存的优先级合并、来源匹配和 TTL 校验
- [x] 1.2 扩展 `FastCaptureActivity` 与 Manifest，使外部快摘同时支持 `ACTION_SEND`、`ACTION_PROCESS_TEXT`，并把 `sourceTitle` 与 `url` 贯通到保存入口
- [x] 1.3 新增可选无障碍辅助识别服务，对受支持宿主写入最近页面上下文缓存，并保证服务关闭时标准分享路径不受影响

## 2. Review Gate

- [x] 2.1 完成 1.1-1.3 后暂停实现，等待用户 review 并明确确认后，才继续执行后续任务

## 3. 来源展示与引导

- [x] 3.1 更新 `CaptureDialogV2`、`RichExcerptCard` 与相关来源展示组件，使弹窗能展示 `sourceTitle`、来源域名和“最近页面补齐”提示
- [x] 3.2 在来源仍缺失且宿主受支持时增加非阻塞的辅助识别引导，并保持手动链接输入与直接保存的降级路径可用

## 4. 验证与收尾

- [x] 4.1 为来源合并规则、`sourceTitle` 持久化和外部快摘入口补充单元测试或 Robolectric 测试
- [ ] 4.2 手动验证 Chrome 与微信公众号中的整页分享、选中文字分享和文本处理入口，记录命中缓存、降级和已知限制
