# 需求与迭代记录

## 项目约束
- 后续所有功能必须采用 MyBatis + MySQL 真实落库。
- 新增接口默认遵循 Controller -> Service -> Mapper -> XML -> MySQL 分层链路。
- 每个需求完成后必须补充本文件记录（需求、改动点、验证结果、风险）。

## 系统整体页面功能架构（基线）

### 平台结构
- 登录页面
- 任务管理
  - 已下单加急任务
  - 未下单交期评估任务
  - 客期变更任务
- 数据报表
  - 数据看板
- 基础数据
  - 请假配置
  - 虚拟产品管理
  - 区域统筹管理
  - 请假代理产品型号管理
  - 消息推送管理
  - 用户操作记录（用户端用户每次操作的详细记录，可复现用户每一步操作数据）
- 系统管理
  - 用户管理
  - 角色管理
  - 权限管理（页面/按钮/字段）
  - 系统日志
  - 字典数据
- 用户端（独立页面）
  - 任务提交页面（单独一套 UI 风格）

### 说明
- 本章节仅做功能架构登记，不涉及本次开发实现。
- 后续每个模块进入开发前，需要在“迭代记录”中拆分为可交付子需求并补充接口与数据结构设计。

## 迭代记录

### 2026-03-25 Iteration-001
- 需求: 升级 MySQL 驱动坐标、整合 MyBatis、建库建表、实现 RBAC 角色列表与新增真实落库接口。
- 改动范围:
  - 升级 MySQL 驱动坐标为 `com.mysql:mysql-connector-j:8.0.33`。
  - 新增角色 Mapper 接口与 XML。
  - Service 层接入角色查询/插入真实实现。
  - 新增 SQL 初始化脚本 `sql/001_init_rbac.sql`。
  - 新增固定基础认证账号用于联调。
- 接口:
  - GET `/api/system/roles` 角色列表查询
  - POST `/api/system/roles` 角色新增插入
- 验证:
  - `mvn -DskipTests compile` 编译通过
  - 本地启动后接口可访问（需 Basic Auth）
- 后续计划:
  - 补充角色权限关联表与权限编码聚合查询
  - 接入统一返回体与参数校验

## 版本管理规则
- 每个关键节点至少保留一个 commit。
- 里程碑打 tag，命名格式: `milestone/YYYYMMDD-序号-说明`。
- 远程分支建议:
  - `main`: 稳定主线
  - `feature/*`: 功能开发
  - `backup/*`: 临时备份分支

### 2026-03-25 Iteration-002
- 需求: 完善代码版本管理，打通远程主线同步。
- 改动范围:
  - 远程仓库绑定完成：`origin=https://github.com/Huangzhongbin2026/HZB.git`
  - 创建备份分支：`backup/20260325-rbac-mybatis`
  - 创建里程碑标签：`milestone/20260325-01-rbac-mybatis`
  - 本地 `main` 已合并远程 `origin/main` 历史并解决 README 冲突
- 当前状态:
  - 本地主线已完成收敛，具备下一模块开发条件
  - 远程 `main` 已同步完成
  - 已新增收口备份分支：`backup/20260325-main-sync`
  - 已新增收口里程碑标签：`milestone/20260325-02-main-sync`

### 2026-03-25 Iteration-003
- 需求: 任务管理平台首版数据库设计与初始化建表（先建表，不做功能开发）。
- 覆盖模块:
  - 任务管理（已下单加急任务、未下单交期评估任务、客期变更任务）
  - 数据报表（先建菜单与页面权限）
  - 基础数据（请假配置、虚拟产品管理、区域统筹管理、请假代理产品型号管理、消息推送管理占位）
  - 系统管理（用户管理新增飞书ID、角色管理、权限管理、系统日志、字典数据）
  - 记录管理（先建菜单与页面权限，含用户详细操作记录表）
- 交付物:
  - 数据库设计说明: `docs/database-design-task-platform.md`
  - 初始化脚本: `backend-pseudo/springboot/sql/002_task_platform_init.sql`
- 权限策略:
  - 任务管理：页面 + 按钮 + 字段权限
  - 数据报表/基础数据/记录管理：页面权限
- 说明:
  - 本迭代只做数据库与权限模型设计，不涉及接口与页面开发。

### 2026-03-25 Iteration-004
- 需求: 调整任务字段定义（客期变更任务新增4字段，已下单加急任务“任务提交人”改为“提问人”）。
- 变更内容:
  - `tm_task_customer_due_change` 新增字段：`task_no`（已存在保留）、`salesperson_name`、`latest_risk_level`、`is_closed_loop`
  - `tm_task_expedited_order` 字段重命名：`submitter_name` -> `questioner_name`
- 交付物:
  - 初始化脚本同步更新：`backend-pseudo/springboot/sql/002_task_platform_init.sql`
  - 增量脚本新增：`backend-pseudo/springboot/sql/003_alter_task_fields_20260325.sql`
  - 设计文档同步更新：`docs/database-design-task-platform.md`

## 下一模块开发记录模板

### YYYY-MM-DD Iteration-XXX
- 需求:
- 影响模块:
- 接口变更:
- 数据库变更:
- 代码改动文件:
- 验证结果:
- 回滚方案:
