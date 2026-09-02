package com.ecommerce.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.modules.review.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {
    
    @Select("SELECT * FROM product_review WHERE product_id = #{productId} ORDER BY create_time DESC")
    List<ProductReview> selectByProductId(Long productId);
    
    @Select("SELECT COUNT(*) FROM product_review WHERE order_id = #{orderId}")
    int countByOrderId(Long orderId);
}
