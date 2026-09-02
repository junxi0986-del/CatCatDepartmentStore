package com.ecommerce.modules.chat.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.chat.entity.ChatMessage;
import com.ecommerce.modules.chat.handler.ChatWebSocketHandler;
import com.ecommerce.modules.chat.mapper.ChatMapper;
import com.ecommerce.modules.chat.service.RagService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatMapper chatMapper;

    @Resource
    private RagService ragService;
    
    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @PostMapping("/session/create")
    public Result createSession(@RequestBody Map<String, Object> params,
                               @RequestHeader(value = "token", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        String sessionId = "session_" + userId + "_" + System.currentTimeMillis();
        
        return Result.success(Map.of("sessionId", sessionId));
    }

    @GetMapping("/session/{id}")
    public Result getSession(@PathVariable String id) {
        List<ChatMessage> messages = chatMapper.selectBySessionId(id);
        return Result.success(Map.of("sessionId", id, "messages", messages));
    }

    @GetMapping("/session/user")
    public Result getUserSessions(@RequestHeader(value = "token", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(java.util.Collections.emptyList());
    }

    @GetMapping("/session/active")
    public Result getActiveSessions() {
        Long adminId = 1L;
        List<String> sessionIds = chatMapper.selectSessionsByAdminId(adminId);
        return Result.success(sessionIds);
    }

    @PostMapping("/message/send")
    public Result sendMessage(@RequestBody Map<String, Object> params,
                             @RequestHeader(value = "token", required = false) String token) {
        String sessionId = (String) params.get("sessionId");
        String content = (String) params.get("content");
        Long userId = getUserIdFromToken(token);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setType(0);
        userMessage.setContent(content);
        userMessage.setAdminId(1L);
        userMessage.setAdminName("AI客服");
        chatMapper.insert(userMessage);

        String aiReply = ragService.chat(content);
        
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setUserId(userId);
        aiMessage.setType(1);
        aiMessage.setContent(aiReply);
        aiMessage.setAdminId(1L);
        aiMessage.setAdminName("AI客服");
        chatMapper.insert(aiMessage);

        return Result.success("发送成功");
    }

    @PostMapping("/message/admin")
    public Result sendAdminMessage(@RequestBody Map<String, Object> params) {
        String sessionId = (String) params.get("sessionId");
        String content = (String) params.get("content");
        Long adminId = params.get("adminId") != null ? Long.parseLong(params.get("adminId").toString()) : 0L;
        Long userId = params.get("userId") != null ? Long.parseLong(params.get("userId").toString()) : null;

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setType(1);
        message.setAdminId(adminId);
        message.setAdminName("人工客服");
        message.setContent(content);
        chatMapper.insert(message);

        return Result.success("发送成功");
    }

    @GetMapping("/message/{sessionId}")
    public Result getMessages(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatMapper.selectBySessionId(sessionId);
        return Result.success(messages);
    }

    @GetMapping("/sessions")
    public Result getSessions(@RequestParam Long adminId) {
        List<String> sessionIds = chatMapper.selectSessionsByAdminId(adminId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (String sessionId : sessionIds) {
            ChatMessage lastMsg = chatMapper.selectLastMessageBySessionId(sessionId);
            int unreadCount = chatMapper.countUnreadBySessionId(sessionId);
            
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", sessionId);
            sessionInfo.put("lastMessage", lastMsg != null ? lastMsg.getContent() : "");
            sessionInfo.put("unreadCount", unreadCount);
            sessionInfo.put("userId", lastMsg != null ? lastMsg.getUserId() : null);
            sessionInfo.put("userName", lastMsg != null ? lastMsg.getUserName() : "用户");
            result.add(sessionInfo);
        }
        
        return Result.success(result);
    }

    @GetMapping("/unreadCount")
    public Result getUnreadCount(@RequestParam String sessionId) {
        int count = chatMapper.countUnreadBySessionId(sessionId);
        return Result.success(count);
    }

    @PostMapping("/markAsRead")
    public Result markAsRead(@RequestParam String sessionId) {
        chatMapper.markAsRead(sessionId);
        return Result.success("已标记为已读");
    }

    @DeleteMapping("/session/{sessionId}")
    public Result deleteSession(@PathVariable String sessionId) {
        chatMapper.deleteBySessionId(sessionId);
        chatWebSocketHandler.clearHumanMode(sessionId);
        return Result.success("会话已删除");
    }

    @PostMapping("/session/{id}/takeover")
    public Result takeOver(@PathVariable String id) {
        return Result.success("已接管会话");
    }

    @PostMapping("/session/{id}/close")
    public Result closeSession(@PathVariable String id) {
        return Result.success("会话已关闭");
    }

    private Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return 1L;
        }
        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length >= 2) {
                String payload = new String(Base64.getDecoder().decode(tokenParts[1]));
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> tokenData = objectMapper.readValue(payload, Map.class);
                if (tokenData.containsKey("userId")) {
                    return Long.valueOf(tokenData.get("userId").toString());
                }
            }
        } catch (Exception e) {
        }
        return 1L;
    }
}
