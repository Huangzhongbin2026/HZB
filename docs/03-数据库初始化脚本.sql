-- 供应链统筹任务管理平台 - 初始化脚本
-- MySQL 8.x

CREATE DATABASE IF NOT EXISTS db_supply_task DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE db_supply_task;

-- 用户与权限
CREATE TABLE IF NOT EXISTS t_user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户工号',
  user_name VARCHAR(64) NOT NULL COMMENT '姓名',
  dept_code VARCHAR(64) DEFAULT NULL COMMENT '部门编码',
  mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_dept_status (dept_code, status)
) COMMENT='用户账号表';

CREATE TABLE IF NOT EXISTS t_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(128) NOT NULL COMMENT '角色名称',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_role_code (role_code)
) COMMENT='角色表';

CREATE TABLE IF NOT EXISTS t_user_account_role_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户工号',
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_user_role (user_id, role_code),
  KEY idx_role_code (role_code)
) COMMENT='用户角色关系表';

-- 产品主数据
CREATE TABLE IF NOT EXISTS t_product_virtual (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  product_model VARCHAR(128) NOT NULL COMMENT '产品型号',
  product_name VARCHAR(128) DEFAULT NULL COMMENT '产品名称',
  product_line VARCHAR(64) DEFAULT NULL COMMENT '产品线',
  category VARCHAR(64) DEFAULT NULL COMMENT '分类',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  effective_from DATETIME DEFAULT NULL COMMENT '生效时间',
  effective_to DATETIME DEFAULT NULL COMMENT '失效时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_product_model (product_model),
  KEY idx_product_line_enabled (product_line, enabled)
) COMMENT='虚拟产品表';

-- 任务主表
CREATE TABLE IF NOT EXISTS t_task_main (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(32) NOT NULL COMMENT '任务编号',
  task_type VARCHAR(32) NOT NULL COMMENT '任务类型:ORDER_URGENT/UNORDERED_ASSESS/DELIVERY_CHANGE',
  task_status VARCHAR(32) NOT NULL COMMENT '任务状态:PENDING/PROCESSING/WAIT_CONFIRM/DONE/CLOSED',
  title VARCHAR(256) NOT NULL COMMENT '任务标题',
  order_no VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  product_model VARCHAR(128) NOT NULL COMMENT '产品型号',
  requester VARCHAR(64) NOT NULL COMMENT '提单人工号',
  owner_user_id VARCHAR(64) NOT NULL COMMENT '当前处理人工号',
  required_date DATETIME NOT NULL COMMENT '需求日期',
  priority TINYINT NOT NULL DEFAULT 3 COMMENT '优先级1-5',
  source_system VARCHAR(64) DEFAULT 'manual' COMMENT '来源系统',
  closed_reason VARCHAR(512) DEFAULT NULL COMMENT '关闭原因',
  closed_at DATETIME DEFAULT NULL COMMENT '关闭时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_task_id (task_id),
  KEY idx_task_status_owner (task_status, owner_user_id),
  KEY idx_task_type_required (task_type, required_date),
  KEY idx_product_model_created (product_model, created_at)
) COMMENT='任务主表';

-- 任务流转日志
CREATE TABLE IF NOT EXISTS t_task_flow_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(32) NOT NULL COMMENT '任务编号',
  action_type VARCHAR(32) NOT NULL COMMENT '动作类型:CREATE/TRANSFER/PROCESS/CLOSE',
  from_user_id VARCHAR(64) DEFAULT NULL COMMENT '原处理人',
  to_user_id VARCHAR(64) DEFAULT NULL COMMENT '目标处理人',
  action_comment VARCHAR(512) DEFAULT NULL COMMENT '动作备注',
  action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '动作时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  KEY idx_task_action_time (task_id, action_time),
  KEY idx_action_type (action_type)
) COMMENT='任务流转日志表';

-- 请假配置
CREATE TABLE IF NOT EXISTS t_cfg_leave (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id VARCHAR(64) NOT NULL COMMENT '请假人工号',
  start_at DATETIME NOT NULL COMMENT '开始时间',
  end_at DATETIME NOT NULL COMMENT '结束时间',
  reason VARCHAR(256) DEFAULT NULL COMMENT '请假原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  KEY idx_user_time (user_id, start_at, end_at)
) COMMENT='请假配置表';

-- 请假代理产品型号
CREATE TABLE IF NOT EXISTS t_cfg_leave_agent_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  leave_id BIGINT NOT NULL COMMENT '请假配置ID',
  product_model VARCHAR(128) NOT NULL COMMENT '产品型号',
  agent_user_id VARCHAR(64) NOT NULL COMMENT '代理人工号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_leave_product (leave_id, product_model),
  KEY idx_agent_user (agent_user_id),
  CONSTRAINT fk_leave_agent_leave_id FOREIGN KEY (leave_id) REFERENCES t_cfg_leave(id)
) COMMENT='请假代理产品型号表';

-- 消息推送开关
CREATE TABLE IF NOT EXISTS t_cfg_message_switch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  event_code VARCHAR(64) NOT NULL COMMENT '事件编码:TASK_CREATED/TASK_TRANSFER/TASK_OVERDUE',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '开关状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_event_code (event_code)
) COMMENT='消息推送开关配置';

-- 消息投递日志
CREATE TABLE IF NOT EXISTS t_msg_push_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(32) NOT NULL COMMENT '任务编号',
  event_code VARCHAR(64) NOT NULL COMMENT '事件编码',
  channel_code VARCHAR(32) NOT NULL COMMENT '渠道编码:IM/MAIL/SMS/IN_APP',
  receiver_user_id VARCHAR(64) NOT NULL COMMENT '接收人工号',
  push_status VARCHAR(16) NOT NULL COMMENT '投递状态:SUCCESS/FAIL/RETRY',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  push_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
  message_body TEXT COMMENT '消息内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  KEY idx_task_event (task_id, event_code),
  KEY idx_receiver_time (receiver_user_id, push_time)
) COMMENT='消息投递日志表';

-- 报表日统计
CREATE TABLE IF NOT EXISTS t_rpt_task_day (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  stat_date DATE NOT NULL COMMENT '统计日期',
  task_type VARCHAR(32) NOT NULL COMMENT '任务类型',
  total_count INT NOT NULL DEFAULT 0 COMMENT '总任务量',
  done_count INT NOT NULL DEFAULT 0 COMMENT '完成量',
  overdue_count INT NOT NULL DEFAULT 0 COMMENT '超时量',
  on_time_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '准时率',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_stat_date_type (stat_date, task_type)
) COMMENT='任务日报统计表';

-- 初始化字典数据
INSERT INTO t_cfg_message_switch(event_code, enabled, created_by, updated_by)
VALUES ('TASK_CREATED', 1, 'system', 'system'),
       ('TASK_TRANSFER', 1, 'system', 'system'),
       ('TASK_OVERDUE', 1, 'system', 'system')
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), updated_at = CURRENT_TIMESTAMP;

-- 分表建议:
-- 1) t_task_main 按月分表: t_task_main_yyyyMM
-- 2) t_task_flow_log 按月分表: t_task_flow_log_yyyyMM
-- 高频索引建议:
-- idx_task_status_owner(task_status, owner_user_id, updated_at)
-- idx_task_type_required(task_type, required_date, priority)
