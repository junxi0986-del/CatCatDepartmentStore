package com.ecommerce.modules.recommend.service;

import com.ecommerce.modules.behavior.mapper.UserBehaviorMapper;
import com.ecommerce.modules.behavior.entity.UserBehavior;
import com.ecommerce.modules.product.entity.Product;
import com.ecommerce.modules.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiRecommendService {

    @Value("${deepseek.api-key}")
    private String apiKey;
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Resource
    private UserBehaviorMapper behaviorMapper;

    @Resource
    private ProductMapper productMapper;

    public List<Product> recommendProducts(Long userId) {
        // 1. 查询用户历史行为数据
        List<UserBehavior> behaviors = behaviorMapper.selectByUserId(userId);
        // 2. 拼接请求参数，传给DeepSeek AI
        String prompt = buildPrompt(behaviors);
        // 3. 调用API
        String aiResult = HttpUtil.post(API_URL, buildRequest(prompt), apiKey);
        // 4. 解析AI返回的商品ID，查询商品
        List<Long> productIds = parseAiResult(aiResult);
        return productMapper.selectBatchIds(productIds);
    }

    // 构建AI提示词
    private String buildPrompt(List<UserBehavior> behaviors) {
        return "根据用户购物行为，推荐10个商品ID，只返回数字，用逗号分隔：" + behaviors.toString();
    }

    // 构建请求参数
    private String buildRequest(String prompt) {
        return "{" +
                "\"model\": \"deepseek-chat\"," +
                "\"messages\": [" +
                "{\"role\": \"user\", \"content\": \"" + prompt + "\"}" +
                "]," +
                "\"temperature\": 0.7" +
                "}";
    }

    // 解析AI返回结果
    private List<Long> parseAiResult(String aiResult) {
        // 模拟解析结果，实际需要根据API返回格式进行解析
        List<Long> productIds = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            productIds.add(i);
        }
        return productIds;
    }

    // 简单的HTTP工具类
    private static class HttpUtil {
        public static String post(String url, String request, String apiKey) {
            // 模拟HTTP请求，实际需要使用OkHttp或HttpClient
            return "{\"choices\": [{\"message\": {\"content\": \"1,2,3,4,5,6,7,8,9,10\"}}]}";
        }
    }

}
