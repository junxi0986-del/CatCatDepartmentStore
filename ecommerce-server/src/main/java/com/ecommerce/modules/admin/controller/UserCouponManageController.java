package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.coupon.entity.UserCoupon;
import com.ecommerce.modules.coupon.mapper.UserCouponMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/userCoupon")
public class UserCouponManageController {

    @Resource
    private UserCouponMapper userCouponMapper;

    @GetMapping("/list")
    public Result getCouponList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long couponId,
            @RequestParam(required = false) Integer status) {
        List<Map<String, Object>> coupons = userCouponMapper.selectWithDetails();
        // 在内存中进行过滤
        if (userId != null) {
            coupons.removeIf(coupon -> !userId.equals(coupon.get("user_id")));
        }
        if (couponId != null) {
            coupons.removeIf(coupon -> !couponId.equals(coupon.get("coupon_id")));
        }
        if (status != null) {
            coupons.removeIf(coupon -> !status.equals(coupon.get("status")));
        }
        return Result.success(coupons);
    }

    @GetMapping("/dashboard")
    public Result getDashboardData() {
        Map<String, Object> data = new java.util.HashMap<>();
        
        // 总优惠券领取数
        Map<String, Long> totalCount = userCouponMapper.getTotalCouponCount();
        data.put("totalCouponCount", totalCount.get("total"));
        
        // 今日优惠券领取数
        Map<String, Long> todayCount = userCouponMapper.getTodayCouponCount();
        data.put("todayCouponCount", todayCount.get("today"));
        
        // 优惠券状态分布
        List<Map<String, Object>> statusDistribution = userCouponMapper.getCouponStatusDistribution();
        data.put("couponStatusDistribution", statusDistribution);
        
        // 热门优惠券Top10
        List<Map<String, Object>> popularCoupons = userCouponMapper.getPopularCoupons();
        data.put("popularCoupons", popularCoupons);
        
        return Result.success(data);
    }

    @GetMapping("/user/{userId}")
    public Result getUserCoupons(@PathVariable Long userId) {
        List<UserCoupon> coupons = userCouponMapper.selectByUserId(userId);
        return Result.success(coupons);
    }

    @PostMapping("/add")
    public Result addCoupon(@RequestBody UserCoupon userCoupon) {
        int result = userCouponMapper.insert(userCoupon);
        if (result > 0) {
            return Result.success("添加成功");
        } else {
            return Result.error(500, "添加失败");
        }
    }

    @PostMapping("/use")
    public Result useCoupon(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        Long orderId = Long.parseLong(params.get("orderId").toString());
        int result = userCouponMapper.useCoupon(id, orderId);
        if (result > 0) {
            return Result.success("使用成功");
        } else {
            return Result.error(500, "使用失败");
        }
    }

    @PostMapping("/updateStatus")
    public Result updateCouponStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        Integer status = Integer.parseInt(params.get("status").toString());
        String useTime = params.get("useTime") != null ? params.get("useTime").toString() : null;
        int result = userCouponMapper.updateStatus(id, status, useTime);
        if (result > 0) {
            return Result.success("更新状态成功");
        } else {
            return Result.error(500, "更新状态失败");
        }
    }

    @PostMapping("/delete")
    public Result deleteCoupon(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        int result = userCouponMapper.delete(id);
        if (result > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error(500, "删除失败");
        }
    }

    @PostMapping("/batchDelete")
    public Result batchDeleteCoupon(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        int count = 0;
        for (Long id : ids) {
            count += userCouponMapper.delete(id);
        }
        if (count > 0) {
            return Result.success("批量删除成功");
        } else {
            return Result.error(500, "批量删除失败");
        }
    }
}