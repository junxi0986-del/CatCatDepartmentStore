package com.ecommerce.modules.user.entity;

import lombok.Data;

import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String avatar;
    private Integer status; // 1正常 0禁用
    private Date createTime;
    private Date updateTime;
}
