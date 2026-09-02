package com.ecommerce.modules.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ChatMessage {
    private Long id;
    private Long userId;
    private String userName;
    private Long adminId;
    private String adminName;
    private String content;
    private Integer type;
    private Date createTime;
    private String sessionId;
    private Integer isRead;
    private Integer messageType;
    private String imageUrl;
}
