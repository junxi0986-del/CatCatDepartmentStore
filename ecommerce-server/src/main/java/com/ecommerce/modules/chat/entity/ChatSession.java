package com.ecommerce.modules.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ChatSession {
    private Long id;
    private Long userId;
    private Long orderId;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
