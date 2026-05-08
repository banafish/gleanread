## 上下文

当前设置页面（`feature/settings/`）包含 4 个文件：`SettingsRoute.kt`、`SettingsScreen.kt`、`SettingsViewModel.kt`、`SettingsUiState.kt`。UI 以两个 `Card`（账号区 + 同步区）组成，登录表单直接内嵌在设置页面中。主题系统（`core/ui/theme/Theme.kt`）硬编码 `dynamicColor = true`，用户无法切换浅色/深色模式或选择主题颜色。头像功能、外观偏好存储均不存在。

项目依赖注入使用手动 `AppContainer`（非 Hilt），ViewModel 通过 `ViewModelProvider.Factory` 创建。Supabase BOM 3.2.6 已引入 `auth-kt`、`postgrest-kt`、`realtime-kt`，但尚未引入 `storage-kt`。图片加载库（如 Coil）也尚未引入。

导航使用 `NavHost` + 字符串路由方式，设置页面作为底部导航 Tab 之一（`MainRoutes.Settings`），通过 `SettingsRoute()` 挂载。

## 目标 / 非目标

**目标：**
- 重新设计设置页面 UI，使其符合截图风格和 Material You 设计语言
- 实现头像展示与更换（Supabase Storage 存储）
- 实现外观设置（主题模式 + 主题颜色选择）并持久化
- 将登录/注册表单拆分为独立全屏页面
- 退出登录显示在设置页面最后一项，与其他区域有间距分隔

**非目标：**
- 不修改云同步引擎逻辑（仅调整同步区域的 UI 样式）
- 不实现头像裁剪功能（第一版直接上传原图/缩略图）
- 不实现第三方 OAuth 登录（仅邮箱密码 + Magic Link）

## 决策

### D1：外观偏好持久化方案 — Preferences DataStore

**选择**：使用 `androidx.datastore:datastore-preferences` 存储外观偏好（主题模式、主题颜色）。

**替代方案考虑**：
- SharedPreferences：可行但已过时，不支持 Flow 响应式读取
- Proto DataStore：过重，外观偏好仅有两个枚举值，不需要 schema

**理由**：DataStore Preferences 轻量、线程安全、支持 Flow，与 Compose 配合良好。项目中 `DeviceIdentityStore` 和 `WorkspaceSyncStateStore` 已使用类似的 `SharedPreferences` 模式，但新增功能倾向使用更现代的 DataStore。

**数据模型**：
```kotlin
// 主题模式
enum class ThemeMode { LIGHT, DARK, SYSTEM }

// 主题颜色
enum class ThemeColor { DYNAMIC, OCEAN, PURPLE, FOREST, SAKURA, AMBER, GRAPHITE }
```

### D2：主题系统改造 — GleanReadTheme 参数化

**选择**：`GleanReadTheme` 增加 `themeMode: ThemeMode` 和 `themeColor: ThemeColor` 参数，由 `MainActivity` 从 DataStore 读取并传入。

**实现方式**：
1. 新增 `AppearancePreferencesRepository`（读写 DataStore）
2. `MainActivity` 中 `collectAsState` 订阅偏好 Flow
3. `GleanReadTheme` 根据 `themeMode` 决定 `darkTheme` 值，根据 `themeColor` 决定使用动态色还是预设 ColorScheme
4. 预设主题色方案（Ocean、Purple、Forest、Sakura、Amber、Graphite）各自定义一套 `lightColorScheme` / `darkColorScheme`

**替代方案考虑**：
- CompositionLocal 注入主题配置：可行但增加复杂度，不如直接传参数给 Theme composable 简单直接
- 在 ViewModel 层管理主题：主题切换需要在 Activity 级别生效，ViewModel 粒度不合适

### D3：头像存储 — Supabase Storage

**选择**：头像文件上传到 Supabase Storage，以 `avatars/{userId}.jpg` 为路径。

**实现方式**：
1. 新增 Gradle 依赖 `io.github.jan-tennert.supabase:storage-kt`（BOM 已有，直接引入）
2. 新增 `AvatarRepository`，封装上传/下载/URL 获取逻辑
3. 上传流程：用户选择图片 → 压缩/缩放到合理尺寸 → 上传到 Supabase Storage → 缓存 Public URL
4. 头像 URL 缓存在本地 DataStore 中，避免每次启动都请求

**替代方案考虑**：
- 存储在 Supabase Postgrest 表中（Base64）：数据量大，不推荐
- 本地文件存储：不支持多端同步

### D4：图片加载库 — Coil 3

**选择**：引入 `io.coil-kt.coil3:coil-compose` 加载头像。

**理由**：Coil 是 Kotlin-first 的图片加载库，对 Compose 原生支持好，轻量且社区活跃。项目当前没有图片加载库，Coil 3 是最适合的选择。

**替代方案考虑**：
- Glide Compose：可行，但 Coil 在 Compose 生态中更主流
- 手动 Bitmap 加载：工作量大，不支持缓存/占位图

### D5：登录页面拆分 — 独立全屏 composable 路由

