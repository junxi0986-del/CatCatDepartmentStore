package com.ecommerce.modules.behavior.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserBehavior {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer behaviorType;
    private Integer stayTime;
    private Date createTime;
}