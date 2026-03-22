package com.ruijie.supplysystem.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaBootstrapRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> ddl = List.of(
                """
                CREATE TABLE IF NOT EXISTS sys_menu (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  menu_name VARCHAR(100) NOT NULL,
                  menu_type VARCHAR(20) NOT NULL DEFAULT 'MENU',
                  parent_id BIGINT NOT NULL DEFAULT 0,
                  route_path VARCHAR(255) NOT NULL,
                  component_path VARCHAR(255) NULL,
                  permission_code VARCHAR(120) NOT NULL,
                  icon VARCHAR(100) NULL,
                  sort_no INT NOT NULL DEFAULT 0,
                  is_visible TINYINT NOT NULL DEFAULT 1,
                  is_enabled TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_dict_type (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  dict_name VARCHAR(100) NOT NULL,
                  dict_code VARCHAR(100) NOT NULL,
                  sort_no INT NOT NULL DEFAULT 0,
                  is_enabled TINYINT NOT NULL DEFAULT 1,
                  remark VARCHAR(500) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_dict_item (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  dict_type_id BIGINT NOT NULL,
                  item_name VARCHAR(120) NOT NULL,
                  item_code VARCHAR(120) NOT NULL,
                  item_value VARCHAR(120) NOT NULL,
                  sort_no INT NOT NULL DEFAULT 0,
                  is_enabled TINYINT NOT NULL DEFAULT 1,
                  remark VARCHAR(500) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_role (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  role_name VARCHAR(100) NOT NULL,
                  role_code VARCHAR(100) NOT NULL,
                  dept_code VARCHAR(64) NULL,
                  description VARCHAR(500) NULL,
                  is_enabled TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_role_menu (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  role_id BIGINT NOT NULL,
                  menu_id BIGINT NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_role_data_permission (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  role_id BIGINT NOT NULL,
                  biz_table VARCHAR(100) NOT NULL,
                  scope_type VARCHAR(30) NOT NULL,
                  custom_dept_codes VARCHAR(1000) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_role_field_permission (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  role_id BIGINT NOT NULL,
                  biz_table VARCHAR(100) NOT NULL,
                  field_code VARCHAR(100) NOT NULL,
                  permission_type VARCHAR(20) NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_user (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_name VARCHAR(100) NOT NULL,
                  account VARCHAR(100) NOT NULL,
                  mobile VARCHAR(20) NOT NULL,
                  feishu_id VARCHAR(100) NOT NULL,
                  email VARCHAR(120) NULL,
                  dept_code VARCHAR(64) NULL,
                  password_hash VARCHAR(255) NOT NULL,
                  password_salt VARCHAR(64) NOT NULL,
                  status TINYINT NOT NULL DEFAULT 1,
                  last_login_at DATETIME NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_user_role (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  role_id BIGINT NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_operation_log (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  oper_user VARCHAR(100) NOT NULL,
                  oper_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  oper_ip VARCHAR(64) NOT NULL,
                  oper_type VARCHAR(50) NOT NULL,
                  oper_module VARCHAR(80) NOT NULL,
                  oper_content VARCHAR(1000) NOT NULL,
                  oper_result VARCHAR(20) NOT NULL,
                  request_uri VARCHAR(300) NULL,
                  request_method VARCHAR(20) NULL,
                  trace_id VARCHAR(64) NULL,
                  remark VARCHAR(500) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_aux_leave_config (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id VARCHAR(64) NOT NULL,
                  user_name VARCHAR(100) NOT NULL,
                  leave_start DATETIME NOT NULL,
                  leave_end DATETIME NOT NULL,
                  leave_reason VARCHAR(500) NULL,
                  remark VARCHAR(500) NULL,
                  status TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_aux_virtual_product (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  product_model VARCHAR(128) NOT NULL,
                  auto_reply_content VARCHAR(1000) NULL,
                  status TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_aux_message_push (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  push_name VARCHAR(120) NOT NULL,
                  route_code VARCHAR(120) NOT NULL,
                  feishu_template_code VARCHAR(120) NULL,
                  is_enabled TINYINT NOT NULL DEFAULT 1,
                  push_rule VARCHAR(500) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_aux_leave_agent_product (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  product_model VARCHAR(128) NOT NULL,
                  original_user_id VARCHAR(64) NOT NULL,
                  original_user_name VARCHAR(100) NOT NULL,
                  agent_user_id VARCHAR(64) NOT NULL,
                  agent_user_name VARCHAR(100) NOT NULL,
                  status TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS sys_aux_area_coordinator (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  sale_dept_code VARCHAR(64) NOT NULL,
                  province_code VARCHAR(64) NOT NULL,
                  region VARCHAR(64) NULL,
                  dept_keyword VARCHAR(120) NULL,
                  project_keyword VARCHAR(120) NULL,
                  coordinator_user_id VARCHAR(64) NOT NULL,
                  coordinator_user_name VARCHAR(100) NOT NULL,
                  agent_coordinator_user_id VARCHAR(64) NOT NULL,
                  agent_coordinator_user_name VARCHAR(100) NOT NULL,
                  priority_no INT NOT NULL DEFAULT 100,
                  status TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  updated_by VARCHAR(64) NOT NULL DEFAULT 'system',
                  is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """,
                "SELECT 1"
        );

        ddl.forEach(sql -> {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ex) {
                log.warn("Schema bootstrap statement skipped: {}", sql, ex);
            }
        });

        addColumnIfMissing("sys_user", "user_name", "ALTER TABLE sys_user ADD COLUMN user_name VARCHAR(100) NOT NULL DEFAULT 'unknown'");
        addColumnIfMissing("sys_user", "account", "ALTER TABLE sys_user ADD COLUMN account VARCHAR(100) NOT NULL DEFAULT ''");
        addColumnIfMissing("sys_user", "mobile", "ALTER TABLE sys_user ADD COLUMN mobile VARCHAR(20) NOT NULL DEFAULT ''");
        addColumnIfMissing("sys_user", "feishu_id", "ALTER TABLE sys_user ADD COLUMN feishu_id VARCHAR(100) NOT NULL DEFAULT ''");
        addColumnIfMissing("sys_user", "email", "ALTER TABLE sys_user ADD COLUMN email VARCHAR(120) NULL");
        addColumnIfMissing("sys_user", "dept_code", "ALTER TABLE sys_user ADD COLUMN dept_code VARCHAR(64) NULL");
        addColumnIfMissing("sys_user", "password_hash", "ALTER TABLE sys_user ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT ''");
        addColumnIfMissing("sys_user", "password_salt", "ALTER TABLE sys_user ADD COLUMN password_salt VARCHAR(64) NOT NULL DEFAULT ''");
        addColumnIfMissing("sys_user", "status", "ALTER TABLE sys_user ADD COLUMN status TINYINT NOT NULL DEFAULT 1");
        addColumnIfMissing("sys_user", "last_login_at", "ALTER TABLE sys_user ADD COLUMN last_login_at DATETIME NULL");
        addColumnIfMissing("sys_user", "is_deleted", "ALTER TABLE sys_user ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0");
        addColumnIfMissing("sys_aux_leave_config", "remark", "ALTER TABLE sys_aux_leave_config ADD COLUMN remark VARCHAR(500) NULL");
        addColumnIfMissing("sys_aux_area_coordinator", "region", "ALTER TABLE sys_aux_area_coordinator ADD COLUMN region VARCHAR(64) NULL");
        addColumnIfMissing("sys_aux_area_coordinator", "agent_coordinator_user_id", "ALTER TABLE sys_aux_area_coordinator ADD COLUMN agent_coordinator_user_id VARCHAR(64) NOT NULL DEFAULT ''");
        addColumnIfMissing("sys_aux_area_coordinator", "agent_coordinator_user_name", "ALTER TABLE sys_aux_area_coordinator ADD COLUMN agent_coordinator_user_name VARCHAR(100) NOT NULL DEFAULT ''");
      }

      private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        try {
          Long count = jdbcTemplate.queryForObject(
              "SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
              Long.class,
              tableName,
              columnName
          );
          if (count != null && count == 0L) {
            jdbcTemplate.execute(alterSql);
          }
        } catch (Exception ex) {
          log.warn("Schema bootstrap add-column skipped: {}.{}", tableName, columnName, ex);
        }
    }
}
