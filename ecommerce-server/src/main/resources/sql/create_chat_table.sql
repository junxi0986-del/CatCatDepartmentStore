-- Create chat message table
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `user_name` VARCHAR(255) DEFAULT NULL,
  `admin_id` BIGINT DEFAULT NULL,
  `admin_name` VARCHAR(255) DEFAULT NULL,
  `content` TEXT,
  `type` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `session_id` VARCHAR(255) DEFAULT NULL,
  `is_read` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_session_id` (`session_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_admin_id` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;