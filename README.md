# 私有化云端知识库系统

这是一个面向个人知识管理的多端私有化知识库 monorepo，目标能力包括：

- Markdown 笔记与本地文件库双向管理
- 类 Obsidian 的双向链接、反向链接、关系图谱与工作区交互
- 密码管家，支持主密码派生密钥、条目加密、审计与统一搜索
- 目标计划与日记联动，支持目标拆解、今日任务与日记复盘同步
- 浏览器、桌面端、移动端统一访问与编辑
- 云服务器与主电脑双备份一致性存储
- 云端 RAG 向量索引，供 AI 创作与检索调用

说明：界面和交互会采用 Obsidian 风格的产品方向，但不会复制其品牌素材、图标或逐像素专有设计。

## 技术栈

- 后端：FastAPI + SQLModel + SQLite
- Web：Vue 3 + Vite + Pinia + Vue Router + Vditor
- 桌面端：Tauri + Vue 3
- 移动端：UniApp
- 元数据：SQLite
- 文件库：Markdown 原始文件 + 附件目录
- RAG：云端文件扫描 + 切片 + 向量化 + 检索 API

## 核心原则

1. 文件优先：Markdown 与附件作为一等公民，元数据只做索引与协同。
2. 云端主链路：所有在线编辑先到云端，再由云端主动同步到主电脑。
3. 双备份一致性：云服务器与主电脑都保留完整 vault。
4. 多端同构：Web、桌面端、移动端共享知识模型与接口契约。
5. AI 可调用：云端始终维护完整向量库，用于 RAG 与写作助手。

## 目录概览

详细目录见 [docs/project-tree.md](docs/project-tree.md)。

## 架构文档

- 整体架构与双备份/RAG 流程：[docs/architecture.md](docs/architecture.md)
- 部署与上线说明：[docs/deployment.md](docs/deployment.md)
- UI 规范与三端设计基线：[docs/ui-guidelines.md](docs/ui-guidelines.md)

## 当前可用能力

1. FastAPI 后端已具备 JWT、笔记 CRUD、目录树、双向链接、全文检索、网页剪藏、RAG 查询、AI 生成、同步事件与 ack。
2. FastAPI 后端已接入密码管家与目标计划模块，包含主密码初始化与校验、密码生成、加密存储、密码审计、目标拆解、阶段计划、任务切换、日记复盘与今日日记联动。
3. Web 端已接入登录、首页搜索/剪藏、目录编辑、自动保存、反向链接、知识图谱、AI 面板与同步状态显示。
4. Web 工作台首页已固定展示目标计划与今日任务，统一搜索可以直接打开笔记或密码条目，并新增独立密码管家页面。
5. 桌面端已接入登录、实时同步事件拉取、本地副本目录配置、自动落盘同步与状态确认。
6. 安卓端已接入首页、笔记、图谱、AI 四分区工作台，支持登录、剪藏、笔记浏览、关系速览与 AI 调用。

## 新增模块

### 1. 知识库密码管家

- 主密码只用于派生加密密钥，不作为明文存储。
- 密码条目支持标题、账号、网站、备注、关联笔记、可见范围与审计日志。
- 支持强密码生成、解锁查看、复制审计、统一搜索跳转和团队共享边界。
- 后端核心入口位于 apps/api/app/api/v1/routes.py 与 apps/api/app/services/password_manager.py。
- Web 入口位于 apps/web/src/views/WorkspaceStudioView.vue 的 passwords 模式。

### 2. 目标计划与日记联动

- 支持目标、阶段计划、任务、复盘四层结构。
- 支持 AI 拆解目标；当远端模型不可用时，后端会走本地兜底拆解逻辑。
- 今日任务可直接切换完成状态，并同步到今日日记。
- 复盘保存后会回写到 05_Daily 下的对应日期笔记，实现目标与日记共用一套 Markdown 资产。
- 后端核心入口位于 apps/api/app/api/v1/routes.py；Web 首页与日记页入口位于 apps/web/src/views/WorkspaceStudioView.vue。
3. 桌面端已接入登录、实时同步事件拉取、本地副本目录配置、自动落盘同步与状态确认。
4. 安卓端已接入首页、笔记、图谱、AI 四分区工作台，支持登录、剪藏、笔记浏览、关系速览与 AI 调用。

## 开发建议顺序

1. 完成 FastAPI 元数据与同步 API。
2. 完成 Web 端知识库工作台与编辑器交互。
3. 完成桌面端本地文件代理与主电脑常驻同步。
4. 完成 UniApp 移动端浏览、编辑、搜索与离线缓存。
5. 接入向量化引擎与 AI 创作接口。

## 快速启动

1. 复制 [apps/api/.env.example](apps/api/.env.example) 为 `.env` 并调整密钥。
2. 在仓库根目录创建并激活 `.venv` 后，安装后端依赖并启动 API。
3. 在 [apps/web/.env.example](apps/web/.env.example) 中配置 API 地址后启动 Web。
4. 桌面端同理使用 [apps/desktop/.env.example](apps/desktop/.env.example)，并在首次连接后配置本地副本目录。
5. 如需运行 Tauri 桌面壳，请先安装 Rust toolchain；仅执行前端构建可直接使用 pnpm。
6. 容器部署见 [docs/deployment.md](docs/deployment.md)。

推荐本地顺序：

1. 在仓库根目录激活 `.venv`。
2. 进入 apps/api 安装 Python 依赖并启动 FastAPI。
3. 在仓库根目录执行 `pnpm --filter web dev` 启动 Web 工作台。
4. 访问首页后，先完成实例初始化或直接使用默认管理员账号进入系统。

## 本地命令

1. `pnpm seed:api`：写入本地演示数据。
2. `pnpm dev:api`：启动 FastAPI。
3. `pnpm dev:web`：启动 Web。
4. `pnpm --dir apps/desktop build`：构建桌面端前端控制台。
5. `pnpm --dir apps/desktop tauri dev`：启动桌面壳与本地副本代理。
6. `pnpm test:api`：执行后端测试。

## 已完成验证

1. 后端烟雾测试已覆盖原有主链路，以及密码管家与目标计划/日记联动流程。
2. Web 前端已完成生产构建验证。
3. 当前开发机上桌面端前端构建可通过，但 Tauri 原生调试仍依赖 Rust toolchain。
