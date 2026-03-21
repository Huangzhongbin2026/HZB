-- 系统管理模块建表脚本
-- schema: db_supply_task

USE db_supply_task;

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
  menu_type VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT '类型:DIR/MENU/BUTTON',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级菜单ID',
  route_path VARCHAR(255) NOT NULL COMMENT '路由地址',
  component_path VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  permission_code VARCHAR(120) NOT NULL COMMENT '权限标识',
  icon VARCHAR(100) DEFAULT NULL COMMENT '图标',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  is_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  env_tag VARCHAR(20) NOT NULL DEFAULT 'default' COMMENT '环境标签',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_menu_route (route_path),
  UNIQUE KEY uk_menu_permission (permission_code),
  KEY idx_parent_sort (parent_id, sort_no),
  KEY idx_enabled_visible (is_enabled, is_visible)
) COMMENT='系统菜单表';

CREATE TABLE IF NOT EXISTS sys_dict_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
  dict_code VARCHAR(100) NOT NULL COMMENT '字典编码',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_dict_type_code (dict_code),
  KEY idx_dict_type_enabled (is_enabled, sort_no)
) COMMENT='字典分类表';

CREATE TABLE IF NOT EXISTS sys_dict_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  dict_type_id BIGINT NOT NULL COMMENT '字典分类ID',
  item_name VARCHAR(120) NOT NULL COMMENT '字典项名称',
  item_code VARCHAR(120) NOT NULL COMMENT '字典项编码',
  item_value VARCHAR(120) NOT NULL COMMENT '字典项值',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_dict_item_code (item_code),
  UNIQUE KEY uk_type_item_value (dict_type_id, item_value),
  KEY idx_dict_item_name (item_name),
  KEY idx_dict_item_enabled (is_enabled, sort_no),
  CONSTRAINT fk_dict_item_type FOREIGN KEY (dict_type_id) REFERENCES sys_dict_type(id)
) COMMENT='字典项表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(100) NOT NULL COMMENT '角色编码',
  dept_code VARCHAR(64) DEFAULT NULL COMMENT '所属部门',
  description VARCHAR(500) DEFAULT NULL COMMENT '描述',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  inherit_role_id BIGINT DEFAULT NULL COMMENT '继承角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_role_code (role_code),
  KEY idx_role_enabled (is_enabled),
  KEY idx_role_dept (dept_code)
) COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_menu_id (menu_id),
  CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id)
) COMMENT='角色菜单权限表';

CREATE TABLE IF NOT EXISTS sys_role_data_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  biz_table VARCHAR(100) NOT NULL COMMENT '业务表名',
  scope_type VARCHAR(30) NOT NULL COMMENT '数据范围:ALL/DEPT/SELF/CUSTOM_DEPT',
  custom_dept_codes VARCHAR(1000) DEFAULT NULL COMMENT '自定义部门编码列表',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_role_table_scope (role_id, biz_table),
  KEY idx_scope_type (scope_type),
  CONSTRAINT fk_data_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) COMMENT='角色数据权限表';

CREATE TABLE IF NOT EXISTS sys_role_field_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  biz_table VARCHAR(100) NOT NULL COMMENT '业务表名',
  field_code VARCHAR(100) NOT NULL COMMENT '字段编码',
  permission_type VARCHAR(20) NOT NULL COMMENT '权限:VISIBLE/EDITABLE/HIDDEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_role_field (role_id, biz_table, field_code),
  KEY idx_field_permission (permission_type),
  CONSTRAINT fk_field_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) COMMENT='角色字段权限表';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_name VARCHAR(100) NOT NULL COMMENT '姓名',
  account VARCHAR(100) NOT NULL COMMENT '登录账号',
  mobile VARCHAR(20) NOT NULL COMMENT '手机号',
  feishu_id VARCHAR(100) NOT NULL COMMENT '飞书ID',
  email VARCHAR(120) DEFAULT NULL COMMENT '邮箱',
  dept_code VARCHAR(64) DEFAULT NULL COMMENT '部门编码',
  password_hash VARCHAR(255) NOT NULL COMMENT 'MD5+盐加密密码',
  password_salt VARCHAR(64) NOT NULL COMMENT '盐值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用0禁用',
  login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续失败次数',
  lock_expire_at DATETIME DEFAULT NULL COMMENT '锁定到期时间',
  last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_user_account (account),
  UNIQUE KEY uk_user_mobile (mobile),
  UNIQUE KEY uk_user_feishu (feishu_id),
  KEY idx_user_dept (dept_code),
  KEY idx_user_status (status)
) COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  updated_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user_role_role (role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_permission_cache (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  cache_key VARCHAR(120) NOT NULL COMMENT '缓存键',
  cache_payload JSON NOT NULL COMMENT '权限快照',
  expired_at DATETIME NOT NULL COMMENT '过期时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_permission_cache_key (cache_key),
  KEY idx_permission_cache_user (user_id),
  KEY idx_permission_cache_expired (expired_at)
) COMMENT='权限缓存快照表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  oper_user VARCHAR(100) NOT NULL COMMENT '操作人',
  oper_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  oper_ip VARCHAR(64) NOT NULL COMMENT '操作IP',
  oper_type VARCHAR(50) NOT NULL COMMENT '操作类型',
  oper_module VARCHAR(80) NOT NULL COMMENT '操作模块',
  oper_content VARCHAR(1000) NOT NULL COMMENT '操作内容',
  oper_result VARCHAR(20) NOT NULL COMMENT '操作结果',
  request_uri VARCHAR(300) DEFAULT NULL COMMENT '请求URI',
  request_method VARCHAR(20) DEFAULT NULL COMMENT '请求方法',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
  KEY idx_log_user_time (oper_user, oper_time),
  KEY idx_log_module_time (oper_module, oper_time),
  KEY idx_log_type_time (oper_type, oper_time),
  KEY idx_log_ip_time (oper_ip, oper_time),
  FULLTEXT KEY ft_log_content (oper_content)
) COMMENT='系统操作日志表';

-- 建议分区: sys_operation_log 按月分区或按季度归档
