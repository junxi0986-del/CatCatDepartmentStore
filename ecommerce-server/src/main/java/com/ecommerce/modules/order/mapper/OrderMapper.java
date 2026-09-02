package com.ecommerce.modules.order.mapper;

import com.ecommerce.modules.order.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

public interface OrderMapper {

    @Select("SELECT * FROM `order_info` WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> selectByUserId(Long userId);

    @Select("SELECT * FROM `order_info` WHERE id = #{id}")
    Order selectById(Long id);
    
    @Select("SELECT * FROM `order_info` WHERE order_no = #{orderNo}")
    Order selectByOrderNo(String orderNo);

    @Insert("INSERT INTO `order_info` (order_no, user_id, total_price, pay_price, receiver, receiver_phone, receiver_address, status, coupon_id, user_coupon_id, create_time, update_time) VALUES (#{orderNo}, #{userId}, #{totalPrice}, #{payPrice}, #{receiver}, #{receiverPhone}, #{receiverAddress}, #{status}, #{couponId}, #{userCouponId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("SELECT * FROM `order_info` WHERE user_id = #{userId} AND status = #{status} ORDER BY create_time DESC")
    List<Order> selectByUserIdAndStatus(Long userId, Integer status);
    
    @Select("SELECT * FROM `order_info` WHERE user_id = #{userId} AND pay_time IS NOT NULL ORDER BY create_time DESC")
    List<Order> selectPaidOrders(Long userId);
    
    @Select("SELECT * FROM `order_info` ORDER BY create_time DESC")
    List<Order> selectAll();
    
    @Select("SELECT * FROM `order_info` WHERE order_no LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<Order> selectByKeyword(String keyword);
    
    @Select("<script>" +
            "SELECT DISTINCT o.* FROM `order_info` o " +
            "LEFT JOIN `order_item` oi ON o.id = oi.order_id " +
            "WHERE 1=1 " +
            "<if test='orderNo != null and orderNo != \"\"'>" +
            "AND o.order_no LIKE CONCAT('%', #{orderNo}, '%') " +
            "</if>" +
            "<if test='receiver != null and receiver != \"\"'>" +
            "AND o.receiver LIKE CONCAT('%', #{receiver}, '%') " +
            "</if>" +
            "<if test='receiverPhone != null and receiverPhone != \"\"'>" +
            "AND o.receiver_phone LIKE CONCAT('%', #{receiverPhone}, '%') " +
            "</if>" +
            "<if test='productName != null and productName != \"\"'>" +
            "AND oi.product_name LIKE CONCAT('%', #{productName}, '%') " +
            "</if>" +
            "<if test='minPrice != null'>" +
            "AND o.total_price >= #{minPrice} " +
            "</if>" +
            "<if test='maxPrice != null'>" +
            "AND o.total_price &lt;= #{maxPrice} " +
            "</if>" +
            "<if test='status != null'>" +
            "AND o.status = #{status} " +
            "</if>" +
            "ORDER BY o.create_time DESC" +
            "</script>")
    List<Order> searchOrders(@Param("orderNo") String orderNo, 
                             @Param("receiver") String receiver,
                             @Param("receiverPhone") String receiverPhone,
                             @Param("productName") String productName,
                             @Param("minPrice") Double minPrice,
                             @Param("maxPrice") Double maxPrice,
                             @Param("status") Integer status);
    
    @Update("UPDATE `order_info` SET order_no = #{orderNo}, user_id = #{userId}, total_price = #{totalPrice}, pay_price = #{payPrice}, receiver = #{receiver}, receiver_phone = #{receiverPhone}, receiver_address = #{receiverAddress}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(Order order);
    
    @Delete("DELETE FROM `order_info` WHERE id = #{id}")
    int delete(Long id);
    
    @Delete("DELETE FROM `order_info` WHERE id IN (${ids})")
    int batchDelete(@Param("ids") List<Long> ids);
    
    @Update("UPDATE `order_info` SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(Long id, Integer status);
    
    @Update("UPDATE `order_info` SET status = #{status}, pay_time = #{payTime}, update_time = NOW() WHERE id = #{id}")
    int updateStatusAndPayTime(Long id, Integer status, Date payTime);
    
    @Update("UPDATE `order_info` SET logistics_info = #{logisticsInfo}, update_time = NOW() WHERE id = #{id}")
    int updateLogisticsInfo(Long id, String logisticsInfo);
    
    @Update("UPDATE `order_info` SET express_no = #{expressNo}, express_company = #{expressCompany}, update_time = NOW() WHERE id = #{id}")
    int updateExpressInfo(Long id, String expressNo, String expressCompany);
    
    @Update("UPDATE `order_info` SET delivery_time = #{deliveryTime}, update_time = NOW() WHERE id = #{id}")
    int updateDeliveryTime(Long id, Date deliveryTime);
    
    @Update("UPDATE `order_info` SET receive_time = #{receiveTime}, update_time = NOW() WHERE id = #{id}")
    int updateReceiveTime(Long id, Date receiveTime);

    @Select("SELECT * FROM `order_info` WHERE refund_status = #{refundStatus} ORDER BY refund_apply_time DESC")
    List<Order> selectByRefundStatus(Integer refundStatus);

    @Update("UPDATE `order_info` SET refund_status = #{refundStatus}, refund_apply_time = #{refundApplyTime}, refund_reason = #{refundReason}, refund_reply = #{refundReply}, update_time = NOW() WHERE id = #{id}")
    int updateRefundInfo(Long id, Integer refundStatus, Date refundApplyTime, String refundReason, String refundReply);

    @Update("UPDATE `order_info` SET refund_status = #{refundStatus}, refund_reply = #{refundReply}, update_time = NOW() WHERE id = #{id}")
    int updateRefundStatus(Long id, Integer refundStatus, String refundReply);

    @Select("SELECT COALESCE(SUM(pay_price), 0) FROM `order_info`")
    Double sumOrderAmount();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') as month, SUM(pay_price) as amount FROM `order_info` WHERE status >= 1 GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month DESC LIMIT 12")
    List<java.util.Map<String, Object>> getMonthlySales();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as orderCount, SUM(pay_price) as amount FROM `order_info` WHERE status >= 1 AND DATE_FORMAT(create_time, '%Y-%m') = #{month} GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY date ASC")
    List<java.util.Map<String, Object>> getDailySalesByMonth(String month);

    @Update("UPDATE `order_info` SET receiver = #{receiver}, receiver_phone = #{receiverPhone}, receiver_address = #{receiverAddress}, update_time = NOW() WHERE id = #{id}")
    int updateAddress(Long id, String receiver, String receiverPhone, String receiverAddress);
}