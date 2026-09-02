package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.order.entity.Order;
import com.ecommerce.modules.order.entity.OrderItem;
import com.ecommerce.modules.order.mapper.OrderItemMapper;
import com.ecommerce.modules.order.mapper.OrderMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/order")
public class OrderManageController {

    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private OrderItemMapper orderItemMapper;
    
    @Resource
    private com.ecommerce.modules.product.mapper.ProductMapper productMapper;

    @GetMapping("/list")
    public Result getOrderList(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String orderNo,
                               @RequestParam(required = false) String receiver,
                               @RequestParam(required = false) String receiverPhone,
                               @RequestParam(required = false) String productName,
                               @RequestParam(required = false) Double minPrice,
                               @RequestParam(required = false) Double maxPrice,
                               @RequestParam(required = false) Integer status) {
        List<Order> orders;
        
        if (orderNo != null || receiver != null || receiverPhone != null || 
            productName != null || minPrice != null || maxPrice != null || status != null) {
            orders = orderMapper.searchOrders(orderNo, receiver, receiverPhone, productName, minPrice, maxPrice, status);
        } else if (keyword != null && !keyword.isEmpty()) {
            orders = orderMapper.selectByKeyword(keyword);
        } else {
            orders = orderMapper.selectAll();
        }
        return Result.success(orders);
    }

    @PostMapping("/add")
    public Result addOrder(@RequestBody Order order) {
        int result = orderMapper.insert(order);
        if (result > 0) {
            return Result.success("添加成功");
        } else {
            return Result.error(500, "添加失败");
        }
    }

    @PostMapping("/update")
    public Result updateOrder(@RequestBody Order order) {
        int result = orderMapper.update(order);
        if (result > 0) {
            return Result.success("更新成功");
        } else {
            return Result.error(500, "更新失败");
        }
    }

    @PostMapping("/delete")
    public Result deleteOrder(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        // 先删除关联的订单项
        orderItemMapper.deleteByOrderId(id);
        // 再删除订单
        int result = orderMapper.delete(id);
        if (result > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error(500, "删除失败");
        }
    }

    @PostMapping("/batchDelete")
    public Result batchDeleteOrder(@RequestBody java.util.Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (java.util.List<Long>) params.get("ids");
        // 先删除关联的订单项
        for (Long id : ids) {
            orderItemMapper.deleteByOrderId(id);
        }
        // 再删除订单
        int result = orderMapper.batchDelete(ids);
        if (result > 0) {
            return Result.success("批量删除成功");
        } else {
            return Result.error(500, "批量删除失败");
        }
    }

    @PostMapping("/status")
    public Result updateOrderStatus(@RequestBody Order order) {
        int result = orderMapper.updateStatus(order.getId(), order.getStatus());
        if (result > 0) {
            return Result.success("更新状态成功");
        } else {
            return Result.error(500, "更新状态失败");
        }
    }

