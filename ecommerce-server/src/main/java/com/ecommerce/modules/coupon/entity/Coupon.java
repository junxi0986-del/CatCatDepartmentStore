package com.ecommerce.modules.coupon.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class Coupon {
    private Long id;
    private String name;
    private Integer type;
    private Double minAmount;
    private Double discountAmount;
    private Integer totalCount;
    private Integer usedCount;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private Date createTime;
    private Date updateTime;
}