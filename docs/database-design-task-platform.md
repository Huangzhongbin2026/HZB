# 任务管理平台数据库设计（V1）

## 1. 完整 MySQL 表设计

### 1.1 系统管理与权限表

#### `sys_user`（用户表）
- `id` BIGINT PK
- `username` VARCHAR(64) UNIQUE，可为空（用户端用户可空）
- `password_hash` VARCHAR(255)，管理平台用户使用
- `display_name` VARCHAR(64) NOT NULL
- `feishu_id` VARCHAR(64) NOT NULL（新增，后续用于飞书消息）
- `user_type` ENUM('PLATFORM','CLIENT') NOT NULL
- `mobile` VARCHAR(32)
- `email` VARCHAR(128)
- `status` TINYINT NOT NULL DEFAULT 1
- `remark` VARCHAR(255)
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL

#### `sys_role`（角色表）
- `id` BIGINT PK
- `role_name` VARCHAR(64) NOT NULL
- `role_code` VARCHAR(64) UNIQUE NOT NULL
- `status` TINYINT NOT NULL DEFAULT 1
- `remark` VARCHAR(255)
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL

#### `sys_user_role`（用户角色关联）
- `id` BIGINT PK
- `user_id` BIGINT NOT NULL FK -> `sys_user.id`
- `role_id` BIGINT NOT NULL FK -> `sys_role.id`
- `created_at` DATETIME NOT NULL
- UNIQUE(`user_id`,`role_id`)

#### `sys_menu`（菜单表）
- `id` BIGINT PK
- `parent_id` BIGINT NULL FK -> `sys_menu.id`
- `menu_name` VARCHAR(64) NOT NULL
- `menu_code` VARCHAR(64) UNIQUE NOT NULL
- `menu_type` ENUM('CATALOG','PAGE') NOT NULL
- `path` VARCHAR(255)
- `component` VARCHAR(255)
- `sort_no` INT NOT NULL DEFAULT 0
- `visible` TINYINT NOT NULL DEFAULT 1
- `status` TINYINT NOT NULL DEFAULT 1
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL

#### `sys_permission`（权限资源表）
- `id` BIGINT PK
- `menu_id` BIGINT NULL FK -> `sys_menu.id`
- `permission_name` VARCHAR(128) NOT NULL
- `permission_code` VARCHAR(128) UNIQUE NOT NULL
- `resource_type` ENUM('PAGE','BUTTON','FIELD') NOT NULL
- `module_code` VARCHAR(64) NOT NULL
- `page_code` VARCHAR(64) NOT NULL
- `field_code` VARCHAR(64) NULL（FIELD 类型使用）
- `button_code` VARCHAR(64) NULL（BUTTON 类型使用）
- `status` TINYINT NOT NULL DEFAULT 1
- `created_at` DATETIME NOT NULL

#### `sys_role_permission`（角色权限关联）
- `id` BIGINT PK
- `role_id` BIGINT NOT NULL FK -> `sys_role.id`
- `permission_id` BIGINT NOT NULL FK -> `sys_permission.id`
- `created_at` DATETIME NOT NULL
- UNIQUE(`role_id`,`permission_id`)

#### `sys_role_field_policy`（角色字段策略）
- `id` BIGINT PK
- `role_id` BIGINT NOT NULL FK -> `sys_role.id`
- `module_code` VARCHAR(64) NOT NULL
- `page_code` VARCHAR(64) NOT NULL
- `field_code` VARCHAR(64) NOT NULL
- `policy_mode` ENUM('HIDDEN','READONLY','EDITABLE') NOT NULL DEFAULT 'EDITABLE'
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL
- UNIQUE(`role_id`,`module_code`,`page_code`,`field_code`)

### 1.2 系统日志与字典配置表

#### `sys_login_log`（登录日志）
- `id` BIGINT PK
- `user_id` BIGINT FK -> `sys_user.id`
- `username_snapshot` VARCHAR(64)
- `ip` VARCHAR(64)
- `user_agent` VARCHAR(512)
- `login_result` TINYINT NOT NULL
- `failure_reason` VARCHAR(255)
- `login_at` DATETIME NOT NULL

#### `sys_operation_log`（系统操作日志）
- `id` BIGINT PK
- `user_id` BIGINT FK -> `sys_user.id`
- `module_code` VARCHAR(64) NOT NULL
- `operation_type` VARCHAR(64) NOT NULL
- `target_type` VARCHAR(64)
- `target_id` VARCHAR(64)
- `request_path` VARCHAR(255)
- `request_method` VARCHAR(16)
- `request_body` LONGTEXT
- `response_body` LONGTEXT
- `operation_result` TINYINT NOT NULL
- `error_message` VARCHAR(500)
- `created_at` DATETIME NOT NULL

