package com.ecommerce.modules.coupon.mapper;

import com.ecommerce.modules.coupon.entity.Coupon;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CouponMapper {

    @Select("SELECT id, name, type, min_price as minAmount, discount as discountAmount, total as totalCount, used as usedCount, status, start_time as startTime, end_time as endTime, create_time as createTime FROM coupon ORDER BY create_time DESC")
    List<Coupon> selectAll();

    @Select("SELECT id, name, type, min_price as minAmount, discount as discountAmount, total as totalCount, used as usedCount, status, start_time as startTime, end_time as endTime, create_time as createTime FROM coupon WHERE id = #{id}")
    Coupon selectById(Long id);

    @Select("SELECT id, name, type, min_price as minAmount, discount as discountAmount, total as totalCount, used as usedCount, status, start_time as startTime, end_time as endTime, create_time as createTime FROM coupon WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<Coupon> selectByKeyword(String keyword);

    @Insert("INSERT INTO coupon (name, type, min_price, discount, total, used, status, start_time, end_time, create_time) VALUES (#{name}, #{type}, #{minAmount}, #{discountAmount}, #{totalCount}, #{usedCount}, #{status}, #{startTime}, #{endTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Coupon coupon);

    @Update("UPDATE coupon SET name = #{name}, type = #{type}, min_price = #{minAmount}, discount = #{discountAmount}, total = #{totalCount}, used = #{usedCount}, status = #{status}, start_time = #{startTime}, end_time = #{endTime} WHERE id = #{id}")
    int update(Coupon coupon);

    @Delete("DELETE FROM coupon WHERE id = #{id}")
    int delete(Long id);

    @Delete("DELETE FROM coupon WHERE id IN (${ids})")
    int batchDelete(@Param("ids") List<Long> ids);

    @Update("UPDATE coupon SET status = #{status} WHERE id = #{id}")
    int updateStatus(Long id, Integer status);

    @Update("UPDATE coupon SET used = used + 1 WHERE id = #{id} AND used < total")
    int incrementUsedCount(Long id);
}