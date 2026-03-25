# Spring Boot 伪后端骨架

该目录给出系统管理模块的后端骨架（伪代码风格），包含：

- RBAC 认证鉴权配置
- 角色/用户/权限/日志/字典控制器
- 字段权限策略服务接口

建议按以下步骤落地：

1. 用 MyBatis/JPA 实现 model 与持久层
2. 接入 JWT + Redis 会话
3. 将伪方法替换成真实 service 实现
4. 与前端 permissionCodes / fieldPolicies 协议对齐

## 全局开发备注

- 后续所有功能开发必须实现 MyBatis + MySQL 的真实数据持久化（真正落库），禁止仅使用内存伪实现作为最终交付。

## 本地初始化数据库

1. 执行 SQL 脚本：`sql/001_init_rbac.sql`
2. 确认表：`sys_role`
3. 启动服务：`mvn spring-boot:run`

## 当前已打通链路

- `GET /api/system/roles` -> 角色列表查询（MyBatis + MySQL）
- `POST /api/system/roles` -> 角色新增插入（MyBatis + MySQL）

## 新功能复用模板

1. `controller` 新增接口
2. `service` 声明方法并在 `impl` 调用 mapper
3. `mapper` 新增 Java 接口
4. `resources/mapper/**/*.xml` 新增 SQL
5. MySQL 新增表结构或索引脚本写入 `sql/`
