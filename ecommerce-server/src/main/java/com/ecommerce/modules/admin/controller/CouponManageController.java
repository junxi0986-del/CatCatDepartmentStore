package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.coupon.entity.Coupon;
import com.ecommerce.modules.coupon.mapper.CouponMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/admin/coupon")
public class CouponManageController {

    @Resource
    private CouponMapper couponMapper;

    @GetMapping("/list")
    public Result getCouponList(@RequestParam(required = false) String keyword) {
        List<Coupon> coupons;
        if (keyword != null && !keyword.isEmpty()) {
            coupons = couponMapper.selectByKeyword(keyword);
        } else {
            coupons = couponMapper.selectAll();
        }
        return Result.success(coupons);
    }

    @PostMapping("/add")
    public Result addCoupon(@RequestBody Coupon coupon) {
        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }
        int result = couponMapper.insert(coupon);
        if (result > 0) {
            return Result.success("添加成功");
        } else {
            return Result.error(500, "添加失败");
        }
    }

    @PostMapping("/update")
    public Result updateCoupon(@RequestBody Coupon coupon) {
        int result = couponMapper.update(coupon);
        if (result > 0) {
            return Result.success("更新成功");
        } else {
            return Result.error(500, "更新失败");
        }
    }

    @PostMapping("/delete")
    public Result deleteCoupon(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        int result = couponMapper.delete(id);
        if (result > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error(500, "删除失败");
        }
    }

    @PostMapping("/batchDelete")
    public Result batchDeleteCoupon(@RequestBody java.util.Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (java.util.List<Long>) params.get("ids");
        int result = couponMapper.batchDelete(ids);
        if (result > 0) {
            return Result.success("批量删除成功");
        } else {
            return Result.error(500, "批量删除失败");
        }
    }

    @PostMapping("/status")
    public Result updateCouponStatus(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        Integer status = Integer.parseInt(params.get("status").toString());
        int result = couponMapper.updateStatus(id, status);
        if (result > 0) {
            return Result.success("更新状态成功");
        } else {
            return Result.error(500, "更新状态失败");
        }
    }

    @GetMapping("/{id}")
    public Result getCouponById(@PathVariable Long id) {
        Coupon coupon = couponMapper.selectById(id);
        return Result.success(coupon);
    }

    @PostMapping("/use")
    public Result useCoupon(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        int result = couponMapper.incrementUsedCount(id);
        if (result > 0) {
            return Result.success("使用成功");
        } else {
            return Result.error(500, "使用失败，优惠券已用完或不存在");
        }
    }
}