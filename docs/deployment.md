# 部署说明

## 1. 部署方式

### 电脑端主机模式

- 适合个人或小范围局域网使用。
- 主电脑同时运行桌面端、API 与本地文件库。
- 主电脑保持在线时，局域网内其他电脑和 Web 端可以围绕主电脑同步。
- 主电脑关闭后，Web 端不可继续使用。

### 服务器中心模式

- 适合团队与长期在线访问。
- 服务器作为统一存储与协作中心。
- 各知识库主电脑继续保留本地备份。

## 2. 环境变量

- 后端模板：apps/api/.env.example
- Docker API 模板：deploy/docker/api.env.example
- Docker Web 模板：deploy/docker/web.env.example
- Web 本地模板：apps/web/.env.example
- Desktop 本地模板：apps/desktop/.env.example
- Mobile 配置入口：apps/mobile/src/utils/config.ts

生产环境至少要修改：

- JWT_SECRET
- ADMIN_PASSWORD
- VITE_API_BASE
- AI_API_BASE / AI_API_KEY
- DEPLOYMENT_MODE
- EDITION
- INSTANCE_NAME

新增模块相关建议：

- 密码管家依赖 `cryptography`，部署镜像或虚拟环境必须包含该依赖。
- 若要让目标拆解走真实模型，应同时配置可用的 AI Provider Base URL、Key 和模型名。

团队版额外建议配置：

- TEAM_AUTHORIZATION_CODE
- TEAM_INVITE_BASE_URL

## 3. 首次初始化

首次启动后，系统会自动生成唯一 `InstanceId`。

然后以管理员身份调用：

- `PUT /api/v1/instance/bootstrap`

初始化项包括：

- `deployment_mode`: `desktop` 或 `server`
- `edition`: `personal` 或 `team`
- `instance_name`
- `authorization_code`: 团队版必填

团队版只有在授权码校验通过后才会激活多用户与团队共享能力。

## 4. Docker 部署

在 deploy/docker 下准备两个文件：

- api.env
- web.env

然后执行：

```powershell
Set-Location deploy/docker
docker compose up -d --build
```

默认端口：

- API: 8000
- Web: 8080

## 5. 本地开发

### 后端

```powershell
Set-Location apps/api
d:/AI编程/个人双向知识库/.venv/Scripts/python.exe -m pip install -e .[dev]
d:/AI编程/个人双向知识库/.venv/Scripts/python.exe -m uvicorn app.main:app --reload
```

### Web

```powershell
Set-Location apps/web
pnpm install
pnpm dev
```

### Desktop

```powershell
Set-Location apps/desktop
pnpm install
pnpm dev
```

## 6. 回归验证

```powershell
Set-Location apps/api
d:/AI编程/个人双向知识库/.venv/Scripts/python.exe -m pytest tests/test_api_smoke.py
```

当前已验证通过：

- 既有认证、笔记、搜索、RAG、剪藏和空间流转烟测。
- 密码管家初始化、校验、创建、解锁、复制审计烟测。
- 目标创建、AI 拆解、阶段、任务、复盘和概览烟测。
- Web 生产构建。

## 7. 团队版运营建议

- 管理员激活团队版后再生成邀请链接。
- 每位成员默认拥有专属知识空间。
- 笔记默认私有，只有显式公开到团队后才进入团队搜索与 RAG。

## 8. 当前上线前差距

- Tauri 原生打包仍需安装 Rust toolchain。
- UniApp 仍需在 HBuilderX 或对应 CLI 下做真机验证。
- AI 目前支持真实 API 接入，也保留本地兜底生成逻辑；生产环境应切到真实模型服务。
- 团队版当前已具备实例、邀请、成员、分享和搜索/RAG 可见性边界；更细粒度的实时协作、审批流与组织架构仍可继续扩展。

## 9. 新模块上线注意事项

### 密码管家

- 首次上线后需要由用户主动设置主密码，系统不会预置默认主密码。
- 若已有持久化数据库，测试环境应避免反复变更主密码；否则需清理对应密码库配置后再重新初始化。
- 团队版共享密码条目时，仍受团队成员可见性边界约束。

### 目标计划与日记联动

- 系统默认把联动内容写入 `05_Daily/YYYY-MM-DD.md`。
- 若用户已有自定义日记目录，需要在后续版本中做成配置项；当前版本先固定到统一目录，保证多端一致。
