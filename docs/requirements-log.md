# 需求与迭代记录

## 项目约束
- 后续所有功能必须采用 MyBatis + MySQL 真实落库。
- 新增接口默认遵循 Controller -> Service -> Mapper -> XML -> MySQL 分层链路。
- 每个需求完成后必须补充本文件记录（需求、改动点、验证结果、风险）。

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

## 下一模块开发记录模板

### YYYY-MM-DD Iteration-XXX
- 需求:
- 影响模块:
- 接口变更:
- 数据库变更:
- 代码改动文件:
- 验证结果:
- 回滚方案:
