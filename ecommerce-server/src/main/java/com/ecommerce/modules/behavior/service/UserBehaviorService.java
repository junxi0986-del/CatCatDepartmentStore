package com.ecommerce.modules.behavior.service;

public interface UserBehaviorService {
    void recordBehavior(Long userId, Long productId, Integer behaviorType, Integer stayTime);
}