-- Add message_type and image_url columns to chat_message table
ALTER TABLE `chat_message` ADD COLUMN `message_type` TINYINT DEFAULT 0 COMMENT '消息类型: 0-文本, 1-图片';
ALTER TABLE `chat_message` ADD COLUMN `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL';
