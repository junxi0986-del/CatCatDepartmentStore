package com.ecommerce.modules.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG客户端服务
 * 用于Java后端与Python RAG服务进行HTTP通信
 * 
 * 主要功能：
 * 1. 调用RAG服务进行智能对话
 * 2. 检查RAG服务是否可用
 * 3. 向向量库添加知识
 * 4. 检索知识库
 * 
 * @author ecommerce-team
 * @since 1.0.0
 */
@Service
public class RagClientService {

    /**
     * RAG服务地址，从配置文件读取，默认为http://localhost:5000
     */
    @Value("${rag.service-url:http://localhost:5000}")
    private String ragServiceUrl;

    /**
     * HTTP客户端，用于发送请求
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    
    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用RAG服务进行对话（无上下文）
     * 
     * @param userMessage 用户消息
     * @return AI回复内容
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * 调用RAG服务进行对话（带上下文）
     * 
     * @param userMessage 用户消息
     * @param context 上下文信息，如订单信息等
     * @return AI回复内容
     */
    public String chat(String userMessage, Map<String, Object> context) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", userMessage);
            if (context != null && !context.isEmpty()) {
                requestBody.put("context", context);
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ragServiceUrl + "/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.sendAsync(
                    request, HttpResponse.BodyHandlers.ofString()).get(30, java.util.concurrent.TimeUnit.SECONDS);

            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                if (Boolean.TRUE.equals(responseMap.get("success"))) {
                    return (String) responseMap.get("answer");
                } else {
                    return "抱歉，AI客服暂时无法回复，请稍后再试。";
                }
            }

            return "抱歉，AI客服服务暂时不可用，请联系人工客服。";

        } catch (java.util.concurrent.TimeoutException e) {
            return "抱歉，AI客服响应超时，请稍后再试或联系人工客服。";
        } catch (Exception e) {
            return "抱歉，AI客服服务连接失败，请联系人工客服。";
        }
    }

    /**
     * 检查RAG服务是否可用
     * 通过调用健康检查接口判断服务状态
     * 
     * @return true-服务可用，false-服务不可用
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ragServiceUrl + "/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 向向量库添加知识
     * 调用Python RAG服务的/knowledge/add接口
     * 
     * @param content 知识内容
     * @param metadata 元数据，如分类信息
     * @return true-添加成功，false-添加失败
     */
    public boolean addKnowledge(String content, Map<String, Object> metadata) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("content", content);
            if (metadata != null) {
                requestBody.put("metadata", metadata);
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发送HTTP POST请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ragServiceUrl + "/knowledge/add"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 处理响应
            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                return Boolean.TRUE.equals(responseMap.get("success"));
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteKnowledge(String content) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("content", content);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ragServiceUrl + "/knowledge/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                return Boolean.TRUE.equals(responseMap.get("success"));
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检索知识库（使用默认最小相似度0.3）
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 检索结果
     */
    public Map<String, Object> searchKnowledge(String query, int topK) {
        return searchKnowledge(query, topK, 0.3);
    }

    /**
     * 检索知识库
     * 调用Python RAG服务的/knowledge/search接口进行向量检索
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param minScore 最小相似度阈值（0-1）
     * @return 检索结果，包含results列表和vector_store_count
     */
    public Map<String, Object> searchKnowledge(String query, int topK, double minScore) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("top_k", topK);
            requestBody.put("min_score", minScore);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发送HTTP POST请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ragServiceUrl + "/knowledge/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 处理响应
            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                if (Boolean.TRUE.equals(responseMap.get("success"))) {
                    return responseMap;
                }
            }

            return Map.of("success", false, "results", java.util.Collections.emptyList());

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
