package com.ecommerce.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.modules.config.service.ConfigService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiService {

    @Value("${deepseek.api-key}")
    private String defaultApiKey;

    @Value("${deepseek.api-url}")
    private String deepseekApiUrl;

    @Value("${deepseek.model}")
    private String deepseekModel;

    @Value("${ai.system-prompt}")
    private String systemPrompt;

    @Value("${ai.max-tokens}")
    private int maxTokens;

    @Value("${ai.temperature}")
    private double temperature;

    @Autowired(required = false)
    private ConfigService configService;

    @Autowired
    private AiHttpClient aiHttpClient;
    
    private final ObjectMapper objectMapper;

    public AiService() {
        this.objectMapper = new ObjectMapper();
    }

    private String getApiKey() {
        if (configService != null) {
            String dbKey = configService.getConfigValue("deepseek_api_key");
            if (dbKey != null && !dbKey.isEmpty()) {
                return dbKey;
            }
        }
        return defaultApiKey;
    }

    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    public String chat(String userMessage, List<ChatMessage> history) {
        try {
            String apiKey = getApiKey();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepseekModel);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            if (history != null && !history.isEmpty()) {
                for (ChatMessage msg : history) {
                    Map<String, String> message = new HashMap<>();
                    message.put("role", msg.getRole());
                    message.put("content", msg.getContent());
                    messages.add(message);
                }
            }

            Map<String, String> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", userMessage);
            messages.add(userMessageMap);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);
            
            if (aiResponse != null) {
                return aiResponse;
            }

            return "抱歉，AI服务暂时无法回复，请稍后再试。";

        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI服务出现错误：" + e.getMessage();
        }
    }

    public String chatWithContext(String userMessage, Map<String, Object> context) {
        StringBuilder contextPrompt = new StringBuilder(systemPrompt);
        contextPrompt.append("\n\n当前上下文信息：");

        if (context.containsKey("userName")) {
            contextPrompt.append("\n- 用户名：").append(context.get("userName"));
        }
        if (context.containsKey("orderInfo")) {
            contextPrompt.append("\n- 订单信息：").append(context.get("orderInfo"));
        }
        if (context.containsKey("logisticsInfo")) {
            contextPrompt.append("\n- 物流信息：").append(context.get("logisticsInfo"));
        }
        if (context.containsKey("expressNo")) {
            contextPrompt.append("\n- 运单号：").append(context.get("expressNo"));
        }
        if (context.containsKey("expressCompany")) {
            contextPrompt.append("\n- 快递公司：").append(context.get("expressCompany"));
        }
        if (context.containsKey("cartItems")) {
            contextPrompt.append("\n- 购物车商品：").append(context.get("cartItems"));
        }
        if (context.containsKey("recentProducts")) {
            contextPrompt.append("\n- 最近浏览商品：").append(context.get("recentProducts"));
        }

        contextPrompt.append("\n\n用户问题：").append(userMessage);

        try {
            String apiKey = getApiKey();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepseekModel);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", contextPrompt.toString());
            messages.add(systemMessage);

            Map<String, String> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", userMessage);
            messages.add(userMessageMap);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);
            
            if (aiResponse != null) {
                return aiResponse;
            }

            return "抱歉，AI服务暂时无法回复，请稍后再试。";

        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI服务出现错误：" + e.getMessage();
        }
    }

    public String search(String query, List<Map<String, Object>> products) {
        return search(query, products, null);
    }

    public String search(String query, List<Map<String, Object>> products, List<ChatMessage> history) {
        StringBuilder productContext = new StringBuilder("以下是商城中的商品列表：\n");
        for (Map<String, Object> product : products) {
            String desc = product.get("description") != null ? product.get("description").toString() : "";
            String shortDesc = desc.length() > 50 ? desc.substring(0, 50) + "..." : (desc.isEmpty() ? "无描述" : desc);
            productContext.append(String.format("- %s (ID:%s, 价格:%s元, 描述:%s)\n",
                    product.get("name"),
                    product.get("id"),
                    product.get("price"),
                    shortDesc
            ));
        }

        String searchPrompt = String.format("""
            %s

            以下是商城中的商品列表：
            %s

            用户想要搜索：%s

            请根据用户的需求，从上面的商品列表中推荐合适的商品。
            商品名称可能存在乱码，请根据价格和描述内容判断商品类型。
            如果商品价格在3000元以上且描述中包含"手机"、"Pro"、"Max"、"Ultra"、"骁龙"、"麒麟"等关键词，可能是手机。
            如果商品列表中有匹配的商品，请列出推荐理由和商品信息。
            如果没有合适的商品，请告诉用户目前没有找到匹配的商品，并给出购物建议。
            回复要简洁明了，突出商品的关键信息。
            """, systemPrompt, productContext.toString(), query);

        try {
            String apiKey = getApiKey();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepseekModel);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", searchPrompt);
            messages.add(systemMessage);

            // 添加对话历史，支持多轮对话
            if (history != null && !history.isEmpty()) {
                for (ChatMessage msg : history) {
                    Map<String, String> historyMsg = new HashMap<>();
                    historyMsg.put("role", msg.getRole());
                    historyMsg.put("content", msg.getContent());
                    messages.add(historyMsg);
                }
            }

            Map<String, String> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", "帮我找：" + query);
            messages.add(userMessageMap);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);

            if (aiResponse != null) {
                return aiResponse;
            }

            return "抱歉，AI搜索服务暂时无法使用。";

        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI搜索服务出现错误：" + e.getMessage();
        }
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public Map<String, Object> analyzeOrderIntent(String userMessage, List<?> products) {
        Map<String, Object> intent = new HashMap<>();
        intent.put("isOrderIntent", false);
        intent.put("products", new ArrayList<>());
        intent.put("budget", null);
        intent.put("quantity", 1);
        intent.put("description", userMessage);

        String lowerMessage = userMessage.toLowerCase();
        
        boolean hasBuyIntent = lowerMessage.contains("买") || lowerMessage.contains("订购") || 
                               lowerMessage.contains("下单") || lowerMessage.contains("购买") ||
                               lowerMessage.contains("想要") || lowerMessage.contains("要买");
        
        boolean hasProduct = false;
        List<Map<String, Object>> matchedProducts = new ArrayList<>();
        
        if (products != null) {
            for (Object p : products) {
                Map<String, Object> product = convertToMap(p);
                if (product != null) {
                    String name = product.get("name") != null ? product.get("name").toString().toLowerCase() : "";
                    String desc = product.get("description") != null ? product.get("description").toString().toLowerCase() : "";
                    
                    if (name.contains("手机") || name.contains("phone") ||
                        desc.contains("手机") || desc.contains("骁龙") || desc.contains("麒麟")) {
                        if (userMessage.contains("手机") || userMessage.contains("phone") ||
                            userMessage.contains("3000") || userMessage.contains("4000") ||
                            userMessage.contains("5000") || userMessage.contains("6000") ||
                            userMessage.contains("7000") || userMessage.contains("8000")) {
                            matchedProducts.add(product);
                            hasProduct = true;
                        }
                    }
                    
                    if (name.contains("电脑") || name.contains("computer") ||
                        name.contains("笔记本") || name.contains("laptop")) {
                        if (userMessage.contains("电脑") || userMessage.contains("笔记本")) {
                            matchedProducts.add(product);
                            hasProduct = true;
                        }
                    }
                }
            }
        }
        
        if (hasBuyIntent && hasProduct) {
            intent.put("isOrderIntent", true);
            intent.put("products", matchedProducts);
            
            String budgetStr = userMessage.replaceAll("[^0-9]", "");
            if (!budgetStr.isEmpty()) {
                try {
                    int budget = Integer.parseInt(budgetStr);
                    if (budget > 100 && budget < 100000) {
                        intent.put("budget", budget);
                    }
                } catch (NumberFormatException e) {
                }
            }
            
            String quantityStr = userMessage.replaceAll("[^0-9]", "");
            if (!quantityStr.isEmpty() && quantityStr.length() <= 2) {
                try {
                    int qty = Integer.parseInt(quantityStr);
                    if (qty > 0 && qty <= 100) {
                        intent.put("quantity", qty);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        
        return intent;
    }

    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return map;
        }
        if (obj instanceof com.ecommerce.modules.product.entity.Product) {
            com.ecommerce.modules.product.entity.Product product = 
                (com.ecommerce.modules.product.entity.Product) obj;
            Map<String, Object> map = new HashMap<>();
            map.put("id", product.getId());
            map.put("name", product.getName());
            map.put("price", product.getPrice());
            map.put("pic", product.getPic());
            map.put("detail", product.getDetail());
            return map;
        }
        return null;
    }

    /**
     * AI分析商品详情，提取参数信息
     */
    public String analyzeProductDetail(String productName, String productDetail) {
        String prompt = String.format("""
            请分析以下商品的详情信息，并提取关键参数。
            
            商品名称：%s
            
            商品详情：
            %s
            
            请按照JSON格式返回商品参数，格式如下：
            {
                "params": [
                    {"label": "品牌", "value": "品牌名称"},
                    {"label": "型号", "value": "商品型号"},
                    {"label": "颜色", "value": "颜色信息"},
                    {"label": "尺寸", "value": "尺寸规格"},
                    {"label": "重量", "value": "重量信息"}
                ]
            }
            
            只返回JSON，不要其他文字。如果信息不足，可以根据商品名称合理推测。
            """, productName, productDetail);

        try {
            String apiKey = getApiKey();

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的电商商品分析师，擅长从商品详情中提取关键参数信息。请以JSON格式返回结果，不要包含markdown代码块标记。");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);
            
            if (aiResponse != null) {
                String cleanedResponse = aiResponse;
                if (cleanedResponse.contains("```json")) {
                    cleanedResponse = cleanedResponse.replaceAll("```json\\s*", "").replaceAll("\\s*```", "");
                }
                if (cleanedResponse.contains("```")) {
                    cleanedResponse = cleanedResponse.replaceAll("```\\s*", "").replaceAll("\\s*```", "");
                }
                cleanedResponse = cleanedResponse.trim();
                return cleanedResponse;
            }

            return getDefaultParams(productName);
        } catch (Exception e) {
            e.printStackTrace();
            return getDefaultParams(productName);
        }
    }

    private String getDefaultParams(String productName) {
        String brand = "未知品牌";
        if (productName != null && !productName.isEmpty()) {
            String[] parts = productName.split(" ");
            brand = parts[0];
        }
        return String.format("{\"params\":[{\"label\":\"品牌\",\"value\":\"%s\"},{\"label\":\"型号\",\"value\":\"%s\"},{\"label\":\"产地\",\"value\":\"中国\"}]}", brand, productName != null ? productName : "未知");
    }

    public String analyzeProductSpecs(String productName, String productDetail) {
        String prompt = String.format("""
            请分析以下商品的规格信息，特别是颜色和型号选项。
            
            商品名称：%s
            
            商品详情：
            %s
            
            请按照JSON格式返回商品规格选项，格式如下：
            {
                "specs": [
                    {
                        "specName": "颜色",
                        "options": [
                            {"value": "黑色", "price": 0, "stock": 100},
                            {"value": "白色", "price": 0, "stock": 80}
                        ]
                    },
                    {
                        "specName": "型号",
                        "options": [
                            {"value": "标准版", "price": 0, "stock": 50},
                            {"value": "高级版", "price": 100, "stock": 30}
                        ]
                    }
                ]
            }
            
            规则：
            1. specName是规格名称（如"颜色"、"型号"、"套餐"、"容量"等）
            2. options是可选的值列表，每个值包含：
               - value: 规格值名称（如"黑色"、"XL"、"256GB"）
               - price: 相对基本价格的加价（0表示不加价），可以是负数表示降价
               - stock: 该规格的库存数量
            3. 只分析颜色和型号相关的规格，不要添加不相关的规格
            4. 如果是手机/电脑等数码产品，重点分析容量规格
            5. 如果是服装鞋帽，重点分析尺码和颜色
            6. 只返回JSON，不要其他文字。如果无法分析，返回空数组: {"specs": []}
            """, productName, productDetail);

        try {
            String apiKey = getApiKey();

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的电商商品规格分析师，擅长分析商品的颜色、型号、尺码等规格选项。请以JSON格式返回分析结果，不要包含markdown代码块标记。");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);

            if (aiResponse != null) {
                String cleanedResponse = aiResponse;
                if (cleanedResponse.contains("```json")) {
                    cleanedResponse = cleanedResponse.replaceAll("```json\\s*", "").replaceAll("\\s*```", "");
                }
                if (cleanedResponse.contains("```")) {
                    cleanedResponse = cleanedResponse.replaceAll("```\\s*", "").replaceAll("\\s*```", "");
                }
                cleanedResponse = cleanedResponse.trim();
                System.out.println("Cleaned AI Response: " + cleanedResponse);
                return cleanedResponse;
            }

            return "{\"specs\":[]}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"specs\":[]}";
        }
    }

    public Map<String, Object> generateProductDetail(String productName) {
        String prompt = String.format("""
            请根据商品名称"%s"，生成完整的商品详情信息。
            
            请按照JSON格式返回，格式如下：
            {
                "detail": "商品详情HTML内容，包含商品介绍、特点、参数等信息",
                "specs": [
                    {
                        "specName": "颜色",
                        "options": [
                            {"value": "黑色", "price": 0, "stock": 100},
                            {"value": "白色", "price": 0, "stock": 80}
                        ]
                    },
                    {
                        "specName": "型号",
                        "options": [
                            {"value": "标准版", "price": 0, "stock": 50},
                            {"value": "高级版", "price": 100, "stock": 30}
                        ]
                    }
                ],
                "params": [
                    {"label": "品牌", "value": "品牌名称"},
                    {"label": "型号", "value": "商品型号"},
                    {"label": "产地", "value": "产地信息"}
                ]
            }
            
            要求：
            1. detail字段必须是HTML格式的商品详情，包含<h3>标题和<p>段落，内容要丰富详细
            2. 根据商品名称智能判断商品类型，生成合适的规格选项：
               - 手机/平板：颜色、容量、套餐
               - 电脑/笔记本：颜色、配置、尺寸
               - 服装/鞋帽：颜色、尺码
               - 家电：颜色、型号、功率
               - 其他：颜色、型号
            3. specs数组包含商品规格，每个规格有specName和options
            4. params数组包含商品基本参数信息
            5. 只返回JSON，不要其他文字
            6. 所有库存随机设置在50-200之间
            """, productName);

        try {
            String apiKey = getApiKey();

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的电商商品信息生成助手，擅长根据商品名称生成详细的商品描述、规格选项和参数信息。请以JSON格式返回结果，不要包含markdown代码块标记。");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);

            if (aiResponse != null) {
                String cleanedResponse = aiResponse;
                if (cleanedResponse.contains("```json")) {
                    cleanedResponse = cleanedResponse.replaceAll("```json\\s*", "").replaceAll("\\s*```", "");
                }
                if (cleanedResponse.contains("```")) {
                    cleanedResponse = cleanedResponse.replaceAll("```\\s*", "").replaceAll("\\s*```", "");
                }
                cleanedResponse = cleanedResponse.trim();
                
                JsonNode rootNode = objectMapper.readTree(cleanedResponse);
                Map<String, Object> result = new HashMap<>();
                
                if (rootNode.has("detail")) {
                    result.put("detail", rootNode.get("detail").asText());
                }
                
                if (rootNode.has("specs")) {
                    List<Map<String, Object>> specs = new ArrayList<>();
                    JsonNode specsNode = rootNode.get("specs");
                    for (JsonNode specNode : specsNode) {
                        Map<String, Object> spec = new HashMap<>();
                        spec.put("specName", specNode.get("specName").asText());
                        List<Map<String, Object>> options = new ArrayList<>();
                        for (JsonNode optNode : specNode.get("options")) {
                            Map<String, Object> opt = new HashMap<>();
                            opt.put("value", optNode.get("value").asText());
                            opt.put("price", optNode.has("price") ? optNode.get("price").asDouble() : 0);
                            opt.put("stock", optNode.has("stock") ? optNode.get("stock").asInt() : 100);
                            options.add(opt);
                        }
                        spec.put("options", options);
                        specs.add(spec);
                    }
                    result.put("specs", specs);
                }
                
                if (rootNode.has("params")) {
                    List<Map<String, String>> params = new ArrayList<>();
                    JsonNode paramsNode = rootNode.get("params");
                    for (JsonNode paramNode : paramsNode) {
                        Map<String, String> param = new HashMap<>();
                        param.put("label", paramNode.get("label").asText());
                        param.put("value", paramNode.get("value").asText());
                        params.add(param);
                    }
                    result.put("params", params);
                }
                
                return result;
            }

            return getDefaultResult(productName);

        } catch (Exception e) {
            e.printStackTrace();
            return getDefaultResult(productName);
        }
    }

    private Map<String, Object> getDefaultResult(String productName) {
        Map<String, Object> result = new HashMap<>();
        result.put("detail", "<h3>商品介绍</h3><p>" + productName + "，品质优良，值得信赖。</p>");
        result.put("specs", new ArrayList<>());
        result.put("params", List.of(
            Map.of("label", "品牌", "value", "未知"),
            Map.of("label", "型号", "value", "未知")
        ));
        return result;
    }

    public Map<String, Object> generateProductReviews(String productName, Integer categoryId) {
        String categoryType = getCategoryType(categoryId, productName);
        
        String prompt = String.format("""
            请为商品"%s"（类型：%s）生成5条真实的用户评价。
            
            请按照JSON格式返回，格式如下：
            {
                "reviews": [
                    {
                        "user": "用户***8",
                        "rating": 5,
                        "content": "评价内容",
                        "time": "2026-05-20"
                    }
                ]
            }
            
            要求：
            1. 评价内容要真实自然，符合该商品类型的特点
            2. rating为1-5的评分，大部分应该是4-5分
            3. 评价内容要具体，提到商品的具体特点
            4. user使用脱敏格式如"用户***8"、"购物达人"等
            5. time为近期日期，从今天往前推
            6. 只返回JSON，不要其他文字
            """, productName, categoryType);

        try {
            String apiKey = getApiKey();

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个电商平台的用户评价生成助手，擅长生成真实自然的商品评价。请以JSON格式返回结果。");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);

            if (aiResponse != null) {
                String cleanedResponse = aiResponse;
                if (cleanedResponse.contains("```json")) {
                    cleanedResponse = cleanedResponse.replaceAll("```json\\s*", "").replaceAll("\\s*```", "");
                }
                if (cleanedResponse.contains("```")) {
                    cleanedResponse = cleanedResponse.replaceAll("```\\s*", "").replaceAll("\\s*```", "");
                }
                cleanedResponse = cleanedResponse.trim();
                
                JsonNode rootNode = objectMapper.readTree(cleanedResponse);
                Map<String, Object> result = new HashMap<>();
                
                if (rootNode.has("reviews")) {
                    List<Map<String, Object>> reviews = new ArrayList<>();
                    JsonNode reviewsNode = rootNode.get("reviews");
                    for (JsonNode reviewNode : reviewsNode) {
                        Map<String, Object> review = new HashMap<>();
                        review.put("user", reviewNode.get("user").asText());
                        int rating = reviewNode.has("rating") ? reviewNode.get("rating").asInt() : 5;
                        review.put("rating", "⭐".repeat(rating));
                        review.put("content", reviewNode.get("content").asText());
                        review.put("time", reviewNode.get("time").asText());
                        reviews.add(review);
                    }
                    result.put("reviews", reviews);
                }
                
                return result;
            }

            return getDefaultReviews(productName);

        } catch (Exception e) {
            e.printStackTrace();
            return getDefaultReviews(productName);
        }
    }

    private String getCategoryType(Integer categoryId, String productName) {
        if (categoryId != null) {
            switch (categoryId) {
                case 1: return "数码产品/手机";
                case 2: return "家居用品/电脑";
                case 3: return "服装鞋帽";
                case 4: return "手机配件";
                case 5: return "厨房用具";
            }
        }
        String name = productName != null ? productName.toLowerCase() : "";
        if (name.contains("手机") || name.contains("phone") || name.contains("iphone") || name.contains("小米") || name.contains("华为")) {
            return "手机";
        }
        if (name.contains("电脑") || name.contains("笔记本")) {
            return "电脑";
        }
        if (name.contains("衣") || name.contains("裤") || name.contains("鞋")) {
            return "服装";
        }
        return "通用商品";
    }

    private Map<String, Object> getDefaultReviews(String productName) {
        List<Map<String, Object>> reviews = new ArrayList<>();
        String[] users = {"用户***8", "购物达人", "老顾客", "新用户***6", "会员用户"};
        String[] contents = {
            productName + "质量很好，包装精美，物流也很快，非常满意这次购物体验！",
            "收到货了，" + productName + "和描述一致，做工精细，性价比很高，推荐购买。",
            productName + "整体不错，就是发货稍微慢了点，不过商品质量没问题。",
            "第二次购买了，" + productName + "一如既往的好，客服态度也很好，好评！",
            "非常满意！" + productName + "超出预期，物超所值，会推荐给朋友。"
        };
        int[] ratings = {5, 5, 4, 5, 5};
        
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> review = new HashMap<>();
            review.put("user", users[i]);
            review.put("rating", "⭐".repeat(ratings[i]));
            review.put("content", contents[i]);
            review.put("time", today.minusDays(i).toString());
            reviews.add(review);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviews);
        return result;
    }

    public String analyzeSalesData(String month, List<Map<String, Object>> dailyData, Map<String, Object> summary) {
        StringBuilder dataDesc = new StringBuilder();
        dataDesc.append("月份：").append(month).append("\n");
        
        if (summary != null) {
            dataDesc.append("总销售额：").append(summary.get("totalAmount")).append("\n");
            dataDesc.append("总订单数：").append(summary.get("totalOrders")).append("\n");
            dataDesc.append("日均销售额：").append(summary.get("avgAmount")).append("\n");
        }
        
        if (dailyData != null && !dailyData.isEmpty()) {
            dataDesc.append("\n每日数据：\n");
            for (Map<String, Object> day : dailyData) {
                dataDesc.append(day.get("date")).append(": 销售额").append(day.get("amount"))
                        .append(", 订单").append(day.get("orderCount")).append("单\n");
            }
        }

        String prompt = String.format("""
            请分析以下电商销售数据，给出专业的数据分析报告：
            
            %s
            
            请从以下几个方面进行分析：
            1. 销售趋势分析：分析本月销售走势，识别高峰和低谷
            2. 异常排查：是否有异常数据（如突然暴增或暴跌）
            3. 运营建议：基于数据给出电商运营建议
            
            请用HTML格式返回报告，使用<h4>作为标题，<p>作为段落。
            """, dataDesc.toString());

        try {
            String apiKey = getApiKey();

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的电商数据分析师，擅长分析销售数据并给出运营建议。");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            String aiResponse = aiHttpClient.callDeepSeekApi(apiKey, deepseekApiUrl, deepseekModel, messages);

            if (aiResponse != null) {
                return aiResponse;
            }

            return generateDefaultAnalysisReport(month, summary);
        } catch (Exception e) {
            e.printStackTrace();
            return generateDefaultAnalysisReport(month, summary);
        }
    }

    private String generateDefaultAnalysisReport(String month, Map<String, Object> summary) {
        double totalAmount = summary != null && summary.get("totalAmount") != null ? 
            Double.parseDouble(summary.get("totalAmount").toString()) : 0;
        int totalOrders = summary != null && summary.get("totalOrders") != null ? 
            Integer.parseInt(summary.get("totalOrders").toString()) : 0;
        double avgAmount = summary != null && summary.get("avgAmount") != null ? 
            Double.parseDouble(summary.get("avgAmount").toString()) : 0;
        
        return String.format("""
            <h4>📊 %s 销售数据分析报告</h4>
            <p><strong>总销售额：</strong>¥%.2f</p>
            <p><strong>总订单数：</strong>%d 单</p>
            <p><strong>日均销售额：</strong>¥%.2f</p>
            <p><strong>趋势分析：</strong>本月销售%s，建议关注高峰时段的促销活动。</p>
            <p><strong>运营建议：</strong>建议优化商品推荐算法，提升客单价；加强用户粘性，提高复购率。</p>
            """, month, totalAmount, totalOrders, avgAmount, 
            totalAmount > 100000 ? "表现良好" : "有待提升");
    }
}