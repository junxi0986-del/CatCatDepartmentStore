package com.ecommerce.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiHttpClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callDeepSeekApi(String apiKey, String apiUrl, String model, List<Map<String, String>> messages) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            
            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);

            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                // 获取响应头中的编码
                String contentType = responseEntity.getContentType().getValue();
                System.out.println("Response Content-Type: " + contentType);
                
                // 使用UTF-8读取响应
                byte[] responseBytes = EntityUtils.toByteArray(responseEntity);
                String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
                
                System.out.println("Response Body: " + responseBody);
                
                // 解析响应
                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode choicesNode = rootNode.get("choices");
                if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
                    JsonNode messageNode = choicesNode.get(0).get("message");
                    if (messageNode != null) {
                        String content = messageNode.get("content").asText();
                        System.out.println("AI Response Content: " + content);
                        return content;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