#### `rec_user_operation_log`（用户端详细操作记录）
- `id` BIGINT PK
- `trace_id` VARCHAR(64) NOT NULL
- `user_id` BIGINT FK -> `sys_user.id`
- `page_code` VARCHAR(64) NOT NULL
- `step_no` INT NOT NULL
- `action_type` VARCHAR(64) NOT NULL
- `action_desc` VARCHAR(255)
- `action_payload` JSON
- `ip` VARCHAR(64)
- `user_agent` VARCHAR(512)
- `created_at` DATETIME NOT NULL
- INDEX(`trace_id`,`step_no`)

#### `sys_dict_type` / `sys_dict_item`（字典类型/字典项）
- 支撑系统管理-字典数据页面。

### 1.3 任务管理业务表

#### `tm_task_expedited_order`（已下单加急任务）
字段（按页面）:
- `id` BIGINT PK
- `task_no` VARCHAR(64) UNIQUE
- `market_code_name` VARCHAR(128)
- `material_desc` VARCHAR(255)
- `undelivered_qty` INT
- `customer_expected_date` DATE
- `odc_due_date` DATE
- `contract_no` VARCHAR(64)
- `crm_no` VARCHAR(64)
- `stock_prepare_date` DATE
- `inventory_detail_url` VARCHAR(500)
- `sales_dept` VARCHAR(128)
- `region_name` VARCHAR(64)
- `customer_type` VARCHAR(64)
- `project_name` VARCHAR(255)
- `blank_due_reason` VARCHAR(255)
- `complete_set_date` DATE
- `order_date` DATETIME
- `project_urgency` VARCHAR(64)
- `latest_arrival_date` DATE
- `accept_partial_delivery` TINYINT
- `most_urgent_items` TEXT
- `late_delivery_impact` TEXT
- `product_coordinator` VARCHAR(64)
- `regional_coordinator` VARCHAR(64)
- `submitter_name` VARCHAR(64)
- `is_repeat_submit` TINYINT
- `business_assistant` VARCHAR(64)
- `order_no` VARCHAR(64)
- `order_line_no` VARCHAR(64)
- `order_type` VARCHAR(64)
- `order_amount` DECIMAL(18,4)
- `settlement_amount_cny` DECIMAL(18,4)（页面名: 结算金额 (人民币)）
- `salesperson_name` VARCHAR(64)
- `latest_risk_level` VARCHAR(32)
- `is_closed_loop` TINYINT
- `agent_regional_coordinator` VARCHAR(64)
- `is_virtual` TINYINT
- `created_at` DATETIME
- `agent_product_coordinator` VARCHAR(64)
- `coordinator_eval_reply` TEXT
- `task_eval_status` VARCHAR(32)
- `completed_at` DATETIME
- `completion_duration_hours` DECIMAL(10,2)
- `updated_at` DATETIME

#### `tm_task_delivery_assessment`（未下单交期评估任务）
- `id` BIGINT PK
- `task_no` VARCHAR(64) UNIQUE
- `product_model` VARCHAR(128)
- `product_name` VARCHAR(255)
- `qty` INT
- `customer_expected_date` DATE
- `is_stock_prepare` TINYINT
- `product_coordinator` VARCHAR(64)
- `inventory_detail_url` VARCHAR(500)
- `questioner_name` VARCHAR(64)
- `crm_no` VARCHAR(64)
- `remark` VARCHAR(500)
- `stock_prepare_date` DATE
- `demand_date` DATE
- `level1_dept` VARCHAR(128)
- `level3_dept` VARCHAR(128)
- `opportunity_owner` VARCHAR(64)
- `project_name` VARCHAR(255)
- `customer_name` VARCHAR(255)
- `is_delist_filing` TINYINT
- `task_type` VARCHAR(64)
- `is_repeat_submit` TINYINT
- `product_flow_rate` INT
- `completion_duration_hours` DECIMAL(10,2)
- `agent_product_coordinator` VARCHAR(64)
- `task_eval_status` VARCHAR(32)
- `coordinator_eval_reply` TEXT
- `completed_at` DATETIME
- `created_at` DATETIME
- `updated_at` DATETIME

#### `tm_task_customer_due_change`（客期变更任务）
- `id` BIGINT PK
- `task_no` VARCHAR(64) UNIQUE
- `market_code_name` VARCHAR(128)
- `material_desc` VARCHAR(255)
- `undelivered_qty` INT
- `customer_expected_date` DATE
- `odc_due_date` DATE
- `order_amount` DECIMAL(18,2)
- `settlement_amount_cny` DECIMAL(18,2)（页面名: 结算金额 (人民币)）
- `change_qty` INT
- `advance_to_date` DATE
- `order_date` DATE
- `product_coordinator` VARCHAR(64)
- `order_no` VARCHAR(64)
- `order_line_no` VARCHAR(64)
- `customer_type` VARCHAR(64)
- `complete_set_date` DATE
- `agree_partial_if_not_complete` TINYINT
- `business_assistant` VARCHAR(64)
- `business_assistant_feishu_id` VARCHAR(64)
- `questioner_name` VARCHAR(64)
- `task_type` VARCHAR(64)
- `inventory_detail_url` VARCHAR(500)
- `advance_reason` VARCHAR(500)
- `delay_reason` VARCHAR(500)
- `delay_proof_url` VARCHAR(500)
- `delay_to_date` DATE
- `sales_dept` VARCHAR(128)
- `project_name` VARCHAR(255)
- `agent_regional_coordinator` VARCHAR(64)
- `crm_no` VARCHAR(64)
- `approval_no` VARCHAR(64)
- `region_name` VARCHAR(64)
- `contract_no` VARCHAR(64)
- `is_repeat_submit` TINYINT
- `created_at` DATETIME
- `agent_product_coordinator` VARCHAR(64)
- `coordinator_eval_reply` TEXT
- `task_eval_status` VARCHAR(32)
- `completed_at` DATETIME
- `updated_at` DATETIME

