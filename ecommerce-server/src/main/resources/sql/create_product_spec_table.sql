CREATE TABLE IF NOT EXISTS `product_spec` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `spec_name` VARCHAR(50) NOT NULL COMMENT '规格名称（如：颜色、型号）',
  `spec_value` VARCHAR(100) NOT NULL COMMENT '规格值（如：红色、XL）',
  `price` DECIMAL(10,2) DEFAULT 0 COMMENT '规格加价',
  `stock` INT DEFAULT 0 COMMENT '规格库存',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '规格图片',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT NOW(),
  `update_time` DATETIME DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

-- 添加索引
CREATE INDEX idx_product_id ON product_spec(product_id);
CREATE INDEX idx_spec_name ON product_spec(spec_name);
