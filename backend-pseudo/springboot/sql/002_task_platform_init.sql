-- 任务管理平台数据库初始化脚本（V1）
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS jithub_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jithub_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 1) 系统管理与权限
-- =========================

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(64) NULL COMMENT '登录名（用户端用户可空）',
  password_hash VARCHAR(255) NULL COMMENT '密码哈希（管理平台用户使用）',
  display_name VARCHAR(64) NOT NULL COMMENT '姓名',
  feishu_id VARCHAR(64) NOT NULL COMMENT '飞书ID',
  user_type ENUM('PLATFORM','CLIENT') NOT NULL COMMENT '用户类型：管理平台/用户端',
  mobile VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_type_status (user_type, status),
  KEY idx_sys_user_feishu (feishu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_name VARCHAR(64) NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_role_id (role_id),
  CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  parent_id BIGINT NULL,
  menu_name VARCHAR(64) NOT NULL,
  menu_code VARCHAR(64) NOT NULL,
  menu_type ENUM('CATALOG','PAGE') NOT NULL,
  path VARCHAR(255) NULL,
  component VARCHAR(255) NULL,
  sort_no INT NOT NULL DEFAULT 0,
  visible TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_menu_code (menu_code),
  KEY idx_sys_menu_parent (parent_id),
  CONSTRAINT fk_sys_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  menu_id BIGINT NULL,
  permission_name VARCHAR(128) NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  resource_type ENUM('PAGE','BUTTON','FIELD') NOT NULL,
  module_code VARCHAR(64) NOT NULL,
  page_code VARCHAR(64) NOT NULL,
  field_code VARCHAR(64) NULL,
  button_code VARCHAR(64) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_permission_code (permission_code),
  KEY idx_sys_permission_menu (menu_id),
  KEY idx_sys_permission_page_type (page_code, resource_type),
  CONSTRAINT fk_sys_permission_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
  KEY idx_sys_role_permission_pid (permission_id),
  CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

CREATE TABLE IF NOT EXISTS sys_role_field_policy (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  module_code VARCHAR(64) NOT NULL,
  page_code VARCHAR(64) NOT NULL,
  field_code VARCHAR(64) NOT NULL,
  policy_mode ENUM('HIDDEN','READONLY','EDITABLE') NOT NULL DEFAULT 'EDITABLE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_field_policy (role_id, module_code, page_code, field_code),
  CONSTRAINT fk_sys_role_field_policy_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色字段权限策略';

-- =========================
-- 2) 日志与字典
-- =========================

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  username_snapshot VARCHAR(64) NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  login_result TINYINT NOT NULL,
  failure_reason VARCHAR(255) NULL,
  login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sys_login_log_user_time (user_id, login_at),
  CONSTRAINT fk_sys_login_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  module_code VARCHAR(64) NOT NULL,
  operation_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NULL,
  target_id VARCHAR(64) NULL,
  request_path VARCHAR(255) NULL,
  request_method VARCHAR(16) NULL,
  request_body LONGTEXT NULL,
  response_body LONGTEXT NULL,
  operation_result TINYINT NOT NULL,
  error_message VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sys_operation_log_user_time (user_id, created_at),
  KEY idx_sys_operation_log_module_time (module_code, created_at),
  CONSTRAINT fk_sys_operation_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志';

CREATE TABLE IF NOT EXISTS rec_user_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  trace_id VARCHAR(64) NOT NULL,
  user_id BIGINT NULL,
  page_code VARCHAR(64) NOT NULL,
  step_no INT NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  action_desc VARCHAR(255) NULL,
  action_payload JSON NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rec_user_op_trace_step (trace_id, step_no),
  KEY idx_rec_user_op_user_time (user_id, created_at),
  CONSTRAINT fk_rec_user_op_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户端详细操作记录';

CREATE TABLE IF NOT EXISTS sys_dict_type (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_name VARCHAR(64) NOT NULL,
  dict_code VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_dict_type_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型';

CREATE TABLE IF NOT EXISTS sys_dict_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_type_id BIGINT NOT NULL,
  item_label VARCHAR(64) NOT NULL,
  item_value VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_dict_item (dict_type_id, item_value),
  CONSTRAINT fk_sys_dict_item_type FOREIGN KEY (dict_type_id) REFERENCES sys_dict_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项';

-- =========================
-- 3) 任务管理
-- =========================

CREATE TABLE IF NOT EXISTS tm_task_expedited_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
  market_code_name VARCHAR(128) NULL,
  material_desc VARCHAR(255) NULL,
  undelivered_qty INT NULL,
  customer_expected_date DATE NULL,
  odc_due_date DATE NULL,
  contract_no VARCHAR(64) NULL,
  crm_no VARCHAR(64) NULL,
  stock_prepare_date DATE NULL,
  inventory_detail_url VARCHAR(500) NULL,
  sales_dept VARCHAR(128) NULL,
  region_name VARCHAR(64) NULL,
  customer_type VARCHAR(64) NULL,
  project_name VARCHAR(255) NULL,
  blank_due_reason VARCHAR(255) NULL,
  complete_set_date DATE NULL,
  order_date DATETIME NULL,
  project_urgency VARCHAR(64) NULL,
  latest_arrival_date DATE NULL,
  accept_partial_delivery TINYINT NULL,
  most_urgent_items TEXT NULL,
  late_delivery_impact TEXT NULL,
  product_coordinator VARCHAR(64) NULL,
  regional_coordinator VARCHAR(64) NULL,
  submitter_name VARCHAR(64) NULL,
  is_repeat_submit TINYINT NOT NULL DEFAULT 0,
  business_assistant VARCHAR(64) NULL,
  order_no VARCHAR(64) NULL,
  order_line_no VARCHAR(64) NULL,
  order_type VARCHAR(64) NULL,
  order_amount DECIMAL(18,4) NULL,
  settlement_amount_cny DECIMAL(18,4) NULL COMMENT '结算金额 (人民币)',
  salesperson_name VARCHAR(64) NULL,
  latest_risk_level VARCHAR(32) NULL,
  is_closed_loop TINYINT NOT NULL DEFAULT 0,
  agent_regional_coordinator VARCHAR(64) NULL,
  is_virtual TINYINT NOT NULL DEFAULT 0,
  agent_product_coordinator VARCHAR(64) NULL,
  coordinator_eval_reply TEXT NULL,
  task_eval_status VARCHAR(32) NULL,
  completed_at DATETIME NULL,
  completion_duration_hours DECIMAL(10,2) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tm_task_expedited_order_task_no (task_no),
  KEY idx_tm_task_expedited_order_status (task_eval_status),
  KEY idx_tm_task_expedited_order_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已下单加急任务';

CREATE TABLE IF NOT EXISTS tm_task_delivery_assessment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
  product_model VARCHAR(128) NULL,
  product_name VARCHAR(255) NULL,
  qty INT NULL,
  customer_expected_date DATE NULL,
  is_stock_prepare TINYINT NULL,
  product_coordinator VARCHAR(64) NULL,
  inventory_detail_url VARCHAR(500) NULL,
  questioner_name VARCHAR(64) NULL,
  crm_no VARCHAR(64) NULL,
  remark VARCHAR(500) NULL,
  stock_prepare_date DATE NULL,
  demand_date DATE NULL,
  level1_dept VARCHAR(128) NULL,
  level3_dept VARCHAR(128) NULL,
  opportunity_owner VARCHAR(64) NULL,
  project_name VARCHAR(255) NULL,
  customer_name VARCHAR(255) NULL,
  is_delist_filing TINYINT NULL,
  task_type VARCHAR(64) NULL,
  is_repeat_submit TINYINT NOT NULL DEFAULT 0,
  product_flow_rate INT NULL,
  completion_duration_hours DECIMAL(10,2) NULL,
  agent_product_coordinator VARCHAR(64) NULL,
  task_eval_status VARCHAR(32) NULL,
  coordinator_eval_reply TEXT NULL,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tm_task_delivery_assessment_task_no (task_no),
  KEY idx_tm_task_delivery_assessment_status (task_eval_status),
  KEY idx_tm_task_delivery_assessment_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='未下单交期评估任务';

CREATE TABLE IF NOT EXISTS tm_task_customer_due_change (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
  market_code_name VARCHAR(128) NULL,
  material_desc VARCHAR(255) NULL,
  undelivered_qty INT NULL,
  customer_expected_date DATE NULL,
  odc_due_date DATE NULL,
  order_amount DECIMAL(18,2) NULL,
  settlement_amount_cny DECIMAL(18,2) NULL COMMENT '结算金额 (人民币)',
  change_qty INT NULL,
  advance_to_date DATE NULL,
  order_date DATE NULL,
  product_coordinator VARCHAR(64) NULL,
  order_no VARCHAR(64) NULL,
  order_line_no VARCHAR(64) NULL,
  customer_type VARCHAR(64) NULL,
  complete_set_date DATE NULL,
  agree_partial_if_not_complete TINYINT NULL,
  business_assistant VARCHAR(64) NULL,
  business_assistant_feishu_id VARCHAR(64) NULL,
  questioner_name VARCHAR(64) NULL,
  task_type VARCHAR(64) NULL,
  inventory_detail_url VARCHAR(500) NULL,
  advance_reason VARCHAR(500) NULL,
  delay_reason VARCHAR(500) NULL,
  delay_proof_url VARCHAR(500) NULL,
  delay_to_date DATE NULL,
  sales_dept VARCHAR(128) NULL,
  project_name VARCHAR(255) NULL,
  agent_regional_coordinator VARCHAR(64) NULL,
  crm_no VARCHAR(64) NULL,
  approval_no VARCHAR(64) NULL,
  region_name VARCHAR(64) NULL,
  contract_no VARCHAR(64) NULL,
  is_repeat_submit TINYINT NOT NULL DEFAULT 0,
  agent_product_coordinator VARCHAR(64) NULL,
  coordinator_eval_reply TEXT NULL,
  task_eval_status VARCHAR(32) NULL,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tm_task_customer_due_change_task_no (task_no),
  KEY idx_tm_task_customer_due_change_status (task_eval_status),
  KEY idx_tm_task_customer_due_change_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客期变更任务';

CREATE TABLE IF NOT EXISTS tm_task_action_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_type VARCHAR(32) NOT NULL COMMENT 'EXPEDITED/ASSESSMENT/DUE_CHANGE',
  task_id BIGINT NOT NULL,
  button_code VARCHAR(64) NOT NULL,
  operator_user_id BIGINT NULL,
  operator_role_type VARCHAR(32) NOT NULL COMMENT 'USER/REGION_COORDINATOR',
  action_result TINYINT NOT NULL DEFAULT 1,
  action_message VARCHAR(500) NULL,
  before_data JSON NULL,
  after_data JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tm_task_action_log_task (task_type, task_id),
  KEY idx_tm_task_action_log_operator (operator_user_id),
  CONSTRAINT fk_tm_task_action_log_user FOREIGN KEY (operator_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务按钮操作日志';

-- =========================
-- 4) 基础数据
-- =========================

CREATE TABLE IF NOT EXISTS bd_leave_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  leave_type ENUM('DAY','HOUR') NOT NULL DEFAULT 'DAY',
  leave_start_time DATETIME NOT NULL,
  leave_end_time DATETIME NOT NULL,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_bd_leave_config_user (user_id),
  CONSTRAINT fk_bd_leave_config_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假配置';

CREATE TABLE IF NOT EXISTS bd_virtual_product (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_model VARCHAR(128) NOT NULL,
  auto_reply_content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_bd_virtual_product_model (product_model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟产品管理';

CREATE TABLE IF NOT EXISTS bd_region_coordinator (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_model VARCHAR(128) NOT NULL,
  origin_coordinator_user_id BIGINT NOT NULL,
  agent_coordinator_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_bd_region_coordinator_model (product_model),
  CONSTRAINT fk_bd_region_coordinator_origin_user FOREIGN KEY (origin_coordinator_user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_bd_region_coordinator_agent_user FOREIGN KEY (agent_coordinator_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域统筹管理';

CREATE TABLE IF NOT EXISTS bd_leave_proxy_product (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_model VARCHAR(128) NOT NULL,
  origin_coordinator_user_id BIGINT NOT NULL,
  agent_coordinator_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_bd_leave_proxy_product_model (product_model),
  CONSTRAINT fk_bd_leave_proxy_product_origin_user FOREIGN KEY (origin_coordinator_user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_bd_leave_proxy_product_agent_user FOREIGN KEY (agent_coordinator_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假代理产品型号管理';

CREATE TABLE IF NOT EXISTS bd_message_push_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  config_key VARCHAR(64) NOT NULL,
  config_value TEXT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bd_message_push_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息推送配置（菜单先占位）';

-- =========================
-- 5) 菜单与权限初始化
-- =========================

INSERT INTO sys_role(role_name, role_code, status, remark)
SELECT '超级管理员', 'admin', 1, '系统内置管理员角色'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code='admin');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT NULL, '任务管理', 'task_mgmt', 'CATALOG', '/task', 'layout', 10
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='task_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='task_mgmt'), '已下单加急任务', 'task_expedited_order', 'PAGE', '/task/expedited-order', 'views/task/ExpeditedOrder', 11
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='task_expedited_order');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='task_mgmt'), '未下单交期评估任务', 'task_delivery_assessment', 'PAGE', '/task/delivery-assessment', 'views/task/DeliveryAssessment', 12
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='task_delivery_assessment');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='task_mgmt'), '客期变更任务', 'task_customer_due_change', 'PAGE', '/task/customer-due-change', 'views/task/CustomerDueChange', 13
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='task_customer_due_change');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT NULL, '数据报表', 'report_mgmt', 'CATALOG', '/report', 'layout', 20
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='report_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='report_mgmt'), '数据看板', 'report_dashboard', 'PAGE', '/report/dashboard', 'views/report/Dashboard', 21
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='report_dashboard');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT NULL, '基础数据', 'base_data', 'CATALOG', '/base', 'layout', 30
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='base_data');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='base_data'), '请假配置', 'base_leave_config', 'PAGE', '/base/leave-config', 'views/base/LeaveConfig', 31
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='base_leave_config');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='base_data'), '虚拟产品管理', 'base_virtual_product', 'PAGE', '/base/virtual-product', 'views/base/VirtualProduct', 32
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='base_virtual_product');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='base_data'), '区域统筹管理', 'base_region_coordinator', 'PAGE', '/base/region-coordinator', 'views/base/RegionCoordinator', 33
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='base_region_coordinator');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='base_data'), '请假代理产品型号管理', 'base_leave_proxy_product', 'PAGE', '/base/leave-proxy-product', 'views/base/LeaveProxyProduct', 34
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='base_leave_proxy_product');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='base_data'), '消息推送管理', 'base_message_push', 'PAGE', '/base/message-push', 'views/base/MessagePush', 35
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='base_message_push');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT NULL, '记录管理', 'record_mgmt', 'CATALOG', '/record', 'layout', 40
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='record_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='record_mgmt'), '用户操作记录', 'record_user_operation', 'PAGE', '/record/user-operation', 'views/record/UserOperation', 41
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='record_user_operation');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT NULL, '系统管理', 'sys_mgmt', 'CATALOG', '/system', 'layout', 50
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='sys_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='sys_mgmt'), '用户管理', 'sys_user_mgmt', 'PAGE', '/system/users', 'views/system/UserManage', 51
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='sys_user_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='sys_mgmt'), '角色管理', 'sys_role_mgmt', 'PAGE', '/system/roles', 'views/system/RoleManage', 52
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='sys_role_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='sys_mgmt'), '权限管理', 'sys_permission_mgmt', 'PAGE', '/system/permissions', 'views/system/PermissionManage', 53
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='sys_permission_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='sys_mgmt'), '系统日志', 'sys_log_mgmt', 'PAGE', '/system/logs', 'views/system/OperationLog', 54
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='sys_log_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='sys_mgmt'), '字典数据', 'sys_dict_mgmt', 'PAGE', '/system/dict', 'views/system/DictType', 55
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='sys_dict_mgmt');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT NULL, '用户端', 'client_portal', 'CATALOG', '/client', 'layout', 60
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='client_portal');

