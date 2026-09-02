package com.ecommerce.modules.recommend.service.impl;

import com.ecommerce.modules.behavior.mapper.UserBehaviorMapper;
import com.ecommerce.modules.product.entity.Product;
import com.ecommerce.modules.product.mapper.ProductMapper;
import com.ecommerce.modules.recommend.service.RecommendService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Override
    public List<Map<String, Object>> getRecommend() {
        List<Product> products = productMapper.selectAll();
        List<Map<String, Object>> result = new ArrayList<>();

        int limit = Math.min(products.size(), 8);

        for (int i = 0; i < limit; i++) {
            Product product = products.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("name", product.getName());
            item.put("price", product.getPrice());
            item.put("pic", product.getPic());
            item.put("image", product.getPic());
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> recommendForUser(Long userId, int topK, int recommendCount) {
        List<Map<String, Object>> userProductScores = userBehaviorMapper.getUserProductScores();

        if (userProductScores == null || userProductScores.isEmpty()) {
            return getHotProducts(recommendCount);
        }

        Map<Long, Map<Long, Double>> userRatingMatrix = buildUserRatingMatrix(userProductScores);

        Map<Long, Double> targetUserRatings = userRatingMatrix.get(userId);
        if (targetUserRatings == null || targetUserRatings.isEmpty()) {
            return getHotProducts(recommendCount);
        }

        Map<Long, Double> userSimilarities = new HashMap<>();
        for (Long otherUserId : userRatingMatrix.keySet()) {
            if (otherUserId.equals(userId)) {
                continue;
            }
            double similarity = cosineSimilarity(targetUserRatings, userRatingMatrix.get(otherUserId));
            if (similarity > 0) {
                userSimilarities.put(otherUserId, similarity);
            }
        }

        List<Map.Entry<Long, Double>> sortedSimilarUsers = userSimilarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toList());

        if (sortedSimilarUsers.isEmpty()) {
            return getHotProducts(recommendCount);
        }

        Map<Long, Double> predictedScores = new HashMap<>();
        for (Map.Entry<Long, Double> similarUser : sortedSimilarUsers) {
            Long similarUserId = similarUser.getKey();
            double similarity = similarUser.getValue();
            Map<Long, Double> similarUserRatings = userRatingMatrix.get(similarUserId);

            for (Map.Entry<Long, Double> entry : similarUserRatings.entrySet()) {
                Long productId = entry.getKey();
                if (targetUserRatings.containsKey(productId)) {
                    continue;
                }
                double score = entry.getValue();
                predictedScores.merge(productId, similarity * score, Double::sum);
            }
        }

        if (predictedScores.isEmpty()) {
            return getHotProducts(recommendCount);
        }

        List<Long> recommendedProductIds = predictedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(recommendCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Product> products = productMapper.selectBatchIds(recommendedProductIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long productId : recommendedProductIds) {
            Product product = productMap.get(productId);
            if (product != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", product.getId());
                item.put("name", product.getName());
                item.put("price", product.getPrice());
                item.put("pic", product.getPic());
                item.put("image", product.getPic());
                result.add(item);
            }
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getHotProducts(int limit) {
        List<Map<String, Object>> hotProducts = userBehaviorMapper.getHotProductsBySales(limit);

        if (hotProducts == null || hotProducts.isEmpty()) {
            List<Product> products = productMapper.selectAll();
            int size = Math.min(products.size(), limit);
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Product product = products.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("id", product.getId());
                item.put("name", product.getName());
                item.put("price", product.getPrice());
                item.put("pic", product.getPic());
                item.put("image", product.getPic());
                result.add(item);
            }
            return result;
        }

        return hotProducts.stream().map(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.get("id"));
            item.put("name", row.get("name"));
            item.put("price", row.get("price"));
            item.put("pic", row.get("pic"));
            item.put("image", row.get("pic"));
            return item;
        }).collect(Collectors.toList());
    }

    private Map<Long, Map<Long, Double>> buildUserRatingMatrix(List<Map<String, Object>> userProductScores) {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        for (Map<String, Object> row : userProductScores) {
            Long userId = ((Number) row.get("user_id")).longValue();
            Long productId = ((Number) row.get("product_id")).longValue();
            Double score = ((Number) row.get("score")).doubleValue();
            matrix.computeIfAbsent(userId, k -> new HashMap<>()).put(productId, score);
        }
        return matrix;
    }

    private double cosineSimilarity(Map<Long, Double> ratingsA, Map<Long, Double> ratingsB) {
        if (ratingsA == null || ratingsB == null || ratingsA.isEmpty() || ratingsB.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<Long, Double> entry : ratingsA.entrySet()) {
            double a = entry.getValue();
            normA += a * a;
            Double b = ratingsB.get(entry.getKey());
            if (b != null) {
                dotProduct += a * b;
            }
        }

        for (double b : ratingsB.values()) {
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

}