**选择**：将登录/注册表单从 `SettingsScreen` 中移出，作为独立的 `AuthRoute` / `AuthScreen` 全屏页面，通过 NavHost 路由导航。页面内支持登录/注册模式切换。

**实现方式**：
1. 新增 `MainRoutes.Auth = "auth"` 路由
2. 新增 `feature/settings/auth/AuthScreen.kt` 和 `AuthRoute.kt`
3. 设置页面头像区域未登录时显示"登录"按钮，点击导航到 Auth 页面
4. Auth 页面包含：
   - **登录模式**：email/password 输入 + 登录按钮 + Magic Link 按钮
   - **注册模式**：email/password/确认密码输入 + 注册按钮
   - 登录/注册模式通过底部文字链接切换（如"没有账号？立即注册" / "已有账号？去登录"）
   - 返回按钮
5. 注册调用 `SupabaseAuthRepository.signUp()` （新增方法，封装 Supabase signUp API）
6. 登录或注册成功后自动 `popBackStack()` 返回设置页

**替代方案考虑**：
- Bottom Sheet：可行，但截图风格偏向全屏页面，且登录+注册表单内容较多
- Dialog：空间不足，体验不好

### D6：设置页面 UI 布局结构

**选择**：按截图风格，设置页面自上而下分为以下区域：

```
┌─────────────────────────┐
│    头像 (圆形, 居中)      │
│    邮箱 / "登录"按钮      │
│    描述文字              │
├─────────────────────────┤
│  外观                    │
│  ┌─────┬─────┬─────┐    │
│  │浅色 │深色 │跟随  │    │
│  └─────┴─────┴─────┘    │
│  主题颜色                │
│  ○动态 ○海洋 ○紫色 ○森林 │
│  ○樱花 ○琥珀 ○石墨      │ ← 主题颜色一行显示不下不换行显示，可以左右滑动查看
├─────────────────────────┤
│  同步                    │
│  同步状态 / 立即同步按钮  │
├─────────────────────────┤
│                          │
│  [ 退出登录 ]  (居中)     │ ← 与上方有明显间距
└─────────────────────────┘
```

- 头像区域：不使用 Card，使用居中列布局，参照截图的简洁风格
- 外观区域：使用 `OutlinedCard` 或 `ElevatedCard`，内含模式选择卡片和色块选择器
- 同步区域：使用与外观区域一致的卡片风格
- 退出登录：`OutlinedButton` 或 `TextButton`，显示在页面最底部，与同步区域有额外间距（如 24.dp）

### D7：文件拆分策略

遵循项目现有的 `feature/settings/` 模块结构，按以下方式组织新文件：

```
feature/settings/
├── SettingsRoute.kt              # 路由入口（保留，调整参数）
├── SettingsScreen.kt             # 主设置页面（大幅重写）
├── SettingsViewModel.kt          # 保留核心逻辑，新增外观偏好方法
├── SettingsUiState.kt            # 扩展新增字段
├── auth/
│   ├── AuthRoute.kt              # 登录页路由入口
│   └── AuthScreen.kt             # 登录页 UI
├── component/
│   ├── UserAvatarSection.kt      # 头像区域组件
│   ├── AppearanceSection.kt      # 外观设置组件
│   └── SyncSection.kt            # 同步区域组件（从 SettingsScreen 提取）
data/
├── appearance/
│   └── AppearancePreferencesRepository.kt  # 外观偏好 DataStore
├── avatar/
│   └── AvatarRepository.kt       # 头像 Supabase Storage 操作
core/ui/theme/
├── Theme.kt                      # 改造支持动态参数
├── OceanColors.kt                # 海洋主题色定义
├── PurpleColors.kt               # 紫色主题色定义
├── ForestColors.kt               # 森林主题色定义
├── SakuraColors.kt               # 樱花主题色定义
├── AmberColors.kt                # 琥珀主题色定义
└── GraphiteColors.kt             # 石墨主题色定义
```

## 风险 / 权衡

| 风险 | 缓解措施 |
|------|----------|
| Supabase Storage bucket 需要提前在 Supabase 控制台创建和配置 RLS | 在任务中明确标注为前置步骤，提供 SQL/配置示例 |
| 新增 Coil 和 DataStore 依赖会增加 APK 体积 | Coil 和 DataStore 均为轻量库，增量很小（< 1MB） |
| 主题切换会触发全量 recomposition | Material3 的 `MaterialTheme` 本身就是这样设计的，性能影响可控；可在后续通过 `CompositionLocal` 优化 |
| 头像上传在弱网环境下可能失败 | 捕获异常并提示用户重试；本地缓存上次成功的头像 URL |
| 登录页面拆分后，ViewModel 需要跨路由共享状态 | `SettingsViewModel` 在 Activity scope 创建，Auth 页面通过 `navBackStackEntry` 或 `activityViewModels` 共享 |
| 注册后邮箱可能需要验证（Supabase 默认配置） | 注册成功后提示用户查收验证邮件；若 Supabase 关闭了邮箱确认则直接登录 |
