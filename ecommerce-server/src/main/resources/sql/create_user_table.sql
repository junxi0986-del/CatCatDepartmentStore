CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(20),
  `avatar` VARCHAR(255),
  `create_time` DATETIME DEFAULT NOW(),
  `update_time` DATETIME DEFAULT NOW()
);

-- 插入默认用户数据
INSERT INTO `user` (`username`, `password`, `phone`, `avatar`) VALUES
('user', '123456', '13800138000', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=user%20avatar&image_size=square'),
('admin', '123456', '13900139000', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=admin%20avatar&image_size=square');
