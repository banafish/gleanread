## 1. 基础设施与依赖

- [x] 1.1 在 `build.gradle.kts` 中添加 `datastore-preferences`、`storage-kt`、`coil-compose` 依赖
- [x] 1.2 在 Supabase 控制台创建 `avatars` Storage bucket 并配置 RLS 策略（仅允许认证用户上传/读取自己的头像）
- [x] 1.3 创建 `data/appearance/AppearancePreferencesRepository.kt`，实现 DataStore 读写 `ThemeMode` 和 `ThemeColor`
- [x] 1.4 创建 `ThemeMode` 和 `ThemeColor` 枚举类（LIGHT/DARK/SYSTEM 和 DYNAMIC/OCEAN/PURPLE/FOREST/SAKURA/AMBER/GRAPHITE）
- [x] 1.5 在 `AppContainer` 中注册 `AppearancePreferencesRepository` 实例

## 2. 主题系统改造

- [x] 2.1 创建 `OceanColors.kt`、`PurpleColors.kt`、`ForestColors.kt`、`SakuraColors.kt`、`AmberColors.kt`、`GraphiteColors.kt`，各定义一套 `lightColorScheme` / `darkColorScheme`
- [x] 2.2 改造 `Theme.kt` 中的 `GleanReadTheme`，增加 `themeMode: ThemeMode` 和 `themeColor: ThemeColor` 参数，根据参数决定 darkTheme 和 colorScheme
- [x] 2.3 改造 `MainActivity.kt`，从 `AppearancePreferencesRepository` collectAsState 订阅偏好并传入 `GleanReadTheme`
- [x] 2.4 改造 `FastCaptureActivity`（如有使用 GleanReadTheme），同步支持主题参数

## 3. 头像存储

- [x] 3.1 创建 `data/avatar/AvatarRepository.kt`，封装 Supabase Storage 上传/下载/获取 Public URL 逻辑
- [x] 3.2 实现图片压缩/缩放工具方法（上传前将图片调整到合理尺寸）
- [x] 3.3 在 `AppearancePreferencesRepository` 或单独 DataStore 中增加头像 URL 本地缓存
- [x] 3.4 在 `AppContainer` 中注册 `AvatarRepository` 实例

## 4. 认证功能扩展

- [x] 4.1 在 `SupabaseAuthRepository` 中新增 `signUp(email, password)` 方法，封装 Supabase signUp API
- [x] 4.2 创建 `feature/settings/auth/AuthScreen.kt`，实现登录/注册全屏页面 UI（支持模式切换：登录 ↔ 注册）
- [x] 4.3 创建 `feature/settings/auth/AuthRoute.kt`，连接 ViewModel 与 AuthScreen
- [x] 4.4 在 `MainRoutes` 中新增 `Auth = "auth"` 路由，在 `MainApp.kt` NavHost 中注册 Auth 页面
- [x] 4.5 实现注册模式下的确认密码校验（两次密码一致性检查）

## 5. 设置页面 UI 重设计

- [x] 5.1 创建 `feature/settings/component/UserAvatarSection.kt`，实现头像区域组件（已登录/未登录两种状态）
- [x] 5.2 创建 `feature/settings/component/AppearanceSection.kt`，实现外观设置组件（主题模式卡片 +主题颜色水平滑动选择器）
- [x] 5.3 创建 `feature/settings/component/SyncSection.kt`，将同步区域从 SettingsScreen 中提取为独立组件并适配新卡片样式
- [x] 5.4 重写 `SettingsScreen.kt`，按新布局结构组装：头像区域 → 外观卡片 → 同步卡片 → 退出登录按钮（底部间距分隔）
- [x] 5.5 扩展 `SettingsUiState.kt`，新增 `themeMode`、`themeColor`、`avatarUrl`、`isAvatarUploading` 等字段
- [x] 5.6 更新 `SettingsViewModel.kt`，集成 `AppearancePreferencesRepository` 和 `AvatarRepository`，新增外观切换和头像更换方法
- [x] 5.7 更新 `SettingsRoute.kt`，调整参数传递（增加导航到 Auth 页面的回调、外观偏好回调、头像相关回调）

## 6. 头像交互

- [x] 6.1 在 `UserAvatarSection` 中实现点击头像弹出选择方式（相册/拍照）
- [x] 6.2 集成 `ActivityResultContracts.PickVisualMedia` 或 `GetContent` 实现相册选取
- [x] 6.3 集成 `ActivityResultContracts.TakePicture` 实现拍照获取
- [x] 6.4 实现头像上传流程：选择图片 → 压缩 → 上传 Supabase Storage → 更新 UI
- [x] 6.5 使用 Coil `AsyncImage` 加载头像（支持占位图和错误图）

## 7. 验证与收尾

- [x] 7.1 为 SettingsScreen 新增 Preview（已登录/未登录两种状态）
- [x] 7.2 为 AuthScreen 新增 Preview（登录模式/注册模式）
- [x] 7.3 为 AppearanceSection 新增 Preview
- [x] 7.4 确认主题切换在浅色/深色/跟随系统下均正确工作
- [x] 7.5 确认所有 7 种主题颜色切换后 UI 正确显示
- [x] 7.6 执行 Gradle build 确保编译通过