INSERT INTO sys_menu(parent_id, menu_name, menu_code, menu_type, path, component, sort_no)
SELECT (SELECT id FROM sys_menu WHERE menu_code='client_portal'), '任务提交页面', 'client_task_submit', 'PAGE', '/client/task-submit', 'views/client/TaskSubmit', 61
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code='client_task_submit');

-- 页面权限（任务管理 + 数据报表 + 基础数据 + 记录管理 + 系统管理）
INSERT INTO sys_permission(menu_id, permission_name, permission_code, resource_type, module_code, page_code)
SELECT m.id, CONCAT(m.menu_name, '页面访问'), CONCAT('PAGE:', m.menu_code), 'PAGE',
  CASE
    WHEN m.menu_code LIKE 'task_%' THEN 'TASK'
    WHEN m.menu_code LIKE 'report_%' THEN 'REPORT'
    WHEN m.menu_code LIKE 'base_%' THEN 'BASE'
    WHEN m.menu_code LIKE 'record_%' THEN 'RECORD'
    WHEN m.menu_code LIKE 'sys_%' THEN 'SYSTEM'
    WHEN m.menu_code LIKE 'client_%' THEN 'CLIENT'
    ELSE 'COMMON'
  END,
  m.menu_code
FROM sys_menu m
WHERE m.menu_type='PAGE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = CONCAT('PAGE:', m.menu_code)
  );

