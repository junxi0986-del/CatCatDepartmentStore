package com.ecommerce.modules.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagService {

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "你是猫猫百货商城的智能客服助手。你需要热情、专业地帮助用户解答问题，包括：\n" +
            "1. 商品咨询：解答商品功能、价格、库存等问题\n" +
            "2. 订单查询：帮助用户查询订单状态、物流信息\n" +
            "3. 售后服务：处理退款退货等问题\n" +
            "4. 购物引导：根据用户需求推荐合适的商品\n\n" +
            "如果无法回答的问题，请礼貌告知用户\"很抱歉，我暂时无法回答您的问题，建议您直接联系在线客服，我们会尽快为您处理。\"，不要编造内容。\n" +
            "不要提及\"知识库\"、\"参考知识库\"等内部信息来源。\n" +
            "请使用友好的语气回答，可以适当使用emoji增加亲和力。";

    public String chat(String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "AI客服暂时不可用，请联系人工客服获取帮助。";
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("temperature", 0.3);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    return message.get("content");
                }
            }

            return "抱歉，AI客服暂时无法响应，请稍后再试或联系人工客服。";

        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI客服出现错误，请联系人工客服获取帮助。";
        }
    }
}
