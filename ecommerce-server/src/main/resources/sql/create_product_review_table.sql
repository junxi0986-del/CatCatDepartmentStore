CREATE TABLE IF NOT EXISTS `product_review` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `rating` INT DEFAULT 5,
  `content` TEXT,
  `create_time` DATETIME DEFAULT NOW(),
  INDEX `idx_product_id` (`product_id`),
  INDEX `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