-- 任务管理按钮权限（用户操作、区域统筹操作）
INSERT INTO sys_permission(menu_id, permission_name, permission_code, resource_type, module_code, page_code, button_code)
SELECT m.id, '用户操作按钮', CONCAT('BTN:', m.menu_code, ':USER_ACTION'), 'BUTTON', 'TASK', m.menu_code, 'USER_ACTION'
FROM sys_menu m
WHERE m.menu_code IN ('task_expedited_order','task_delivery_assessment','task_customer_due_change')
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = CONCAT('BTN:', m.menu_code, ':USER_ACTION')
  );

INSERT INTO sys_permission(menu_id, permission_name, permission_code, resource_type, module_code, page_code, button_code)
SELECT m.id, '区域统筹操作按钮', CONCAT('BTN:', m.menu_code, ':REGION_ACTION'), 'BUTTON', 'TASK', m.menu_code, 'REGION_ACTION'
FROM sys_menu m
WHERE m.menu_code IN ('task_expedited_order','task_customer_due_change')
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = CONCAT('BTN:', m.menu_code, ':REGION_ACTION')
  );

-- 任务管理字段权限样例（可按同规范继续扩展）
INSERT INTO sys_permission(menu_id, permission_name, permission_code, resource_type, module_code, page_code, field_code)
SELECT m.id, '任务编号字段', CONCAT('FIELD:', m.menu_code, ':task_no'), 'FIELD', 'TASK', m.menu_code, 'task_no'
FROM sys_menu m
WHERE m.menu_code IN ('task_expedited_order','task_delivery_assessment','task_customer_due_change')
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = CONCAT('FIELD:', m.menu_code, ':task_no')
  );

INSERT INTO sys_permission(menu_id, permission_name, permission_code, resource_type, module_code, page_code, field_code)
SELECT m.id, '任务评估状态字段', CONCAT('FIELD:', m.menu_code, ':task_eval_status'), 'FIELD', 'TASK', m.menu_code, 'task_eval_status'
FROM sys_menu m
WHERE m.menu_code IN ('task_expedited_order','task_delivery_assessment','task_customer_due_change')
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = CONCAT('FIELD:', m.menu_code, ':task_eval_status')
  );

-- 超级管理员默认赋予全部权限
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON 1=1
WHERE r.role_code='admin'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id
  );

SET FOREIGN_KEY_CHECKS = 1;
