USE db_supply_task;

CREATE TABLE IF NOT EXISTS sys_aux_leave_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '请假人员ID',
  user_name VARCHAR(100) NOT NULL COMMENT '请假人员名称',
  leave_start DATETIME NOT NULL COMMENT '请假开始时间',
  leave_end DATETIME NOT NULL COMMENT '请假结束时间',
  leave_reason VARCHAR(500) DEFAULT NULL COMMENT '请假原因',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  KEY idx_leave_user_time (user_id, leave_start, leave_end),
  CONSTRAINT fk_aux_leave_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='请假配置';

CREATE TABLE IF NOT EXISTS sys_aux_virtual_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  product_model VARCHAR(128) NOT NULL COMMENT '产品型号',
  auto_reply_content VARCHAR(1000) DEFAULT NULL COMMENT '自动回复内容',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_virtual_model (product_model),
  KEY idx_virtual_created (created_at)
) COMMENT='虚拟产品管理';

CREATE TABLE IF NOT EXISTS sys_aux_message_push (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  push_name VARCHAR(120) NOT NULL COMMENT '消息推送名称',
  route_code VARCHAR(120) NOT NULL COMMENT '路由代码',
  feishu_template_code VARCHAR(120) DEFAULT NULL COMMENT '飞书模板编码',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启',
  push_rule VARCHAR(500) DEFAULT NULL COMMENT '推送规则',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_push_route_code (route_code),
  KEY idx_push_name_enabled (push_name, is_enabled)
) COMMENT='消息推送管理';

CREATE TABLE IF NOT EXISTS sys_aux_leave_agent_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  product_model VARCHAR(128) NOT NULL COMMENT '产品型号',
  original_user_id BIGINT NOT NULL COMMENT '原产品统筹ID',
  original_user_name VARCHAR(100) NOT NULL COMMENT '原产品统筹名称',
  agent_user_id BIGINT NOT NULL COMMENT '代理产品统筹ID',
  agent_user_name VARCHAR(100) NOT NULL COMMENT '代理产品统筹名称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_leave_agent_unique (product_model, original_user_id),
  KEY idx_leave_agent_original (original_user_id),
  KEY idx_leave_agent_agent (agent_user_id),
  CONSTRAINT fk_leave_agent_original_user FOREIGN KEY (original_user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_leave_agent_proxy_user FOREIGN KEY (agent_user_id) REFERENCES sys_user(id)
) COMMENT='请假代理产品型号管理';

CREATE TABLE IF NOT EXISTS sys_aux_area_coordinator (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  sale_dept_code VARCHAR(64) NOT NULL COMMENT '销售部门编码',
  province_code VARCHAR(64) NOT NULL COMMENT '省份编码',
  dept_keyword VARCHAR(120) DEFAULT NULL COMMENT '部门关键词',
  project_keyword VARCHAR(120) DEFAULT NULL COMMENT '项目关键词',
  coordinator_user_id BIGINT NOT NULL COMMENT '区域统筹ID',
  coordinator_user_name VARCHAR(100) NOT NULL COMMENT '区域统筹名称',
  priority_no INT NOT NULL DEFAULT 100 COMMENT '匹配优先级',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  KEY idx_area_precise (sale_dept_code, province_code, priority_no),
  KEY idx_area_keyword (dept_keyword, project_keyword),
  KEY idx_area_user (coordinator_user_id),
  CONSTRAINT fk_area_user FOREIGN KEY (coordinator_user_id) REFERENCES sys_user(id)
) COMMENT='区域统筹划分管理';
