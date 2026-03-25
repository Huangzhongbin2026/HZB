-- 增量字段变更脚本（适用于已初始化过库表的环境）
USE jithub_db;

-- 1) 已下单加急任务：任务提交人 -> 提问人
ALTER TABLE tm_task_expedited_order
  CHANGE COLUMN submitter_name questioner_name VARCHAR(64) NULL;

-- 2) 客期变更任务：新增字段
ALTER TABLE tm_task_customer_due_change
  ADD COLUMN salesperson_name VARCHAR(64) NULL COMMENT '业务员姓名' AFTER contract_no,
  ADD COLUMN latest_risk_level VARCHAR(32) NULL COMMENT '最新风险等级' AFTER salesperson_name,
  ADD COLUMN is_closed_loop TINYINT NOT NULL DEFAULT 0 COMMENT '是否闭环' AFTER latest_risk_level;