    @GetMapping("/{id}")
    public Result getOrderDetail(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", order.getId());
            data.put("orderNo", order.getOrderNo());
            data.put("userId", order.getUserId());
            data.put("totalPrice", order.getTotalPrice());
            data.put("payPrice", order.getPayPrice());
            data.put("discountAmount", order.getDiscountAmount());
            data.put("receiver", order.getReceiver());
            data.put("receiverPhone", order.getReceiverPhone());
            data.put("receiverAddress", order.getReceiverAddress());
            data.put("status", order.getStatus());
            data.put("expressNo", order.getExpressNo());
            data.put("expressCompany", order.getExpressCompany());
            data.put("logisticsInfo", order.getLogisticsInfo());
            data.put("createTime", order.getCreateTime());
            data.put("payTime", order.getPayTime());
            data.put("deliveryTime", order.getDeliveryTime());
            data.put("receiveTime", order.getReceiveTime());
            return Result.success(data);
        }
        return Result.fail("订单不存在");
    }

    @GetMapping("/item/list")
    public Result getOrderItemList(@RequestParam(required = false) String keyword) {
        List<OrderItem> orderItems;
        if (keyword != null && !keyword.isEmpty()) {
            orderItems = orderItemMapper.selectByKeyword(keyword);
        } else {
            orderItems = orderItemMapper.selectAll();
        }
        return Result.success(orderItems);
    }

    @GetMapping("/item/order")
    public Result getOrderItemsByOrderId(@RequestParam Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        return Result.success(orderItems);
    }

    @GetMapping("/item/{id}")
    public Result getOrderItemById(@PathVariable Long id) {
        OrderItem orderItem = orderItemMapper.selectById(id);
        return Result.success(orderItem);
    }

    // 管理员确认支付
    @PostMapping("/confirmPayment")
    public Result adminConfirmPayment(@RequestBody java.util.Map<String, Object> params) {
        Long orderId = Long.parseLong(params.get("orderId").toString());

        Order order = orderMapper.selectById(orderId);
        if (order != null && (order.getStatus() == 0 || order.getStatus() == 1)) {
            // 将订单状态从待支付(0)/已支付(1)更新为待发货(2)，并记录支付时间
            orderMapper.updateStatusAndPayTime(orderId, 2, new Date());
            return Result.success("确认支付成功，订单状态已更新为待发货");
        } else {
            return Result.fail("确认支付失败，订单状态不正确");
        }
    }

    // 管理员更新物流信息
    @PostMapping("/updateLogistics")
    public Result adminUpdateLogistics(@RequestBody java.util.Map<String, Object> params) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        String expressNo = (String) params.get("expressNo");
        String expressCompany = (String) params.get("expressCompany");

        // 参数验证
        if (expressNo == null || expressNo.trim().isEmpty()) {
            return Result.fail("请填写快递单号");
        }
        if (expressCompany == null || expressCompany.trim().isEmpty()) {
            return Result.fail("请选择快递公司");
        }

        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            // 允许的状态：未支付(0)、已支付(1)、待发货(2)、已发货(3)
            if (order.getStatus() == 0 || order.getStatus() == 1 || order.getStatus() == 2 || order.getStatus() == 3) {
                // 检查是否有待处理的退款申请（除了已发货状态）
                if (order.getStatus() != 3 && order.getRefundStatus() != null && order.getRefundStatus() == 1) {
                    return Result.fail("该订单有待处理的退款申请，无法操作");
                }
                
                // 更新快递信息（快递单号和快递公司）
                orderMapper.updateExpressInfo(orderId, expressNo.trim(), expressCompany.trim());
                
                // 根据当前状态进行相应的更新
                if (order.getStatus() == 0) {
                    // 未支付订单，直接发货并更新为已发货，同时记录支付时间（管理员代付）
                    orderMapper.updateStatusAndPayTime(orderId, 3, new Date());
                    orderMapper.updateDeliveryTime(orderId, new Date());
                    return Result.success("订单已发货（管理员代付），订单状态已更新为已发货");
                } else if (order.getStatus() == 1) {
                    // 已支付订单，直接发货
                    orderMapper.updateStatus(orderId, 3);
                    orderMapper.updateDeliveryTime(orderId, new Date());
                    return Result.success("订单已发货，订单状态已更新为已发货");
                } else if (order.getStatus() == 2) {
                    // 待发货状态，更新为已发货
                    orderMapper.updateStatus(orderId, 3);
                    orderMapper.updateDeliveryTime(orderId, new Date());
                    return Result.success("物流信息更新成功，订单状态已更新为已发货");
                } else {
                    // 已发货状态，只更新快递信息
                    return Result.success("物流信息更新成功");
                }
            } else {
                return Result.fail("订单状态不允许编辑物流信息");
            }
        } else {
            return Result.fail("更新物流信息失败，订单不存在");
        }
    }

    @PostMapping("/item/update")
    public Result updateOrderItem(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        
        OrderItem orderItem = orderItemMapper.selectById(id);
        if (orderItem == null) {
            return Result.fail("订单项不存在");
        }
        
        if (params.containsKey("specInfo")) {
            orderItem.setSpecInfo((String) params.get("specInfo"));
        }
        
        if (params.containsKey("productPrice")) {
            orderItem.setProductPrice(Double.parseDouble(params.get("productPrice").toString()));
        }
        
        if (params.containsKey("quantity")) {
            orderItem.setQuantity(Integer.parseInt(params.get("quantity").toString()));
        }
        
        if (params.containsKey("totalPrice")) {
            orderItem.setTotalPrice(Double.parseDouble(params.get("totalPrice").toString()));
        } else if (orderItem.getProductPrice() != null && orderItem.getQuantity() != null) {
            orderItem.setTotalPrice(orderItem.getProductPrice() * orderItem.getQuantity());
        }
        
        int result = orderItemMapper.updateById(orderItem);
        if (result > 0) {
            return Result.success("订单项更新成功");
        } else {
            return Result.fail("订单项更新失败");
        }
    }

    @PostMapping("/batchConfirmPayment")
    public Result batchConfirmPayment(@RequestBody java.util.Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        java.util.List<Object> idList = (java.util.List<Object>) params.get("ids");
        int successCount = 0;
        int failCount = 0;
        
        for (Object idObj : idList) {
            Long orderId = Long.parseLong(idObj.toString());
            Order order = orderMapper.selectById(orderId);
            if (order != null && (order.getStatus() == 0 || order.getStatus() == 1)) {
                orderMapper.updateStatusAndPayTime(orderId, 2, new Date());
                successCount++;
            } else {
                failCount++;
            }
        }
        
        return Result.success(String.format("批量确认支付完成，成功%d个，失败%d个", successCount, failCount));
    }

    @PostMapping("/batchCancel")
    public Result batchCancelOrder(@RequestBody java.util.Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        java.util.List<Object> idList = (java.util.List<Object>) params.get("ids");
        int successCount = 0;
        int failCount = 0;
        java.util.List<String> failReasons = new java.util.ArrayList<>();
        
        for (Object idObj : idList) {
            Long orderId = Long.parseLong(idObj.toString());
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                failCount++;
                failReasons.add("订单" + orderId + "不存在");
                continue;
            }
            
            if (order.getStatus() >= 3) {
                failCount++;
                String statusText = order.getStatus() == 3 ? "已发货" : (order.getStatus() == 4 ? "已完成" : "已退款");
                failReasons.add("订单" + order.getOrderNo() + "状态为" + statusText + "，无法取消");
                continue;
            }
            
            if (order.getStatus() == 5 || order.getStatus() == 6) {
                failCount++;
                failReasons.add("订单" + order.getOrderNo() + "已取消或已退款");
                continue;
            }
            
            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
            for (OrderItem item : orderItems) {
                productMapper.increaseStock(item.getProductId(), item.getQuantity());
            }
            orderMapper.updateStatus(orderId, 5);
            successCount++;
        }
        
        String message = String.format("批量取消订单完成，成功%d个，失败%d个", successCount, failCount);
        if (!failReasons.isEmpty() && failReasons.size() <= 3) {
            message += "。失败原因：" + String.join("；", failReasons);
        }
        
        return Result.success(message);
    }

    @PostMapping("/batchShip")
    public Result batchShipOrder(@RequestBody java.util.Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        java.util.List<Object> idList = (java.util.List<Object>) params.get("ids");
        String expressCompany = (String) params.get("expressCompany");
        String expressNo = (String) params.get("expressNo");
        
        if (expressCompany == null || expressCompany.trim().isEmpty()) {
            return Result.fail("请选择快递公司");
        }
        if (expressNo == null || expressNo.trim().isEmpty()) {
            return Result.fail("请输入快递单号");
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (Object idObj : idList) {
            Long orderId = Long.parseLong(idObj.toString());
            Order order = orderMapper.selectById(orderId);
            if (order != null && (order.getStatus() == 1 || order.getStatus() == 2)) {
                orderMapper.updateExpressInfo(orderId, expressNo.trim(), expressCompany.trim());
                orderMapper.updateStatus(orderId, 3);
                orderMapper.updateDeliveryTime(orderId, new Date());
                successCount++;
            } else {
                failCount++;
            }
        }
        
        return Result.success(String.format("批量发货完成，成功%d个，失败%d个", successCount, failCount));
    }

}