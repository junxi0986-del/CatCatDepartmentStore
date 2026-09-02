package com.ecommerce.modules.behavior.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.behavior.service.UserBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/behavior")
public class UserBehaviorController {

    private static final Logger log = LoggerFactory.getLogger(UserBehaviorController.class);

    @Resource
    private UserBehaviorService userBehaviorService;

    @PostMapping("/record")
    public Result recordBehavior(@RequestBody Map<String, Object> params) {
        log.info("===== 收到用户行为记录请求 =====");
        log.info("请求参数: {}", params);

        try {
            Long userId = params.get("userId") != null ? Long.parseLong(params.get("userId").toString()) : null;
            Long productId = params.get("productId") != null ? Long.parseLong(params.get("productId").toString()) : null;
            Integer behaviorType = params.get("behaviorType") != null ? Integer.parseInt(params.get("behaviorType").toString()) : null;
            Integer stayTime = params.get("stayTime") != null ? Integer.parseInt(params.get("stayTime").toString()) : 0;

            if (userId == null || behaviorType == null) {
                log.warn("参数校验失败: userId={}, behaviorType={}", userId, behaviorType);
                return Result.error(400, "userId和behaviorType不能为空");
            }

            String behaviorName = switch (behaviorType) {
                case 1 -> "浏览";
                case 2 -> "搜索";
                case 3 -> "加购";
                case 5 -> "购买";
                default -> "未知";
            };
            log.info("记录行为: userId={}, productId={}, behaviorType={}({}), stayTime={}",
                    userId, productId, behaviorType, behaviorName, stayTime);

            userBehaviorService.recordBehavior(userId, productId, behaviorType, stayTime);
            log.info("行为记录成功: userId={}, type={}", userId, behaviorName);
            return Result.success("行为记录成功");
        } catch (Exception e) {
            log.error("记录行为失败", e);
            return Result.error(500, "记录行为失败");
        }
    }
}