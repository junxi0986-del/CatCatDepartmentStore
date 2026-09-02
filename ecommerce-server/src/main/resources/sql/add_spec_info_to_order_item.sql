-- 为 order_item 表添加 spec_info 字段（如果不存在）
-- 用于保存商品规格信息，如 "颜色: 黑色, 尺寸: 128GB"

ALTER TABLE order_item ADD COLUMN IF NOT EXISTS spec_info VARCHAR(255) DEFAULT NULL COMMENT '商品规格信息';

-- 如果上面的语句不支持 IF NOT EXISTS，可以使用下面的语句
-- ALTER TABLE order_item ADD COLUMN spec_info VARCHAR(255) DEFAULT NULL COMMENT '商品规格信息';
