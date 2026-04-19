# 项目目录结构

```text
private-knowledge-cloud/
├─ apps/
│  ├─ api/
│  │  ├─ app/
│  │  │  ├─ api/v1/
│  │  │  ├─ core/
│  │  │  ├─ db/
│  │  │  ├─ models/
│  │  │  ├─ schemas/
│  │  │  ├─ services/
│  │  │  └─ workers/
│  │  └─ pyproject.toml
│  ├─ web/
│  │  ├─ src/
│  │  │  ├─ api/
│  │  │  ├─ components/
│  │  │  ├─ router/
│  │  │  ├─ stores/
│  │  │  ├─ styles/
│  │  │  └─ views/
│  │  └─ package.json
│  ├─ desktop/
│  │  ├─ src/
│  │  ├─ src-tauri/
│  │  │  └─ src/
│  │  └─ package.json
│  └─ mobile/
│     ├─ src/
│     │  └─ pages/
│     ├─ package.json
│     └─ pages.json
├─ packages/
│  └─ shared/
│     └─ src/
├─ deploy/
│  └─ docker/
├─ docs/
├─ package.json
└─ pnpm-workspace.yaml
```

## 分层职责

- `apps/api`：统一 API、同步编排、索引任务入口。
- `apps/web`：浏览器知识工作台。
- `apps/desktop`：Tauri 桌面容器与本地同步代理入口。
- `apps/mobile`：UniApp 移动端。
- `packages/shared`：共享类型、接口常量、领域模型。
- `deploy/docker`：云端部署样板。
