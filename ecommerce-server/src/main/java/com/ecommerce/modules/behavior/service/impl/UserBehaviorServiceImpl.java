package com.ecommerce.modules.behavior.service.impl;

import com.ecommerce.modules.behavior.entity.UserBehavior;
import com.ecommerce.modules.behavior.mapper.UserBehaviorMapper;
import com.ecommerce.modules.behavior.service.UserBehaviorService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class UserBehaviorServiceImpl implements UserBehaviorService {

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Override
    public void recordBehavior(Long userId, Long productId, Integer behaviorType, Integer stayTime) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setProductId(productId);
        behavior.setBehaviorType(behaviorType);
        behavior.setStayTime(stayTime != null ? stayTime : 0);
        userBehaviorMapper.insert(behavior);
    }
}