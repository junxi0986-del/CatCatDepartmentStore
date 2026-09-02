package com.ecommerce.modules.chat.handler;

import com.ecommerce.modules.ai.service.AiService;
import com.ecommerce.modules.chat.entity.ChatMessage;
import com.ecommerce.modules.chat.mapper.ChatMapper;
import com.ecommerce.modules.chat.service.RagClientService;
import com.ecommerce.modules.order.entity.Order;
import com.ecommerce.modules.order.entity.OrderItem;
import com.ecommerce.modules.order.mapper.OrderMapper;
import com.ecommerce.modules.order.mapper.OrderItemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private ChatMapper chatMapper;

    @Resource
    private AiService aiService;

    @Resource
    private RagClientService ragClientService;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, Long> userOrderMap = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> sessionHumanModeMap = new ConcurrentHashMap<>();
    private static final Map<String, String> wsSessionToUserId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String userType = (String) session.getAttributes().get("userType");
        String id = (String) session.getAttributes().get("id");
        String orderIdStr = (String) session.getAttributes().get("orderId");

        if ("user".equals(userType)) {
            String sessionKey = id + "_" + sessionId;
            userSessions.put(sessionKey, session);
            wsSessionToUserId.put(sessionId, id);
            if (orderIdStr != null && !orderIdStr.isEmpty()) {
                try {
                    Long orderId = Long.parseLong(orderIdStr);
                    userOrderMap.put(id, orderId);
                } catch (NumberFormatException e) {
                    System.err.println("解析orderId失败: " + orderIdStr);
                }
            }
        } else if ("admin".equals(userType)) {
            adminSessions.put(id + "_" + sessionId, session);
            wsSessionToUserId.put(sessionId, id);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        if (payload.contains("heartbeat")) {
            session.sendMessage(new TextMessage("{\"type\": \"heartbeat\"}"));
            return;
        }
        
        if (payload.contains("switchToHuman")) {
            try {
                Map<String, Object> switchMsg = objectMapper.readValue(payload, Map.class);
                String chatSessionId = (String) switchMsg.get("sessionId");
                sessionHumanModeMap.put(chatSessionId, true);
                System.out.println("会话 " + chatSessionId + " 切换到人工客服模式");
                
                boolean adminOnline = !adminSessions.isEmpty();
                String replyContent;
                if (adminOnline) {
                    replyContent = "已为您转接人工客服，请稍候...";
                } else {
                    replyContent = "已为您申请人工客服，当前客服繁忙，请稍候...";
                }
                
                ChatMessage systemMessage = new ChatMessage();
                systemMessage.setUserId(switchMsg.get("userId") != null ? Long.parseLong(switchMsg.get("userId").toString()) : null);
                systemMessage.setUserName((String) switchMsg.get("userName"));
                systemMessage.setAdminId(1L);
                systemMessage.setAdminName("系统");
                systemMessage.setContent(replyContent);
                systemMessage.setType(1);
                systemMessage.setSessionId(chatSessionId);
                
                chatMapper.insert(systemMessage);
                
                String systemReplyJson = objectMapper.writeValueAsString(systemMessage);
                sendToUser(switchMsg.get("userId").toString(), new TextMessage(systemReplyJson));
                
                for (WebSocketSession adminSession : adminSessions.values()) {
                    try {
                        adminSession.sendMessage(new TextMessage(systemReplyJson));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("处理转人工消息失败: " + e.getMessage());
            }
            return;
        }
        
        try {
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            
            System.out.println("收到消息: " + payload);
            System.out.println("消息类型: " + chatMessage.getType());
            System.out.println("用户ID: " + chatMessage.getUserId());
            System.out.println("会话ID: " + chatMessage.getSessionId());
            
            String chatSessionId = chatMessage.getSessionId();
            boolean isHumanMode = Boolean.TRUE.equals(sessionHumanModeMap.get(chatSessionId));
            
            if (chatMessage.getType() == 0) {
                if (chatMessage.getAdminId() == null) {
                    chatMessage.setAdminId(1L);
                }
                
                boolean adminOnline = !adminSessions.isEmpty();
                if (isHumanMode && !adminOnline) {
                    System.out.println("人工模式但管理员不在线，自动切换回AI模式");
                    sessionHumanModeMap.remove(chatSessionId);
                    isHumanMode = false;
                }
                
                if (chatMessage.getAdminName() == null || chatMessage.getAdminName().isEmpty()) {
                    chatMessage.setAdminName(isHumanMode ? "人工客服" : "AI客服");
                }
                System.out.println("自动分配管理员ID: " + chatMessage.getAdminId() + ", 管理员名称: " + chatMessage.getAdminName());
            }
            
            int result = chatMapper.insert(chatMessage);
            System.out.println("保存消息结果: " + result + ", 消息ID: " + chatMessage.getId());
            
            if (chatMessage.getType() == 0) {
                for (WebSocketSession adminSession : adminSessions.values()) {
                    try {
                        adminSession.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
                if (isHumanMode) {
                    boolean adminOnline = !adminSessions.isEmpty();
                    if (!adminOnline) {
                        ChatMessage waitMessage = new ChatMessage();
                        waitMessage.setUserId(chatMessage.getUserId());
                        waitMessage.setUserName(chatMessage.getUserName());
                        waitMessage.setAdminId(1L);
                        waitMessage.setAdminName("系统");
                        waitMessage.setContent("当前客服繁忙，请稍候，我们会尽快回复您...");
                        waitMessage.setType(1);
                        waitMessage.setSessionId(chatSessionId);
                        
                        chatMapper.insert(waitMessage);
                        
                        String waitReplyJson = objectMapper.writeValueAsString(waitMessage);
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(new TextMessage(waitReplyJson));
                            } catch (IOException e) {
                                System.err.println("发送等待消息给用户失败: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    System.out.println("使用AI自动回复");
                    String aiReply = null;
                    
                    String orderNo = extractOrderNo(chatMessage.getContent());
                    if (orderNo != null) {
                        if (!isRealUser(chatMessage.getUserId())) {
                            aiReply = "抱歉，查询订单需要登录后才能使用，请先登录后再试。";
                        } else {
                            String orderInfo = queryOrderInfo(orderNo, chatMessage.getUserId());
                            if (orderInfo != null) {
                                aiReply = "根据您提供的订单号，我查询到以下订单信息：\n\n" + orderInfo;
                            } else {
                                aiReply = "抱歉，未找到该订单信息，请确认订单号是否正确或该订单是否属于您的账户。";
                            }
                        }
                    } else if (isShippingTimeQuestion(chatMessage.getContent())) {
                        if (!isRealUser(chatMessage.getUserId())) {
                            aiReply = null;
                        } else {
                            String pendingInfo = queryPendingOrders(chatMessage.getUserId());
                            if ("NO_PENDING".equals(pendingInfo)) {
                                aiReply = "您当前没有待发货的订单，所有订单均已发货或已完成。如需查询具体订单，请提供订单号。";
                            } else if (pendingInfo != null) {
                                aiReply = "您有以下待发货的订单：\n\n" + pendingInfo + "\n⏰ 我们通常会在下单后24-48小时内安排发货，请您耐心等待。发货后您可以在订单详情中查看物流信息。";
                            } else {
                                aiReply = null;
                            }
                        }
                    } else if (isOrderQueryIntent(chatMessage.getContent())) {
                        if (!isRealUser(chatMessage.getUserId())) {
                            aiReply = "抱歉，查询订单需要登录后才能使用，请先登录后再试。";
                        } else {
                            String recentOrders = queryRecentOrders(chatMessage.getUserId());
                            if (recentOrders != null) {
                                aiReply = "我为您查询到以下订单信息：\n\n" + recentOrders + "\n如需查看某个订单的详细信息，请提供对应的订单号。";
                            }
                        }
                    }
                    
                    if (aiReply == null) {
                        Long orderId = null;
                        if (isRealUser(chatMessage.getUserId())) {
                            orderId = userOrderMap.get(chatMessage.getUserId().toString());
                        }
                        
                        Map<String, Object> context = new HashMap<>();
                        if (orderId != null) {
                            Order order = orderMapper.selectById(orderId);
                            if (order != null && order.getUserId().equals(chatMessage.getUserId())) {
                                String orderInfo = String.format("订单号:%s, 状态:%s, 金额:%s",
                                        order.getOrderNo(),
                                        getOrderStatusText(order.getStatus()),
                                        order.getTotalPrice());
                                context.put("orderInfo", orderInfo);
                                
                                if (order.getLogisticsInfo() != null && !order.getLogisticsInfo().isEmpty()) {
                                    context.put("logisticsInfo", order.getLogisticsInfo());
                                }
                                if (order.getExpressNo() != null && !order.getExpressNo().isEmpty()) {
                                    context.put("expressNo", order.getExpressNo());
                                }
                                if (order.getExpressCompany() != null && !order.getExpressCompany().isEmpty()) {
                                    context.put("expressCompany", order.getExpressCompany());
                                }
                            }
                        }
                        
                        try {
                            System.out.println("检查RAG服务可用性...");
                            boolean ragAvailable = ragClientService != null && ragClientService.isAvailable();
                            System.out.println("RAG服务可用: " + ragAvailable);
                            
                            if (ragAvailable) {
                                System.out.println("使用Python RAG服务，发送消息: " + chatMessage.getContent());
                                aiReply = ragClientService.chat(chatMessage.getContent(), context);
                                System.out.println("RAG服务返回: " + aiReply);
                            } else if (aiService != null) {
                                System.out.println("使用Java AI服务");
                                if (context.isEmpty()) {
                                    aiReply = aiService.chat(chatMessage.getContent());
                                } else {
                                    aiReply = aiService.chatWithContext(chatMessage.getContent(), context);
                                }
                                System.out.println("AI服务返回: " + aiReply);
                            }
                        } catch (Exception e) {
                            System.err.println("AI服务调用失败: " + e.getMessage());
                            e.printStackTrace();
                            aiReply = null;
                        }
                    }
                    
                    if (aiReply == null || aiReply.isEmpty()) {
                        aiReply = "抱歉，AI客服暂时无法回复，请联系人工客服。";
                    }
                    
                    ChatMessage aiMessage = new ChatMessage();
                    aiMessage.setUserId(chatMessage.getUserId());
                    aiMessage.setUserName(chatMessage.getUserName());
                    aiMessage.setAdminId(1L);
                    aiMessage.setAdminName("AI客服");
                    aiMessage.setContent(aiReply);
                    aiMessage.setType(1);
                    aiMessage.setSessionId(chatSessionId);
                    
                    chatMapper.insert(aiMessage);
                    
                    String aiReplyJson = objectMapper.writeValueAsString(aiMessage);
                    
                    // AI回复只发给发起请求的WebSocket Session，避免多用户串流
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(aiReplyJson));
                        } catch (IOException e) {
                            System.err.println("发送AI回复给用户失败: " + e.getMessage());
                        }
                    }
                    
                    for (WebSocketSession adminSession : adminSessions.values()) {
                        try {
                            adminSession.sendMessage(new TextMessage(aiReplyJson));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (chatMessage.getType() == 1) {
                sessionHumanModeMap.put(chatSessionId, true);
                System.out.println("管理员发送消息，会话 " + chatSessionId + " 切换到人工客服模式");
                
                if (chatMessage.getUserId() != null) {
                    String userIdStr = chatMessage.getUserId().toString();
                    sendToUser(userIdStr, new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String wsSessionId = session.getId();
        String userType = (String) session.getAttributes().get("userType");
        String id = (String) session.getAttributes().get("id");

        String sessionKey = id + "_" + wsSessionId;
        wsSessionToUserId.remove(wsSessionId);

        if ("user".equals(userType)) {
            userSessions.remove(sessionKey);
            boolean hasOtherSessions = userSessions.keySet().stream()
                    .anyMatch(key -> key.startsWith(id + "_"));
            if (!hasOtherSessions) {
                userOrderMap.remove(id);
            }
        } else if ("admin".equals(userType)) {
            adminSessions.remove(sessionKey);
        }
    }
    
    public void clearHumanMode(String chatSessionId) {
        sessionHumanModeMap.remove(chatSessionId);
    }
    
    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "待发货";
            case 3: return "已发货";
            case 4: return "已完成";
            case 5: return "已取消";
            case 6: return "已退款";
            default: return "未知";
        }
    }
    
    private String extractOrderNo(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("ORDER[A-Za-z0-9]{8}", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }

    private boolean isOrderQueryIntent(String message) {
        String[] keywords = {"订单", "物流", "快递", "到哪了", "到哪儿了", "订单状态", "查询订单", "查看订单", "我的订单", "订单查询", "运单"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShippingTimeQuestion(String message) {
        String[] patterns = {
            "多久.*发货", "多久.*到", "多久.*收到", "多久.*能到",
            "什么时候.*发货", "什么时候.*到", "什么时候.*收到",
            "几天.*发货", "几天.*到", "几天.*收到",
            "发货时间", "到货时间", "收货时间",
            "多久能发", "多久能到", "多久送到",
            "什么时候发", "什么时候送",
            "能发货吗", "能发货了", "还发不发货",
            "什么时候可以发", "多久可以发",
            "几天能发", "还要多久"
        };
        for (String pattern : patterns) {
            if (message.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    private boolean isRealUser(Long userId) {
        return userId != null && userId > 0;
    }

    private String queryRecentOrders(Long userId) {
        List<Order> orders = orderMapper.selectByUserId(userId);
        if (orders == null || orders.isEmpty()) {
            return null;
        }

        int count = Math.min(orders.size(), 5);
        StringBuilder info = new StringBuilder();
        for (int i = 0; i < count; i++) {
            Order order = orders.get(i);
            if (i > 0) info.append("\n\n");
            info.append("📦 订单号：").append(order.getOrderNo()).append("\n");
            info.append("📋 状态：").append(getOrderStatusText(order.getStatus())).append("\n");
            info.append("💰 金额：¥").append(String.format("%.2f", order.getTotalPrice()));

            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            if (items != null && !items.isEmpty()) {
                StringBuilder productNames = new StringBuilder();
                for (OrderItem item : items) {
                    if (productNames.length() > 0) productNames.append("、");
                    productNames.append(item.getProductName());
                }
                info.append("\n🛍️ 商品：").append(productNames);
            }

            if (order.getExpressNo() != null && !order.getExpressNo().isEmpty()) {
                info.append("\n🚚 快递：").append(order.getExpressCompany()).append(" ").append(order.getExpressNo());
            }

            info.append("\n🕐 下单时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(order.getCreateTime()));
        }
        return info.toString();
    }

    private String queryPendingOrders(Long userId) {
        List<Order> orders = orderMapper.selectByUserId(userId);
        if (orders == null || orders.isEmpty()) {
            return null;
        }

        List<Order> pendingOrders = new ArrayList<>();
        for (Order order : orders) {
            if (order.getStatus() != null && (order.getStatus() == 0 || order.getStatus() == 1 || order.getStatus() == 2)) {
                pendingOrders.add(order);
            }
        }

        if (pendingOrders.isEmpty()) {
            return "NO_PENDING";
        }

        StringBuilder info = new StringBuilder();
        for (int i = 0; i < pendingOrders.size(); i++) {
            Order order = pendingOrders.get(i);
            if (i > 0) info.append("\n\n");
            info.append("📦 订单号：").append(order.getOrderNo()).append("\n");
            info.append("📋 状态：").append(getOrderStatusText(order.getStatus())).append("\n");

            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            if (items != null && !items.isEmpty()) {
                StringBuilder productNames = new StringBuilder();
                for (OrderItem item : items) {
                    if (productNames.length() > 0) productNames.append("、");
                    productNames.append(item.getProductName());
                }
                info.append("🛍️ 商品：").append(productNames).append("\n");
            }

            info.append("🕐 下单时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(order.getCreateTime()));
        }
        return info.toString();
    }
    
    private String queryOrderInfo(String orderNo, Long userId) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return null;
        }
        
        if (userId == null || !userId.equals(order.getUserId())) {
            return null;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("📦 订单号：").append(order.getOrderNo()).append("\n");
        info.append("📋 订单状态：").append(getOrderStatusText(order.getStatus())).append("\n");
        info.append("💰 订单金额：¥").append(String.format("%.2f", order.getTotalPrice()));
        if (order.getPayPrice() != null && order.getPayPrice() > 0) {
            info.append("\n💳 实付金额：¥").append(String.format("%.2f", order.getPayPrice()));
        }
        
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        if (items != null && !items.isEmpty()) {
            info.append("\n🛍️ 商品信息：");
            for (OrderItem item : items) {
                info.append("\n  • ").append(item.getProductName());
                if (item.getSpecInfo() != null && !item.getSpecInfo().isEmpty()) {
                    info.append("（").append(item.getSpecInfo()).append("）");
                }
                info.append(" × ").append(item.getQuantity());
                info.append("，单价¥").append(String.format("%.2f", item.getProductPrice()));
            }
        }
        
        if (order.getExpressNo() != null && !order.getExpressNo().isEmpty()) {
            info.append("\n🚚 快递公司：").append(order.getExpressCompany());
            info.append("\n📮 快递单号：").append(order.getExpressNo());
        }
        
        if (order.getLogisticsInfo() != null && !order.getLogisticsInfo().isEmpty()) {
            info.append("\n📍 物流信息：").append(order.getLogisticsInfo());
        }
        
        info.append("\n🕐 下单时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(order.getCreateTime()));
        
        return info.toString();
    }

    public static void sendMessageToUser(Long userId, String message) {
        sendToUser(userId.toString(), new TextMessage(message));
    }
    
    private static void sendToUser(String userId, TextMessage message) {
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            if (entry.getKey().startsWith(userId + "_") && entry.getValue().isOpen()) {
                try {
                    entry.getValue().sendMessage(message);
                } catch (IOException e) {
                    System.err.println("发送消息给用户失败: " + e.getMessage());
                }
            }
        }
    }
    
    private static WebSocketSession findUserSession(String userId) {
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            if (entry.getKey().startsWith(userId + "_") && entry.getValue().isOpen()) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static void sendMessageToAdmin(String message) {
        for (WebSocketSession session : adminSessions.values()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}