package com.ecommerce.modules.order.service;

import com.ecommerce.modules.config.service.ConfigService;
import com.ecommerce.modules.order.entity.Order;
import com.ecommerce.modules.order.mapper.OrderMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class OrderTimeoutService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ConfigService configService;

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutOrders() {
        List<Order> unpaidOrders = orderMapper.selectByUserIdAndStatus(null, 0);
        
        int cancelMinutes = 30;
        try {
            String configValue = configService.getConfigValue("order_auto_cancel");
            if (configValue != null && !configValue.isEmpty()) {
                cancelMinutes = Integer.parseInt(configValue);
            }
        } catch (NumberFormatException e) {
            cancelMinutes = 30;
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -cancelMinutes);
        Date timeout = calendar.getTime();

        for (Order order : unpaidOrders) {
            if (order.getCreateTime() != null && order.getCreateTime().before(timeout)) {
                orderMapper.updateStatus(order.getId(), 5);
            }
        }
    }
}
