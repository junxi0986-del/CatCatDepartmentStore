package com.ecommerce.modules.recommend.service;

import java.util.List;
import java.util.Map;

public interface RecommendService {
    List<Map<String, Object>> getRecommend();

    List<Map<String, Object>> recommendForUser(Long userId, int topK, int recommendCount);

    List<Map<String, Object>> getHotProducts(int limit);
}
