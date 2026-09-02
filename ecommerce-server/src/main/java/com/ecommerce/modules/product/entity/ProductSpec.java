package com.ecommerce.modules.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_spec")
public class ProductSpec {
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("spec_name")
    private String specName;
    @TableField("spec_value")
    private String specValue;
    private Double price;
    private Integer stock;
    private String image;
    private Integer sort;
}
