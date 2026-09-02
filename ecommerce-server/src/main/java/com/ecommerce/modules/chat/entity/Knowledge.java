package com.ecommerce.modules.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 知识库实体类
 * 用于存储AI智能客服的知识内容，支持RAG检索增强生成
 * 
 * @author ecommerce-team
 * @since 1.0.0
 */
@TableName("knowledge")
public class Knowledge {
    
    /**
     * 知识ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 知识内容，存储问答形式的文本
     * 例如："七天无理由退换怎么操作？答：签收后7天内..."
     */
    private String content;
    
    /**
     * 知识分类
     * 可选值：product(商品咨询)、logistics(物流配送)、after_sale(售后问题)、coupon(优惠券活动)、general(通用问题)
     */
    private String category;
    
    /**
     * 关键词，用于辅助检索
     * 多个关键词用逗号分隔，例如："退换,售后,七天"
     */
    private String keywords;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
    
    /**
     * 默认构造函数
     */
    public Knowledge() {}
    
    /**
     * 带参构造函数
     * 
     * @param content 知识内容
     * @param category 知识分类
     */
    public Knowledge(String content, String category) {
        this.content = content;
        this.category = category;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
