package com.ecommerce.modules.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Double totalPrice;
    private Double payPrice;
    private Double discountAmount;
    private Long couponId;
    private Long userCouponId;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;
    private Integer status;
    private Date payTime;
    private Date deliveryTime;
    private Date receiveTime;
    private Date createTime;
    private Date updateTime;
    private String logisticsInfo;
    private String expressNo;
    private String expressCompany;
    private Integer refundStatus;
    private Date refundApplyTime;
    private String refundReason;
    private String refundReply;
}