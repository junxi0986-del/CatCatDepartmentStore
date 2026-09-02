package com.ecommerce.modules.coupon.mapper;

import com.ecommerce.modules.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface UserCouponMapper {

    @Select("SELECT * FROM user_coupon ORDER BY create_time DESC")
    List<UserCoupon> selectAll();

    @Select("SELECT * FROM user_coupon WHERE id = #{id}")
    UserCoupon selectById(Long id);

    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<UserCoupon> selectByUserId(Long userId);

    @Select("SELECT * FROM user_coupon WHERE coupon_id = #{couponId} ORDER BY create_time DESC")
    List<UserCoupon> selectByCouponId(Long couponId);

    @Select("SELECT * FROM user_coupon WHERE order_id = #{orderId}")
    List<UserCoupon> selectByOrderId(Long orderId);

    @Select("SELECT * FROM user_coupon WHERE status = #{status} ORDER BY create_time DESC")
    List<UserCoupon> selectByStatus(Integer status);

    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = #{status} ORDER BY create_time DESC")
    List<UserCoupon> selectByUserIdAndStatus(Long userId, Integer status);

    @Select("SELECT COUNT(*) as total FROM user_coupon")
    Map<String, Long> getTotalCouponCount();

    @Select("SELECT COUNT(*) as today FROM user_coupon WHERE DATE(create_time) = DATE(NOW())")
    Map<String, Long> getTodayCouponCount();

    @Select("SELECT status, COUNT(*) as count FROM user_coupon GROUP BY status")
    List<Map<String, Object>> getCouponStatusDistribution();

    @Select("SELECT c.name as coupon_name, COUNT(*) as count FROM user_coupon uc JOIN coupon c ON uc.coupon_id = c.id GROUP BY uc.coupon_id ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> getPopularCoupons();

    @Insert("INSERT INTO user_coupon (coupon_id, user_id, order_id, status, use_time, create_time) VALUES (#{couponId}, #{userId}, #{orderId}, #{status}, #{useTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserCoupon userCoupon);

    @Update("UPDATE user_coupon SET status = #{status}, use_time = #{useTime} WHERE id = #{id}")
    int updateStatus(Long id, Integer status, String useTime);

    @Update("UPDATE user_coupon SET order_id = #{orderId}, status = 1, use_time = NOW() WHERE id = #{id}")
    int useCoupon(Long id, Long orderId);
    
    @Update("UPDATE user_coupon SET status = 0, order_id = NULL, use_time = NULL WHERE id = #{id}")
    int revertCoupon(Long id);

    @Update("UPDATE user_coupon SET order_id = #{orderId} WHERE id = #{id}")
    int updateOrderId(Long id, Long orderId);

    @Delete("DELETE FROM user_coupon WHERE id = #{id}")
    int delete(Long id);

    @Delete("DELETE FROM user_coupon WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Select("SELECT uc.*, c.name as coupon_name, c.type, c.min_price as minAmount, c.discount, u.username FROM user_coupon uc LEFT JOIN coupon c ON uc.coupon_id = c.id LEFT JOIN user u ON uc.user_id = u.id ORDER BY uc.create_time DESC")
    List<Map<String, Object>> selectWithDetails();
    
    @Select("SELECT uc.*, c.name as coupon_name, c.type, c.min_price as minAmount, c.discount, c.start_time as startTime, c.end_time as endTime, u.username FROM user_coupon uc LEFT JOIN coupon c ON uc.coupon_id = c.id LEFT JOIN user u ON uc.user_id = u.id WHERE uc.user_id = #{userId} ORDER BY uc.create_time DESC")
    List<Map<String, Object>> selectByUserIdWithDetails(Long userId);
}