package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.behavior.entity.UserBehavior;
import com.ecommerce.modules.behavior.mapper.UserBehaviorMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/behavior")
public class UserBehaviorManageController {

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @GetMapping("/list")
    public Result getBehaviorList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer behaviorType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        List<Map<String, Object>> behaviors = userBehaviorMapper.selectWithDetails();
        // 在内存中进行过滤
        if (userId != null) {
            behaviors.removeIf(behavior -> !userId.equals(behavior.get("user_id")));
        }
        if (productId != null) {
            behaviors.removeIf(behavior -> !productId.equals(behavior.get("product_id")));
        }
        if (behaviorType != null) {
            behaviors.removeIf(behavior -> !behaviorType.equals(behavior.get("behavior_type")));
        }
        if (startTime != null) {
            behaviors.removeIf(behavior -> {
                String createTime = behavior.get("create_time").toString();
                return createTime.compareTo(startTime) < 0;
            });
        }
        if (endTime != null) {
            behaviors.removeIf(behavior -> {
                String createTime = behavior.get("create_time").toString();
                return createTime.compareTo(endTime) > 0;
            });
        }
        return Result.success(behaviors);
    }

    @GetMapping("/dashboard")
    public Result getDashboardData() {
        Map<String, Object> data = new java.util.HashMap<>();
        
        // 总用户行为数
        Map<String, Long> totalCount = userBehaviorMapper.getTotalBehaviorCount();
        data.put("totalBehaviorCount", totalCount.get("total"));
        
        // 今日行为数
        Map<String, Long> todayCount = userBehaviorMapper.getTodayBehaviorCount();
        data.put("todayBehaviorCount", todayCount.get("today"));
        
        // 各行为类型占比
        List<Map<String, Object>> typeDistribution = userBehaviorMapper.getBehaviorTypeDistribution();
        data.put("behaviorTypeDistribution", typeDistribution);
        
        // 行为趋势
        List<Map<String, Object>> trend = userBehaviorMapper.getBehaviorTrend();
        data.put("behaviorTrend", trend);
        
        // 热门商品Top10
        List<Map<String, Object>> hotProducts = userBehaviorMapper.getHotProducts();
        data.put("hotProducts", hotProducts);
        
        return Result.success(data);
    }

    @GetMapping("/user/{userId}")
    public Result getUserBehavior(@PathVariable Long userId) {
        List<UserBehavior> behaviors = userBehaviorMapper.selectByUserId(userId);
        return Result.success(behaviors);
    }

    @GetMapping("/product/{productId}")
    public Result getProductBehavior(@PathVariable Long productId) {
        List<UserBehavior> behaviors = userBehaviorMapper.selectByProductId(productId);
        return Result.success(behaviors);
    }

    @PostMapping("/add")
    public Result addBehavior(@RequestBody UserBehavior userBehavior) {
        int result = userBehaviorMapper.insert(userBehavior);
        if (result > 0) {
            return Result.success("添加成功");
        } else {
            return Result.error(500, "添加失败");
        }
    }

    @PostMapping("/delete")
    public Result deleteBehavior(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        int result = userBehaviorMapper.delete(id);
        if (result > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error(500, "删除失败");
        }
    }

    @PostMapping("/batchDelete")
    public Result batchDeleteBehavior(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        int count = 0;
        for (Long id : ids) {
            count += userBehaviorMapper.delete(id);
        }
        if (count > 0) {
            return Result.success("批量删除成功");
        } else {
            return Result.error(500, "批量删除失败");
        }
    }

    @GetMapping("/analysis/{userId}")
    public Result analyzeUserBehavior(@PathVariable Long userId) {
        Map<String, Object> data = new java.util.HashMap<>();
        
        // 用户所有行为记录
        List<UserBehavior> behaviors = userBehaviorMapper.selectByUserId(userId);
        data.put("behaviors", behaviors);
        
        // 各行为类型统计
        Map<Integer, Integer> behaviorTypeCount = new java.util.HashMap<>();
        for (UserBehavior behavior : behaviors) {
            behaviorTypeCount.put(behavior.getBehaviorType(), behaviorTypeCount.getOrDefault(behavior.getBehaviorType(), 0) + 1);
        }
        data.put("behaviorTypeCount", behaviorTypeCount);
        
        // 兴趣标签（简单示例，实际可以更复杂）
        List<String> interestTags = new java.util.ArrayList<>();
        if (behaviorTypeCount.getOrDefault(4, 0) > 5) {
            interestTags.add("购物达人");
        }
        if (behaviorTypeCount.getOrDefault(2, 0) > 10) {
            interestTags.add("搜索活跃");
        }
        if (behaviorTypeCount.getOrDefault(1, 0) > 20) {
            interestTags.add("浏览活跃");
        }
        data.put("interestTags", interestTags);
        
        return Result.success(data);
    }
}