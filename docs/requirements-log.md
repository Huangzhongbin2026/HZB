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
