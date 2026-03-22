## 环境要求

- nodejs >= `20`
- pnpm = `8`

## 快速入门

- [更多文档](https://ruijie.feishu.cn/wiki/QOGHwvt8vim4RRkhxxxc5AsMnje)

```bash
# 1. 安装：
pnpm install

# 2. 目录：src/views下新建代码
- src/views/HelloWorld/index.vue

# 3. 输出：修改根目录下views.js
module.exports = {
  '/views/HelloWorld/index.js?label=你好世界': './src/views/HelloWorld/index.vue'
}
# 编写代码

# 4. 运行： -> 访问: /_
npm run serve

# 5. 编译： -> 产物: public/views/HelloWorld/index.js
npm run build

!!! /_ 路由仅在开发阶段生效，需部署到生产请接入带权限的菜单接口
```

## AI编程

- [更多文档](https://ruijie.feishu.cn/wiki/JRjywYxLdi87W1keVVdccbH2nhh)

```bash
# 拉取技能包然后开始对话
bash <(curl -fsSL http://one.ruijie.com.cn/libs/skills.sh)
```

## 项目架构文档

- 第一阶段（架构基线）：./docs/01-架构设计基线.md
- 第二阶段（整体架构设计）：./docs/02-整体项目架构设计.md

## 全量交付清单

- 架构基线文档：./docs/01-架构设计基线.md
- 整体架构设计：./docs/02-整体项目架构设计.md
- 系统管理架构与流程：./docs/10-系统管理模块架构与流程.md
- 系统管理建表脚本：./docs/11-系统管理模块建表.sql
- 系统管理表结构说明：./docs/12-系统管理表结构设计说明.md
- 系统管理使用手册：./docs/13-系统管理模块使用手册.md
- 系统管理开发手册：./docs/14-系统管理模块开发手册.md
- 系统管理测试用例：./docs/15-系统管理模块测试用例.md
- 系统管理 OpenAPI：./docs/16-系统管理模块-openapi.yaml
- 辅助模块架构与流程：./docs/24-辅助功能模块架构与流程.md
- 辅助模块建表脚本：./docs/25-辅助功能模块建表.sql
- 辅助模块表结构说明：./docs/26-辅助功能模块表结构说明.md
- 辅助模块使用手册：./docs/27-辅助功能模块使用手册.md
- 辅助模块开发手册：./docs/28-辅助功能模块开发手册.md
- 辅助模块测试用例：./docs/29-辅助功能模块测试用例.md
- 辅助模块 OpenAPI：./docs/30-辅助功能模块-openapi.yaml
- 数据库初始化脚本：./docs/03-数据库初始化脚本.sql
- OpenAPI 文档：./docs/04-接口文档-openapi.yaml
- 核心测试用例：./docs/05-测试用例.md
- 前端核心页面：./src/views/SupplyTask/
- 前端系统管理页面：./src/views/SystemManage/
- 后端核心代码骨架：./server/supply-task-service/
- 后端系统管理服务：./server/supply-system-service/
- 部署脚本：./deploy/frontend-deploy.ps1、./deploy/backend-deploy.ps1
- 版本变更记录：./CHANGELOG.md
- 版本与迭代管理规范：./docs/06-代码版本与迭代管理规范.md
- 需求与变更台账：./docs/07-需求与变更记录.md
- 环境与缺失配置清单：./docs/08-环境与缺失配置清单.md
- 本机环境安装指南：./docs/09-本机环境安装指南.md
- 关键节点备份脚本：./scripts/git-keypoint-backup.ps1
- 环境自检脚本：./scripts/check-env.ps1
- 本地数据库编排：./deploy/docker-compose.local.yml

## 前端访问入口

- /
- /?mode=management&tab=unordered
- /?mode=management&tab=urgent
- /?mode=management&tab=delivery
- /?mode=management&tab=operation
- /?mode=management&tab=auxiliary
- /?mode=management&tab=system

## 用户端智能解析配置

- Kimi 配置文件：./src/config/ai/kimi-config.json
- 提示词配置：./src/config/ai/task-prompts.json
- 将 `apiKey` 填入后，用户端页面将调用 Kimi 进行任务分类与字段抽取。
- 未填写 `apiKey` 时，前端自动使用本地兜底规则解析，便于联调页面流程。

## 后端本地运行（示例）

```bash
cd server/supply-task-service
mvn clean package -DskipTests
java -jar target/supply-task-service-1.0.0.jar
```

Swagger 地址（启动后）：

- /swagger-ui.html

## 版本管理与需求管理

- Git 已初始化，默认分支为 main。
- 需求登记和迭代记录：./docs/07-需求与变更记录.md
- 代码变更记录：./CHANGELOG.md
- 关键节点备份：

```bash
powershell -File ./scripts/git-keypoint-backup.ps1 -Version 1.1.0 -Message "M1 已下单加急模块完成"
```

## 数据库与配置说明

- 当前仓库默认未连接真实数据库，后端为骨架示例实现。
- 可直接启动本地 MySQL/Redis（数据落本机容器卷）：

```bash
cd deploy
docker compose -f docker-compose.local.yml up -d
```

- 开发环境配置：./server/supply-task-service/src/main/resources/application-dev.yml
- 生产环境配置模板：./server/supply-task-service/src/main/resources/application-prod.yml