#### `tm_task_action_log`（任务按钮操作日志）
- `id` BIGINT PK
- `task_type` VARCHAR(32) NOT NULL（EXPEDITED/ASSESSMENT/DUE_CHANGE）
- `task_id` BIGINT NOT NULL
- `button_code` VARCHAR(64) NOT NULL
- `operator_user_id` BIGINT FK -> `sys_user.id`
- `operator_role_type` VARCHAR(32) NOT NULL（USER/REGION_COORDINATOR）
- `action_result` TINYINT NOT NULL
- `action_message` VARCHAR(500)
- `before_data` JSON
- `after_data` JSON
- `created_at` DATETIME NOT NULL

### 1.4 基础数据表

#### `bd_leave_config`（请假配置）
- `id` BIGINT PK
- `user_id` BIGINT NOT NULL FK -> `sys_user.id`
- `leave_type` ENUM('DAY','HOUR') NOT NULL DEFAULT 'DAY'
- `leave_start_time` DATETIME NOT NULL
- `leave_end_time` DATETIME NOT NULL
- `remark` VARCHAR(255)
- `created_at` DATETIME
- `updated_at` DATETIME

#### `bd_virtual_product`（虚拟产品管理）
- `id` BIGINT PK
- `product_model` VARCHAR(128) NOT NULL
- `auto_reply_content` TEXT NOT NULL
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL

#### `bd_region_coordinator`（区域统筹管理）
- `id` BIGINT PK
- `product_model` VARCHAR(128) NOT NULL
- `origin_coordinator_user_id` BIGINT NOT NULL FK -> `sys_user.id`
- `agent_coordinator_user_id` BIGINT NOT NULL FK -> `sys_user.id`
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL

#### `bd_leave_proxy_product`（请假代理产品型号管理）
- `id` BIGINT PK
- `product_model` VARCHAR(128) NOT NULL
- `origin_coordinator_user_id` BIGINT NOT NULL FK -> `sys_user.id`
- `agent_coordinator_user_id` BIGINT NOT NULL FK -> `sys_user.id`
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL

#### `bd_message_push_config`（消息推送管理，先占位）
- `id` BIGINT PK
- `config_key` VARCHAR(64) UNIQUE NOT NULL
- `config_value` TEXT
- `enabled` TINYINT NOT NULL DEFAULT 1
- `remark` VARCHAR(255)
- `created_at` DATETIME NOT NULL
- `updated_at` DATETIME NOT NULL

## 2. 表关系说明

1. 用户与角色
- `sys_user` 与 `sys_role` 为多对多，通过 `sys_user_role` 关联。

2. 角色与权限
- `sys_role` 与 `sys_permission` 为多对多，通过 `sys_role_permission` 关联。
- 任务管理需要页面+按钮+字段权限：
  - 页面权限：`resource_type='PAGE'`
  - 按钮权限：`resource_type='BUTTON'`
  - 字段权限：`resource_type='FIELD'`
- 字段显示/可编辑策略由 `sys_role_field_policy` 控制。

3. 菜单与权限资源
- `sys_menu` 描述菜单树。
- `sys_permission.menu_id` 指向菜单节点，权限码与菜单解耦。

4. 系统日志
- `sys_login_log`、`sys_operation_log`、`rec_user_operation_log` 关联 `sys_user`，用于审计与复盘。

5. 任务业务
- 三类任务主表独立：
  - `tm_task_expedited_order`
  - `tm_task_delivery_assessment`
  - `tm_task_customer_due_change`
- 按钮操作历史统一写入 `tm_task_action_log`，通过 `task_type + task_id` 关联到具体任务。

6. 基础数据
- 请假配置、区域统筹、请假代理产品型号均通过用户 ID 关联到 `sys_user`。

7. 模块权限要求落地
- 任务管理：页面/字段/按钮全量权限控制（由 `sys_permission + sys_role_field_policy` 完成）。
- 数据报表、基础数据、记录管理：仅页面权限控制（`resource_type='PAGE'` 即可）。

## 3. 数据库初始化 SQL 脚本

- 脚本文件：`backend-pseudo/springboot/sql/002_task_platform_init.sql`
- 说明：
  - 包含全部核心业务表、权限表、日志表、配置表。
  - 包含菜单节点初始化（含“数据报表”“记录管理”“消息推送管理”占位节点）。
  - 包含任务管理页面/按钮/字段权限初始化样例。
