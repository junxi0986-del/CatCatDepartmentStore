-- 添加订单时间相关字段
-- 如果表不存在，请先创建 order_info 表

-- 添加支付时间字段
ALTER TABLE `order_info` 
ADD COLUMN IF NOT EXISTS `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间' AFTER `status`;

-- 添加发货时间字段
ALTER TABLE `order_info` 
ADD COLUMN IF NOT EXISTS `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间' AFTER `pay_time`;

-- 添加收货时间字段
ALTER TABLE `order_info` 
ADD COLUMN IF NOT EXISTS `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间' AFTER `delivery_time`;

-- 添加快递信息相关字段（如果还没有的话）
ALTER TABLE `order_info` 
ADD COLUMN IF NOT EXISTS `logistics_info` TEXT COMMENT '物流信息' AFTER `receive_time`;

ALTER TABLE `order_info` 
ADD COLUMN IF NOT EXISTS `express_no` VARCHAR(100) DEFAULT NULL COMMENT '快递单号' AFTER `logistics_info`;

ALTER TABLE `order_info` 
ADD COLUMN IF NOT EXISTS `express_company` VARCHAR(100) DEFAULT NULL COMMENT '快递公司' AFTER `express_no`;

-- 如果 MySQL 版本不支持 IF NOT EXISTS，使用以下语句（需要手动执行前先检查）
-- ALTER TABLE `order_info` ADD COLUMN `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间' AFTER `status`;
-- ALTER TABLE `order_info` ADD COLUMN `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间' AFTER `pay_time`;
-- ALTER TABLE `order_info` ADD COLUMN `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间' AFTER `delivery_time`;
-- ALTER TABLE `order_info` ADD COLUMN `logistics_info` TEXT COMMENT '物流信息' AFTER `receive_time`;
-- ALTER TABLE `order_info` ADD COLUMN `express_no` VARCHAR(100) DEFAULT NULL COMMENT '快递单号' AFTER `logistics_info`;
-- ALTER TABLE `order_info` ADD COLUMN `express_company` VARCHAR(100) DEFAULT NULL COMMENT '快递公司' AFTER `express_no`;
