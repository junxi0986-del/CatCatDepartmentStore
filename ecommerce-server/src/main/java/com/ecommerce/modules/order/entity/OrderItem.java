package com.ecommerce.modules.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productPic;
    private String specInfo;
    private Long specId;
    private Double productPrice;
    private Integer quantity;
    private Double totalPrice;
    private Date createTime;
}