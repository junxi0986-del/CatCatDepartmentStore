package com.ecommerce.modules.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.modules.ai.service.AiService;
import com.ecommerce.modules.product.entity.Product;
import com.ecommerce.modules.product.mapper.ProductMapper;
import com.ecommerce.modules.user.entity.User;
import com.ecommerce.modules.user.mapper.UserMapper;
import com.ecommerce.modules.order.mapper.OrderMapper;
import com.ecommerce.modules.order.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request,
                                    @RequestHeader(value = "token", required = false) String token) {
        Map<String, Object> result = new HashMap<>();

        try {
            String message = (String) request.get("message");
            Boolean withContext = request.get("context") != null ? (Boolean) request.get("context") : false;

            if (message == null || message.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "消息不能为空");
                return result;
            }

            // 解析对话历史
            List<AiService.ChatMessage> chatHistory = new ArrayList<>();
            Object historyObj = request.get("history");
            if (historyObj instanceof List<?>) {
                List<?> historyList = (List<?>) historyObj;
                for (Object item : historyList) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> historyItem = (Map<String, Object>) item;
                        String role = historyItem.get("role") != null ? historyItem.get("role").toString() : "user";
                        String content = historyItem.get("content") != null ? historyItem.get("content").toString() : "";
                        chatHistory.add(new AiService.ChatMessage(role, content));
                    }
                }
            }

            List<Product> allProducts = productMapper.selectAll();

            List<Map<String, Object>> productMaps = new ArrayList<>();
            for (Product product : allProducts) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", product.getId());
                productMap.put("name", product.getName());
                productMap.put("price", product.getPrice());
                productMap.put("pic", product.getPic());
                productMap.put("categoryId", product.getCategoryId());
                productMap.put("description", product.getDetail());
                productMaps.add(productMap);
            }

            Map<String, Object> searchResult = aiService.searchWithSelection(message, productMaps, chatHistory);
            String aiReply = (String) searchResult.get("reply");
            List<Number> selectedIds = (List<Number>) searchResult.get("ids");
            Map<Long, String> reasons = (Map<Long, String>) searchResult.get("reasons");
            boolean protocolFound = Boolean.TRUE.equals(searchResult.get("protocolFound"));

            // 优先使用 AI 精选的商品（按 AI 给出的推荐顺序）
            List<Product> matchedProducts = new ArrayList<>();
            if (selectedIds != null && !selectedIds.isEmpty()) {
                Map<Long, Product> productById = new HashMap<>();
                for (Product p : allProducts) {
                    productById.put(p.getId(), p);
                }
                for (Number id : selectedIds) {
                    Product p = productById.get(id.longValue());
                    if (p != null && !matchedProducts.contains(p)) {
                        matchedProducts.add(p);
                    }
                }
            } else if (!protocolFound) {
                // AI 未按协议输出时才用关键词规则兜底；
                // AI 明确判断"没有匹配商品"（协议行存在但为空）时尊重该判断
                matchedProducts = fallbackMatch(message, allProducts);
            }

            List<Map<String, Object>> responseProducts = new ArrayList<>();
            for (Product p : matchedProducts) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", p.getId());
                productMap.put("name", p.getName());
                productMap.put("price", p.getPrice());
                productMap.put("pic", p.getPic());
                productMap.put("description", p.getDetail());
                productMap.put("reason", reasons != null ? reasons.get(p.getId()) : null);
                responseProducts.add(productMap);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("reply", aiReply);
            
            if (!responseProducts.isEmpty()) {
                responseData.put("type", "products");
                responseData.put("products", responseProducts);
                responseData.put("intro", "为您找到以下商品，点击【加入购物车】可添加到购物车：");
            } else {
                responseData.put("type", "text");
            }

            result.put("code", 200);
            result.put("message", "success");
            result.put("data", responseData);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "AI服务出错：" + e.getMessage());
        }

        return result;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String query) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (query == null || query.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "搜索关键词不能为空");
                return result;
            }

            List<Product> allProducts = productMapper.selectAll();

            List<Map<String, Object>> productMaps = new ArrayList<>();
            for (Product product : allProducts) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", product.getId());
                productMap.put("name", product.getName());
                productMap.put("price", product.getPrice());
                productMap.put("pic", product.getPic());
                productMap.put("categoryId", product.getCategoryId());
                String detail = product.getDetail();
                if (detail != null && detail.length() > 100) {
                    detail = detail.substring(0, 100) + "...";
                }
                productMap.put("description", detail);
                productMaps.add(productMap);
            }

            Map<String, Object> searchResult = aiService.searchWithSelection(query, productMaps, null);
            String aiReply = (String) searchResult.get("reply");
            List<Number> selectedIds = (List<Number>) searchResult.get("ids");
            boolean protocolFound = Boolean.TRUE.equals(searchResult.get("protocolFound"));

            // 优先使用 AI 精选的商品（按 AI 给出的推荐顺序）
            List<Product> matchedProducts = new ArrayList<>();
            if (selectedIds != null && !selectedIds.isEmpty()) {
                Map<Long, Product> productById = new HashMap<>();
                for (Product p : allProducts) {
                    productById.put(p.getId(), p);
                }
                for (Number id : selectedIds) {
                    Product p = productById.get(id.longValue());
                    if (p != null && !matchedProducts.contains(p)) {
                        matchedProducts.add(p);
                    }
                }
            } else if (!protocolFound) {
                // AI 未按协议输出时才用关键词规则兜底
                matchedProducts = fallbackMatch(query, allProducts);
            }

            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of(
                    "reply", aiReply,
                    "products", matchedProducts
            ));

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "AI搜索服务出错：" + e.getMessage());
        }

        return result;
    }

    /**
     * 关键词兜底匹配（仅在 AI 未按协议输出时使用）。
     * 按商品名称关键词匹配，不依赖 category_id（商品数据的类别归属可能不准确，
     * 例如机械键盘的 category_id 与手机相同）。
     */
    private List<Product> fallbackMatch(String message, List<Product> allProducts) {
        Map<String, List<String>> categoryKeywords = new LinkedHashMap<>();
        categoryKeywords.put("手机", List.of("手机", "iphone", "苹果", "华为", "小米", "三星", "荣耀", "oppo", "vivo", "红米", "一加", "realme", "魅族", "中兴", "努比亚"));
        categoryKeywords.put("电脑", List.of("电脑", "笔记本", "macbook", "laptop", "台式", "拯救者", "thinkpad", "mac"));
        categoryKeywords.put("平板", List.of("平板", "ipad", "pad"));
        categoryKeywords.put("服装", List.of("恤", "衬衫", "外套", "卫衣", "裤", "裙", "夹克", "羽绒服", "毛衣"));
        categoryKeywords.put("家电", List.of("冰箱", "洗衣机", "空调", "电视", "微波炉", "电饭煲", "热水器", "吸尘器"));

        String msg = message.toLowerCase();
        List<String> nameKeywords = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            if (msg.contains(entry.getKey())) {
                nameKeywords.addAll(entry.getValue());
            }
        }

        List<Product> out = new ArrayList<>();
        if (nameKeywords.isEmpty()) {
            return out;
        }
        for (Product p : allProducts) {
            String name = p.getName() == null ? "" : p.getName().toLowerCase();
            for (String kw : nameKeywords) {
                if (name.contains(kw)) {
                    out.add(p);
                    break;
                }
            }
            if (out.size() >= 8) {
                break;
            }
        }
        return out;
    }

    @PostMapping("/chat/order")
    public Map<String, Object> chatWithOrder(@RequestBody Map<String, Object> request,
                                             @RequestHeader(value = "token", required = false) String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String message = (String) request.get("message");
            Long orderId = request.get("orderId") != null ? Long.valueOf(request.get("orderId").toString()) : null;

            if (message == null || message.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "消息不能为空");
                return result;
            }

            Map<String, Object> context = new HashMap<>();

            if (orderId != null) {
                Order order = orderMapper.selectById(orderId);
                if (order != null) {
                    String orderInfo = String.format("订单号:%s, 状态:%s, 金额:%s",
                            order.getOrderNo(),
                            getOrderStatusText(order.getStatus()),
                            order.getTotalPrice());
                    context.put("orderInfo", orderInfo);
                    
                    if (order.getLogisticsInfo() != null && !order.getLogisticsInfo().isEmpty()) {
                        context.put("logisticsInfo", order.getLogisticsInfo());
                    }
                    if (order.getExpressNo() != null && !order.getExpressNo().isEmpty()) {
                        context.put("expressNo", order.getExpressNo());
                    }
                    if (order.getExpressCompany() != null && !order.getExpressCompany().isEmpty()) {
                        context.put("expressCompany", order.getExpressCompany());
                    }
                }
            }

            String reply = aiService.chatWithContext(message, context);

            result.put("code", 200);
            result.put("message", "success");
            result.put("data", reply);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "AI服务出错：" + e.getMessage());
        }

        return result;
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "待发货";
            case 3: return "已发货";
            case 4: return "已完成";
            case 5: return "已取消";
            case 6: return "已退款";
            default: return "未知";
        }
    }

    @GetMapping("/analyzeIntent")
    public Map<String, Object> analyzeIntent(@RequestParam String message) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> intent = aiService.analyzeOrderIntent(message, productMapper.selectAll());
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", intent);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "意图分析失败：" + e.getMessage());
        }

        return result;
    }

    @PostMapping("/analyzeProduct")
    public String analyzeProduct(@RequestBody Map<String, Object> request) {
        try {
            String productName = request.get("name") != null ? request.get("name").toString() : "";
            String productDetail = request.get("detail") != null ? request.get("detail").toString() : "";

            String aiResponse = aiService.analyzeProductDetail(productName, productDetail);

            return "{\"code\":200,\"message\":\"success\",\"data\":" + aiResponse + "}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"code\":500,\"message\":\"商品分析失败\",\"data\":{\"params\":[{\"label\":\"品牌\",\"value\":\"未知\"},{\"label\":\"型号\",\"value\":\"未知\"}]}}";
        }
    }

    @PostMapping("/analyzeProductSpecs")
    public String analyzeProductSpecs(@RequestBody Map<String, Object> request) {
        try {
            String productName = request.get("name") != null ? request.get("name").toString() : "";
            String productDetail = request.get("detail") != null ? request.get("detail").toString() : "";

            String aiResponse = aiService.analyzeProductSpecs(productName, productDetail);

            return "{\"code\":200,\"message\":\"success\",\"data\":" + aiResponse + "}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"code\":500,\"message\":\"规格分析失败\",\"data\":{\"specs\":[]}}";
        }
    }

    @PostMapping("/generateProductDetail")
    public Map<String, Object> generateProductDetail(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String productName = request.get("name") != null ? request.get("name").toString() : "";
            
            if (productName == null || productName.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "商品名称不能为空");
                return result;
            }

            Map<String, Object> aiResult = aiService.generateProductDetail(productName);

            result.put("code", 200);
            result.put("message", "success");
            result.put("data", aiResult);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "生成商品详情失败：" + e.getMessage());
        }

        return result;
    }

    @PostMapping("/generateReviews")
    public Map<String, Object> generateReviews(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String productName = request.get("name") != null ? request.get("name").toString() : "";
            Integer categoryId = request.get("categoryId") != null ? Integer.valueOf(request.get("categoryId").toString()) : null;

            Map<String, Object> aiResult = aiService.generateProductReviews(productName, categoryId);

            result.put("code", 200);
            result.put("message", "success");
            result.put("data", aiResult);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "生成评价失败：" + e.getMessage());
        }

        return result;
    }

    @PostMapping("/analyzeSalesData")
    public Map<String, Object> analyzeSalesData(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String month = request.get("month") != null ? request.get("month").toString() : "";
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dailyData = (List<Map<String, Object>>) request.get("dailyData");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) request.get("summary");

            String report = aiService.analyzeSalesData(month, dailyData, summary);

            Map<String, Object> data = new HashMap<>();
            data.put("report", report);
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "分析失败：" + e.getMessage());
        }

        return result;
    }
}