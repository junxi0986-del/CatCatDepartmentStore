package com.ecommerce.modules.recommend.controller;

import com.ecommerce.modules.recommend.service.RecommendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public String getRecommend() {
        try {
            List<?> data = recommendService.getRecommend();
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"code\":500,\"message\":\"获取推荐失败\",\"data\":[]}";
        }
    }

    @GetMapping("/{userId}")
    public String recommendForUser(@PathVariable Long userId) {
        try {
            List<?> data = recommendService.recommendForUser(userId, 10, 4);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"code\":500,\"message\":\"获取个性化推荐失败\",\"data\":[]}";
        }
    }

}