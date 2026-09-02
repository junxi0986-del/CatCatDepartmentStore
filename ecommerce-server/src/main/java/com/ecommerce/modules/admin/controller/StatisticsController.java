package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.order.entity.Order;
import com.ecommerce.modules.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @GetMapping("/overview")
    public Result getStatisticsOverview() {
        Map<String, Object> data = new HashMap<>();
        
        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Long.class);
        Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        Long orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_info", Long.class);
        
        Double totalSales = orderMapper.sumOrderAmount();
        if (totalSales == null) {
            totalSales = 0.0;
        }
        
        data.put("userCount", userCount);
        data.put("productCount", productCount);
        data.put("orderCount", orderCount);
        data.put("salesAmount", totalSales);
        
        return Result.success(data);
    }

    @GetMapping("/monthlySales")
    public Result getMonthlySales() {
        List<Map<String, Object>> monthlyData = orderMapper.getMonthlySales();
        return Result.success(monthlyData);
    }

    @GetMapping("/dailySales")
    public Result getDailySales(@RequestParam String month) {
        List<Map<String, Object>> dailyData = orderMapper.getDailySalesByMonth(month);
        return Result.success(dailyData);
    }

    @GetMapping("/ordersByDate")
    public Result getOrdersByDate(@RequestParam String date) {
        String sql = "SELECT order_no, pay_price, create_time FROM order_info WHERE DATE(create_time) = ? AND status >= 1 ORDER BY create_time DESC";
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql, date);
        return Result.success(orders);
    }

    @GetMapping("/exportReport")
    public void exportReport(@RequestParam String month, HttpServletResponse response) {
        try {
            List<Map<String, Object>> dailyData = orderMapper.getDailySalesByMonth(month);
            
            response.setContentType("text/csv; charset=UTF-8");
            String fileName = URLEncoder.encode("销售报表_" + month + ".csv", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            
            OutputStream os = response.getOutputStream();
            // 添加 BOM 头，让 Excel 正确识别 UTF-8 编码
            os.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            
            StringBuilder sb = new StringBuilder();
            sb.append("日期,订单数,销售额(元)\n");
            
            for (Map<String, Object> row : dailyData) {
                sb.append(row.get("date")).append(",");
                sb.append(row.get("orderCount")).append(",");
                sb.append(String.format("%.2f", row.get("amount") != null ? Double.parseDouble(row.get("amount").toString()) : 0)).append("\n");
            }
            
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
