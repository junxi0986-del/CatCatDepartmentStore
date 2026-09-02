package com.ecommerce.modules.product.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Product {
    private Long id;
    private String name;
    private Long categoryId;
    private Double price;
    private Double marketPrice;
    private Integer stock;
    private String pic;
    private String images;
    private String detail;
    private Integer status; // 1上架 0下架
    private Integer sales;
    private Date createTime;
    private Date updateTime;
}
