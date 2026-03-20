# Changelog

本文件用于记录每次版本迭代的重要变更，遵循 Keep a Changelog 风格。

## [Unreleased]
### Added
- 初始化供应链统筹任务管理平台整体架构与基础代码。
- 新增前端核心页面：任务提交、列表、配置管理、数据大屏。
- 新增后端 Spring Boot 骨架：任务/配置/报表/消息 API、统一异常、鉴权拦截。
- 新增数据库初始化脚本与 OpenAPI 文档。

### Changed
- README 增加交付清单与运行说明。

### Fixed
- N/A

## [2026-03-20] - 架构与骨架初始化
### Added
- docs/01-架构设计基线.md
- docs/02-整体项目架构设计.md
- docs/03-数据库初始化脚本.sql
- docs/04-接口文档-openapi.yaml
- docs/05-测试用例.md
- src/views/SupplyTask/
- server/supply-task-service/
- deploy/frontend-deploy.ps1
- deploy/backend-deploy.ps1
