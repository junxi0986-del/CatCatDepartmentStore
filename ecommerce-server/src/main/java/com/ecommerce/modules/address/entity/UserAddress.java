package com.ecommerce.modules.address.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserAddress {
    private Long id;
    private Long userId;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String area;
    private String detailAddress;
    private Integer isDefault;
    private Date createTime;
}
