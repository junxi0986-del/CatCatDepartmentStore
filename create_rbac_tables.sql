-- RBAC权限管理表
USE ecommerce_system;

CREATE TABLE IF NOT EXISTS `admin_role` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `role_key` VARCHAR(50) NOT NULL UNIQUE,
  `role_name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(200),
  `create_time` DATETIME DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_permission` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `permission_key` VARCHAR(50) NOT NULL UNIQUE,
  `permission_name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(200),
  `sort_order` INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_role_permission` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `role_key` VARCHAR(50) NOT NULL,
  `permission_key` VARCHAR(50) NOT NULL,
  UNIQUE KEY `uk_role_perm` (`role_key`, `permission_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `admin` ADD COLUMN `role_key` VARCHAR(50) DEFAULT 'store_admin' AFTER `role`;

INSERT INTO `admin_role` (`role_key`, `role_name`, `description`) VALUES
('super_admin', '超级管理员', '拥有所有权限，可管理其他管理员'),
('service_admin', '客服管理员', '负责客服相关业务'),
('store_admin', '商城管理员', '负责商城日常运营管理');

INSERT INTO `admin_permission` (`permission_key`, `permission_name`, `description`, `sort_order`) VALUES
('dashboard', '数据概览', '查看数据概览和统计', 1),
('user', '用户管理', '管理用户信息', 2),
('product', '商品管理', '管理商品信息', 3),
('order', '订单管理', '管理订单信息', 4),
('service', '客服中心', '在线客服服务', 5),
('knowledge', '知识库管理', '管理客服知识库', 6),
('coupon', '促销管理', '管理优惠券和促销活动', 7),
('behavior', '用户行为', '查看用户行为分析', 8),
('userCoupon', '用户优惠券', '查看用户优惠券', 9),
('system', '系统配置', '管理系统配置', 10),
('admin', '管理员管理', '管理管理员账号和权限', 11);

INSERT INTO `admin_role_permission` (`role_key`, `permission_key`) VALUES
('super_admin', 'dashboard'),
('super_admin', 'user'),
('super_admin', 'product'),
('super_admin', 'order'),
('super_admin', 'service'),
('super_admin', 'knowledge'),
('super_admin', 'coupon'),
('super_admin', 'behavior'),
('super_admin', 'userCoupon'),
('super_admin', 'system'),
('super_admin', 'admin'),
('service_admin', 'dashboard'),
('service_admin', 'order'),
('service_admin', 'service'),
('store_admin', 'dashboard'),
('store_admin', 'user'),
('store_admin', 'product'),
('store_admin', 'order'),
('store_admin', 'knowledge'),
('store_admin', 'coupon');

UPDATE `admin` SET `role_key` = 'super_admin' WHERE `username` = 'admin';
