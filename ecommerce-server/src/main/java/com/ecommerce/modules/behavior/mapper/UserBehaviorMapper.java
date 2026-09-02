package com.ecommerce.modules.behavior.mapper;

import com.ecommerce.modules.behavior.entity.UserBehavior;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface UserBehaviorMapper {

    @Select("SELECT * FROM user_behavior ORDER BY create_time DESC")
    List<UserBehavior> selectAll();

    @Select("SELECT * FROM user_behavior WHERE id = #{id}")
    UserBehavior selectById(Long id);

    @Select("SELECT * FROM user_behavior WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<UserBehavior> selectByUserId(Long userId);

    @Select("SELECT * FROM user_behavior WHERE product_id = #{productId} ORDER BY create_time DESC")
    List<UserBehavior> selectByProductId(Long productId);

    @Select("SELECT * FROM user_behavior WHERE behavior_type = #{behaviorType} ORDER BY create_time DESC")
    List<UserBehavior> selectByBehaviorType(Integer behaviorType);

    @Select("SELECT * FROM user_behavior WHERE user_id = #{userId} AND behavior_type = #{behaviorType} ORDER BY create_time DESC")
    List<UserBehavior> selectByUserIdAndType(Long userId, Integer behaviorType);

    @Select("SELECT * FROM user_behavior WHERE create_time >= #{startTime} AND create_time <= #{endTime} ORDER BY create_time DESC")
    List<UserBehavior> selectByTimeRange(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("SELECT COUNT(*) as total FROM user_behavior")
    Map<String, Long> getTotalBehaviorCount();

    @Select("SELECT COUNT(*) as today FROM user_behavior WHERE DATE(create_time) = DATE(NOW())")
    Map<String, Long> getTodayBehaviorCount();

    @Select("SELECT behavior_type, COUNT(*) as count FROM user_behavior GROUP BY behavior_type")
    List<Map<String, Object>> getBehaviorTypeDistribution();

    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM user_behavior WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getBehaviorTrend();

    @Select("SELECT p.id, p.name, p.pic, COUNT(*) as behavior_count FROM user_behavior ub JOIN product p ON ub.product_id = p.id GROUP BY ub.product_id ORDER BY behavior_count DESC LIMIT 10")
    List<Map<String, Object>> getHotProducts();

    @Select("SELECT ub.user_id, ub.product_id, SUM(CASE ub.behavior_type WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 3 WHEN 4 THEN 4 WHEN 5 THEN 5 ELSE 1 END) as score FROM user_behavior ub GROUP BY ub.user_id, ub.product_id")
    List<Map<String, Object>> getUserProductScores();

    @Select("SELECT DISTINCT user_id FROM user_behavior")
    List<Long> getAllUserIds();

    @Select("SELECT ub.product_id, SUM(CASE ub.behavior_type WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 3 WHEN 4 THEN 4 WHEN 5 THEN 5 ELSE 1 END) as score FROM user_behavior ub WHERE ub.user_id = #{userId} GROUP BY ub.user_id, ub.product_id")
    List<Map<String, Object>> getUserProductScoresByUserId(Long userId);

    @Select("SELECT p.* FROM product p WHERE p.sales > 0 ORDER BY p.sales DESC LIMIT #{limit}")
    List<Map<String, Object>> getHotProductsBySales(@Param("limit") int limit);

    @Insert("INSERT INTO user_behavior (user_id, product_id, behavior_type, stay_time, create_time) VALUES (#{userId}, #{productId}, #{behaviorType}, #{stayTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserBehavior userBehavior);

    @Delete("DELETE FROM user_behavior WHERE id = #{id}")
    int delete(Long id);

    @Delete("DELETE FROM user_behavior WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Delete("DELETE FROM user_behavior WHERE product_id = #{productId}")
    int deleteByProductId(Long productId);

    @Select("SELECT ub.*, u.username, p.name as product_name FROM user_behavior ub LEFT JOIN user u ON ub.user_id = u.id LEFT JOIN product p ON ub.product_id = p.id WHERE 1=1 ")
    List<Map<String, Object>> selectWithDetails();
}