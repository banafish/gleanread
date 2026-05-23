# 规范：web-auth-shell

## Purpose
定义 GleanRead Web 端的独立登录页、认证回调页和会话切换规则，确保用户可以从产品入口进入登录流程，并在认证成功后稳定回到受保护工作台。

## Requirements
### Requirement: 登录页必须作为独立全屏入口存在
系统 MUST在 `/login` 提供独立全屏登录页，并使用左侧品牌展示区与右侧表单区的分栏布局。登录页必须与主工作台分离，用户在该页面只处理认证输入，而不是浏览工作台内容。

#### Scenario: 访问登录页
- **WHEN** 用户直接访问 `/login`
- **THEN** 系统 MUST展示独立全屏登录页
- **THEN** 页面必须保留品牌展示区和表单区的分栏结构

#### Scenario: 已登录用户访问登录页
- **WHEN** 用户已经持有有效会话但再次打开 `/login`
- **THEN** 系统 MUST引导其返回 `/app`
- **THEN** 系统不得让已登录用户停留在登录表单中

### Requirement: 登录页必须支持邮箱密码、Magic Link 和注册模式
系统 MUST在登录页提供邮箱密码登录、Magic Link 登录以及注册模式。注册模式必须要求用户再次确认密码；提交前必须验证密码和确认密码一致。页面必须允许用户在登录与注册模式之间切换，并保留当前输入上下文。

#### Scenario: 邮箱密码登录
- **WHEN** 用户输入邮箱和密码并提交登录
- **THEN** 系统 MUST尝试建立 Supabase 会话
- **THEN** 成功后必须进入认证后的工作台流程

#### Scenario: Magic Link 登录
- **WHEN** 用户点击 Magic Link 登录入口
- **THEN** 系统 MUST向用户邮箱发送认证链接
- **THEN** 系统 MUST提示用户查收邮件

#### Scenario: 注册时密码不一致
- **WHEN** 用户在注册模式下输入的密码与确认密码不一致
- **THEN** 系统 MUST阻止提交
- **THEN** 系统 MUST显示密码不一致提示

### Requirement: 登录页必须支持 OAuth 入口
系统 MUST在登录页提供 OAuth 登录入口，入口可以与邮箱密码区域并列展示。用户点击 OAuth 登录后，系统 MUST跳转至相应外部认证流程，并在认证完成后回到 `/auth/callback`。

#### Scenario: 点击 OAuth 登录
- **WHEN** 用户点击任一 OAuth 提供方入口
- **THEN** 系统 MUST进入对应的外部认证流程
- **THEN** 认证完成后必须回到回调页

### Requirement: `/auth/callback` 必须完成会话接收并重定向
系统 MUST在 `/auth/callback` 接收 Supabase 返回的认证信息，完成会话持久化和临时参数清理后跳转到 `/app`。回调页在处理期间必须展示加载态，若认证失败则必须显示可重试的错误态，而不是卡死在空白页。

#### Scenario: 认证成功回调
- **WHEN** 用户带着有效认证信息进入 `/auth/callback`
- **THEN** 系统 MUST完成会话写入
- **THEN** 系统 MUST清理当前回调页的临时认证参数
- **THEN** 系统 MUST重定向到 `/app`

#### Scenario: 认证失败回调
- **WHEN** 回调页收到无效、过期或缺失的认证信息
- **THEN** 系统 MUST显示错误态
- **THEN** 系统 MUST允许用户返回登录页重试

### Requirement: 认证状态必须驱动路由守卫
系统 MUST根据当前会话状态守卫 `/app`、`/login` 和 `/auth/callback` 的路由行为。未登录时访问 `/app` 必须被引导到登录流程；已登录时访问登录页或完成回调后必须回到工作台。

#### Scenario: 未登录访问工作台
- **WHEN** 用户没有有效会话却访问 `/app`
- **THEN** 系统 MUST将其引导到登录流程

#### Scenario: 登录后进入工作台
- **WHEN** 用户完成认证并获得有效会话
- **THEN** 系统 MUST把用户带回 `/app`
- **THEN** 系统不得要求用户重复登录
