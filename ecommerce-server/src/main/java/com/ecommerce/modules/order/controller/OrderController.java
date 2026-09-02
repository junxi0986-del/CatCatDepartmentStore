package com.ecommerce.modules.order.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.coupon.entity.Coupon;
import com.ecommerce.modules.coupon.entity.UserCoupon;
import com.ecommerce.modules.coupon.mapper.CouponMapper;
import com.ecommerce.modules.coupon.mapper.UserCouponMapper;
import com.ecommerce.modules.order.entity.Order;
import com.ecommerce.modules.order.entity.OrderItem;
import com.ecommerce.modules.order.mapper.OrderItemMapper;
import com.ecommerce.modules.order.mapper.OrderMapper;
import com.ecommerce.modules.product.mapper.ProductSpecMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private OrderItemMapper orderItemMapper;
    
    @Resource
    private CouponMapper couponMapper;
    
    @Resource
    private UserCouponMapper userCouponMapper;
    
    @Resource
    private ProductSpecMapper productSpecMapper;
    
    @Resource
    private com.ecommerce.modules.product.mapper.ProductMapper productMapper;

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);
        Double totalPrice = Double.parseDouble(params.get("totalPrice").toString());
        List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
        String receiver = (String) params.get("receiver");
        String receiverPhone = (String) params.get("receiverPhone");
        String receiverAddress = (String) params.get("receiverAddress");
        Long userCouponId = params.get("couponId") != null ? Long.parseLong(params.get("couponId").toString()) : null;

        // 计算实付金额
        Double payPrice = totalPrice;
        Long couponId = null;
        Double discountAmount = 0.0;

        if (userCouponId != null) {
            // 查询用户优惠券信息
            UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
            if (userCoupon == null || !userCoupon.getUserId().equals(userId) || userCoupon.getStatus() != 0) {
                return Result.fail("无效的优惠券");
            }

            // 查询优惠券详情
            couponId = userCoupon.getCouponId();
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null || coupon.getStatus() != 0) {
                return Result.fail("优惠券已失效");
            }

            // 检查优惠券有效期
            Date now = new Date();
            if (coupon.getStartTime() != null && now.before(coupon.getStartTime())) {
                return Result.fail("优惠券尚未生效");
            }
            if (coupon.getEndTime() != null && now.after(coupon.getEndTime())) {
                return Result.fail("优惠券已过期");
            }

            // 检查是否满足最低消费金额
            if (totalPrice >= coupon.getMinAmount()) {
                discountAmount = coupon.getDiscountAmount();
                payPrice = totalPrice - discountAmount;
            }
        }

        // 创建订单前先扣减库存
        for (Map<String, Object> item : items) {
            Long productId = Long.parseLong(item.get("productId").toString());
            Integer quantity = Integer.parseInt(item.get("quantity").toString());
            int stockResult = productMapper.decreaseStock(productId, quantity);
            if (stockResult == 0) {
                return Result.fail("库存不足，商品ID: " + productId);
            }
            
            if (item.get("specId") != null) {
                Long specId = Long.parseLong(item.get("specId").toString());
                int specStockResult = productSpecMapper.decreaseStock(specId, quantity);
                if (specStockResult == 0) {
                    return Result.fail("规格库存不足，规格ID: " + specId);
                }
            }
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo("ORDER" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUserId(userId);
        order.setTotalPrice(totalPrice);
        order.setPayPrice(payPrice);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(couponId);
        order.setUserCouponId(userCouponId);
        order.setReceiver(receiver);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setStatus(0); // 0: 待支付

        int result = orderMapper.insert(order);
        if (result > 0) {
            // 保存订单项
            for (Map<String, Object> item : items) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setOrderNo(order.getOrderNo());
                orderItem.setProductId(Long.parseLong(item.get("productId").toString()));
                orderItem.setProductName((String) item.get("productName"));
                orderItem.setProductPic((String) item.get("productPic"));
                orderItem.setSpecInfo(item.get("specInfo") != null ? (String) item.get("specInfo") : null);
                orderItem.setSpecId(item.get("specId") != null ? Long.parseLong(item.get("specId").toString()) : null);
                orderItem.setProductPrice(Double.parseDouble(item.get("productPrice").toString()));
                orderItem.setQuantity(Integer.parseInt(item.get("quantity").toString()));
                orderItem.setTotalPrice(Double.parseDouble(item.get("totalPrice").toString()));
                orderItemMapper.insert(orderItem);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", order.getId());
            data.put("orderNo", order.getOrderNo());
            data.put("totalPrice", totalPrice);
            data.put("payPrice", payPrice);
            data.put("discountAmount", discountAmount);
            return Result.success(data);
        } else {
            return Result.fail("创建订单失败");
        }
    }

    @GetMapping("/list")
    public Result getOrderList(@RequestParam(required = false) String status, 
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestHeader(value = "token", required = false) String token) {
        Long userId = getUserIdFromToken(token);

        List<Order> orderList;
        if (status != null && !status.isEmpty()) {
            if ("unpaid".equals(status)) {
                orderList = orderMapper.selectByUserIdAndStatus(userId, 0);
            } else if ("unshipped".equals(status)) {
                orderList = orderMapper.selectByUserIdAndStatus(userId, 2);
            } else if ("unreceived".equals(status)) {
                orderList = orderMapper.selectByUserIdAndStatus(userId, 3);
            } else if ("completed".equals(status)) {
                orderList = orderMapper.selectByUserIdAndStatus(userId, 4);
            } else if ("paid".equals(status)) {
                orderList = orderMapper.selectByUserIdAndStatus(userId, 1);
            } else if ("cancelled".equals(status)) {
                orderList = orderMapper.selectByUserIdAndStatus(userId, 5);
            } else if ("refunded".equals(status)) {
                orderList = orderMapper.selectByUserIdAndStatus(userId, 6);
            } else {
                orderList = orderMapper.selectByUserId(userId);
            }
        } else {
            orderList = orderMapper.selectByUserId(userId);
        }
        
        int total = orderList.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        
        List<Order> pagedList;
        if (fromIndex >= total) {
            pagedList = new java.util.ArrayList<>();
        } else {
            pagedList = orderList.subList(fromIndex, toIndex);
        }

        List<Map<String, Object>> orders = new java.util.ArrayList<>();
        for (Order order : pagedList) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("payPrice", order.getPayPrice());
            orderMap.put("discountAmount", order.getDiscountAmount());
            orderMap.put("status", order.getStatus());
            orderMap.put("createTime", order.getCreateTime());
            orderMap.put("receiver", order.getReceiver());
            orderMap.put("receiverPhone", order.getReceiverPhone());
            orderMap.put("receiverAddress", order.getReceiverAddress());
            orderMap.put("logisticsInfo", order.getLogisticsInfo());
            orderMap.put("expressNo", order.getExpressNo());
            orderMap.put("expressCompany", order.getExpressCompany());
            orderMap.put("payTime", order.getPayTime());
            orderMap.put("deliveryTime", order.getDeliveryTime());
            orderMap.put("receiveTime", order.getReceiveTime());

            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (OrderItem item : orderItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("productId", item.getProductId());
                itemMap.put("productName", item.getProductName());
                itemMap.put("productPic", item.getProductPic());
                itemMap.put("specInfo", item.getSpecInfo());
                itemMap.put("productPrice", item.getProductPrice());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("totalPrice", item.getTotalPrice());
                items.add(itemMap);
            }

            orderMap.put("items", items);
            orders.add(orderMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", orders);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result getOrderDetail(@PathVariable Long id, @RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);

        Order order = orderMapper.selectById(id);
        if (order != null && order.getUserId().equals(userId)) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", order.getId());
            data.put("orderNo", order.getOrderNo());
            data.put("totalPrice", order.getTotalPrice());
            data.put("payPrice", order.getPayPrice());
            data.put("discountAmount", order.getDiscountAmount());
            data.put("status", order.getStatus());
            data.put("createTime", order.getCreateTime());
            data.put("receiver", order.getReceiver());
            data.put("receiverPhone", order.getReceiverPhone());
            data.put("receiverAddress", order.getReceiverAddress());
            data.put("logisticsInfo", order.getLogisticsInfo());
            data.put("expressNo", order.getExpressNo());
            data.put("expressCompany", order.getExpressCompany());
            data.put("payTime", order.getPayTime());
            data.put("deliveryTime", order.getDeliveryTime());
            data.put("receiveTime", order.getReceiveTime());
            data.put("refundStatus", order.getRefundStatus());
            data.put("refundApplyTime", order.getRefundApplyTime());
            data.put("refundReason", order.getRefundReason());
            data.put("refundReply", order.getRefundReply());

            // 获取优惠券信息
            if (order.getCouponId() != null) {
                Coupon coupon = couponMapper.selectById(order.getCouponId());
                if (coupon != null) {
                    Map<String, Object> couponInfo = new HashMap<>();
                    couponInfo.put("id", coupon.getId());
                    couponInfo.put("name", coupon.getName());
                    couponInfo.put("type", coupon.getType());
                    couponInfo.put("minAmount", coupon.getMinAmount());
                    couponInfo.put("discountAmount", coupon.getDiscountAmount());
                    couponInfo.put("status", coupon.getStatus());
                    data.put("coupon", couponInfo);
                }
            }

            // 获取真实的商品列表
            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (OrderItem item : orderItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("productId", item.getProductId());
                itemMap.put("productName", item.getProductName());
                itemMap.put("productPic", item.getProductPic());
                itemMap.put("specInfo", item.getSpecInfo());
                itemMap.put("productPrice", item.getProductPrice());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("totalPrice", item.getTotalPrice());
                items.add(itemMap);
            }

            data.put("items", items);
            return Result.success(data);
        } else {
            return Result.fail("订单不存在");
        }
    }

    @PostMapping("/pay")
    public Result payOrder(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        Long userId = getUserIdFromToken(token);

        Order order = orderMapper.selectById(orderId);
        if (order != null && order.getUserId().equals(userId) && order.getStatus() == 0) {
            order.setStatus(1);
            orderMapper.updateStatusAndPayTime(orderId, 1, new Date());
            
            if (order.getUserCouponId() != null) {
                userCouponMapper.useCoupon(order.getUserCouponId(), orderId);
            }
            
            return Result.success("支付成功");
        } else {
            return Result.fail("支付失败");
        }
    }

    @PostMapping("/confirm")
    public Result confirmReceipt(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);

        Order order = orderMapper.selectById(orderId);
        if (order != null && order.getUserId().equals(userId) && order.getStatus() == 3) {
            order.setStatus(4); // 4: 已完成
            // 更新数据库
            orderMapper.updateStatus(orderId, 4);
            orderMapper.updateReceiveTime(orderId, new Date());
            return Result.success("确认收货成功");
        } else {
            return Result.fail("确认收货失败");
        }
    }

    @PostMapping("/cancel")
    public Result cancelOrder(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);

        Order order = orderMapper.selectById(orderId);
        if (order != null && order.getUserId().equals(userId) && order.getStatus() == 0) {
            // 获取订单项，恢复库存
            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
            for (OrderItem item : orderItems) {
                productMapper.increaseStock(item.getProductId(), item.getQuantity());
            }
            
            // 如果订单使用了优惠券，恢复优惠券状态
            if (order.getUserCouponId() != null) {
                userCouponMapper.revertCoupon(order.getUserCouponId());
            }
            
            order.setStatus(5); // 5: 已取消
            // 更新数据库
            orderMapper.updateStatus(orderId, 5);
            return Result.success("取消订单成功");
        } else {
            return Result.fail("取消订单失败");
        }
    }

    @PostMapping("/refund/apply")
    public Result applyRefund(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        String refundReason = (String) params.get("refundReason");
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权操作此订单");
        }
        if (order.getStatus() < 1 || order.getStatus() > 4) {
            return Result.fail("只有已支付、待发货、待收货、已完成的订单才能申请退款");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == 1) {
            return Result.fail("该订单已在退款处理中");
        }

        // 设置退款申请信息
        order.setRefundStatus(1); // 1: 申请退款
        order.setRefundApplyTime(new Date());
        order.setRefundReason(refundReason);
        orderMapper.updateRefundInfo(orderId, 1, new Date(), refundReason, null);
        return Result.success("退款申请已提交，请等待管理员处理");
    }

    @GetMapping("/refund/list")
    public Result getRefundList(@RequestHeader(value = "token", required = false) String token) {
        // 获取所有申请退款的订单
        List<Order> refundOrders = orderMapper.selectByRefundStatus(1);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Order order : refundOrders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("payPrice", order.getPayPrice());
            orderMap.put("discountAmount", order.getDiscountAmount());
            orderMap.put("status", order.getStatus());
            orderMap.put("refundStatus", order.getRefundStatus());
            orderMap.put("refundApplyTime", order.getRefundApplyTime());
            orderMap.put("refundReason", order.getRefundReason());
            orderMap.put("receiver", order.getReceiver());
            orderMap.put("receiverPhone", order.getReceiverPhone());
            orderMap.put("receiverAddress", order.getReceiverAddress());
            orderMap.put("createTime", order.getCreateTime());
            orderMap.put("userId", order.getUserId());

            // 获取用户名
            orderMap.put("username", getUsernameByUserId(order.getUserId()));

            // 获取商品列表
            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (OrderItem item : orderItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("productId", item.getProductId());
                itemMap.put("productName", item.getProductName());
                itemMap.put("productPic", item.getProductPic());
                itemMap.put("productPrice", item.getProductPrice());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("totalPrice", item.getTotalPrice());
                items.add(itemMap);
            }
            orderMap.put("items", items);
            result.add(orderMap);
        }
        return Result.success(result);
    }

    @PostMapping("/refund/handle")
    public Result handleRefund(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        Integer action = Integer.parseInt(params.get("action").toString()); // 1: 同意, 2: 拒绝
        String reply = (String) params.get("reply");

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getRefundStatus() != 1) {
            return Result.fail("该订单没有待处理的退款申请");
        }

        if (action == 1) {
            // 同意退款，更新订单状态和退款状态
            orderMapper.updateRefundStatus(orderId, 2, reply); // 2: 退款成功
            orderMapper.updateStatus(orderId, 6); // 6: 已退款
            
            // 如果订单使用了优惠券，恢复优惠券
            if (order.getUserCouponId() != null) {
                // 直接使用订单关联的用户优惠券ID恢复
                userCouponMapper.updateStatus(order.getUserCouponId(), 0, null);
                userCouponMapper.updateOrderId(order.getUserCouponId(), null);
            }
            
            return Result.success("已同意退款，退款金额：" + order.getPayPrice());
        } else if (action == 2) {
            // 拒绝退款
            orderMapper.updateRefundStatus(orderId, 3, reply); // 3: 退款拒绝
            return Result.success("已拒绝退款申请");
        } else {
            return Result.fail("无效的操作");
        }
    }

    @PostMapping("/address/update")
    public Result updateAddress(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        String receiver = (String) params.get("receiver");
        String receiverPhone = (String) params.get("receiverPhone");
        String receiverAddress = (String) params.get("receiverAddress");
        
        Long userId = getUserIdFromToken(token);
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权操作此订单");
        }
        if (order.getStatus() > 2) {
            return Result.fail("订单已发货，无法更换地址");
        }
        
        int result = orderMapper.updateAddress(orderId, receiver, receiverPhone, receiverAddress);
        if (result > 0) {
            return Result.success("地址修改成功");
        } else {
            return Result.fail("地址修改失败");
        }
    }

    private String getUsernameByUserId(Long userId) {
        // 这里应该调用userMapper查询用户名，暂时返回固定值
        return "用户" + userId;
    }



    // 从token中获取用户ID，这里暂时从token中提取用户ID
    // 实际项目中应该使用JWT解析token获取用户ID
    private Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return 1L;
        }
        
        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length >= 2) {
                String payload = new String(java.util.Base64.getDecoder().decode(tokenParts[1]));
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> tokenData = objectMapper.readValue(payload, java.util.Map.class);
                if (tokenData.containsKey("userId")) {
                    return Long.valueOf(tokenData.get("userId").toString());
                }
            }
        } catch (Exception e) {
        }
        
        if (token.contains("_")) {
            try {
                String[] parts = token.split("_");
                return Long.parseLong(parts[1]);
            } catch (Exception e) {
            }
        }
        
        return 1L;
    }
}
