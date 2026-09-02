package com.ecommerce.modules.review.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.review.entity.ProductReview;
import com.ecommerce.modules.review.mapper.ProductReviewMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/review")
public class ReviewController {

    @Resource
    private ProductReviewMapper reviewMapper;

    @PostMapping("/add")
    public Result addReviews(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        Long userId = getUserIdFromToken(token);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reviews = (List<Map<String, Object>>) params.get("reviews");
        
        for (Map<String, Object> review : reviews) {
            ProductReview productReview = new ProductReview();
            productReview.setOrderId(orderId);
            productReview.setProductId(Long.parseLong(review.get("productId").toString()));
            productReview.setUserId(userId);
            productReview.setRating(review.get("rating") != null ? Integer.parseInt(review.get("rating").toString()) : 5);
            productReview.setContent(review.get("content") != null ? review.get("content").toString() : "");
            productReview.setCreateTime(new Date());
            reviewMapper.insert(productReview);
        }
        
        return Result.success("评价成功");
    }

    @GetMapping("/list/{productId}")
    public Result getReviews(@PathVariable Long productId) {
        List<ProductReview> reviews = reviewMapper.selectByProductId(productId);
        return Result.success(reviews);
    }

    @GetMapping("/check/{orderId}")
    public Result checkReviewed(@PathVariable Long orderId) {
        int count = reviewMapper.countByOrderId(orderId);
        return Result.success(count > 0);
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.contains("_")) {
            try {
                String[] parts = token.split("_");
                return Long.parseLong(parts[1]);
            } catch (Exception e) {
                return 1L;
            }
        }
        return 1L;
    }
}
