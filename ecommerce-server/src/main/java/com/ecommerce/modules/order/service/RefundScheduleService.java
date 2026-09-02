package com.ecommerce.modules.order.service;

import com.ecommerce.modules.order.entity.Order;
import com.ecommerce.modules.order.mapper.OrderMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class RefundScheduleService {

    @Resource
    private OrderMapper orderMapper;

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutRefunds() {
        List<Order> refundOrders = orderMapper.selectByRefundStatus(1);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -30);
        Date timeout = calendar.getTime();

        for (Order order : refundOrders) {
            if (order.getRefundApplyTime() != null && order.getRefundApplyTime().before(timeout)) {
                orderMapper.updateRefundStatus(order.getId(), 2, "系统自动退款：超过30分钟未处理");
                orderMapper.updateStatus(order.getId(), 6);
            }
        }
    }
}