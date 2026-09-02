package com.ecommerce.modules.coupon.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserCoupon {
    private Long id;
    private Long couponId;
    private Long userId;
    private Long orderId;
    private Integer status;
    private Date useTime;
    private Date createTime;
}