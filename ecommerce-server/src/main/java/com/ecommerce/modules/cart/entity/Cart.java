package com.ecommerce.modules.cart.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cart")
public class Cart {
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("product_id")
    private Long productId;
    private Integer quantity;
    private Integer selected;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

    // 关联商品信息（非数据库字段）
    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String productPic;
    @TableField(exist = false)
    private Double productPrice;
}
