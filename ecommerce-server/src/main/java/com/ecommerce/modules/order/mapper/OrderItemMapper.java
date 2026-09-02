package com.ecommerce.modules.order.mapper;

import com.ecommerce.modules.order.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface OrderItemMapper {

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(Long orderId);

    @Select("SELECT * FROM order_item WHERE order_no = #{orderNo}")
    List<OrderItem> selectByOrderNo(String orderNo);

    @Select("SELECT * FROM order_item WHERE id = #{id}")
    OrderItem selectById(Long id);

    @Insert("INSERT INTO order_item (order_id, order_no, product_id, product_name, product_pic, spec_info, product_price, quantity, total_price, create_time) VALUES (#{orderId}, #{orderNo}, #{productId}, #{productName}, #{productPic}, #{specInfo}, #{productPrice}, #{quantity}, #{totalPrice}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderItem orderItem);

    @Select("SELECT * FROM order_item")
    List<OrderItem> selectAll();

    @Select("SELECT * FROM order_item WHERE product_name LIKE CONCAT('%', #{keyword}, '%') OR order_no LIKE CONCAT('%', #{keyword}, '%')")
    List<OrderItem> selectByKeyword(String keyword);

    @Delete("DELETE FROM order_item WHERE order_id = #{orderId}")
    int deleteByOrderId(Long orderId);

    @Update("UPDATE order_item SET order_id = #{orderId}, order_no = #{orderNo}, product_id = #{productId}, product_name = #{productName}, product_pic = #{productPic}, spec_info = #{specInfo}, product_price = #{productPrice}, quantity = #{quantity}, total_price = #{totalPrice} WHERE id = #{id}")
    int updateById(OrderItem orderItem);
}