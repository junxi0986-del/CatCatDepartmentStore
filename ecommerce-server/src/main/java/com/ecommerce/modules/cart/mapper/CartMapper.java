package com.ecommerce.modules.cart.mapper;

import com.ecommerce.modules.cart.entity.Cart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
    // 根据userId查询购物车列表，关联商品信息
    @Select("SELECT c.*, p.name as productName, p.pic as productPic, p.price as productPrice FROM cart c LEFT JOIN product p ON c.product_id = p.id WHERE c.user_id = #{userId} ORDER BY c.create_time DESC")
    List<Cart> selectByUserId(Long userId);

    // 根据ID查询购物车商品
    @Select("SELECT * FROM cart WHERE id = #{id}")
    Cart selectById(Long id);

    // 根据userId和productId查询购物车商品
    @Select("SELECT * FROM cart WHERE user_id = #{userId} AND product_id = #{productId}")
    Cart selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    // 更新购物车商品数量
    @Update("UPDATE cart SET quantity = #{quantity}, update_time = NOW() WHERE id = #{id}")
    void updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    // 更新购物车商品选中状态
    @Update("UPDATE cart SET selected = #{selected}, update_time = NOW() WHERE id = #{id}")
    void updateSelected(@Param("id") Long id, @Param("selected") Integer selected);

    // 清空用户已选中的购物车商品
    @Update("DELETE FROM cart WHERE user_id = #{userId} AND selected = 1")
    void deleteSelectedByUserId(Long userId);
}
