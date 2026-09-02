package com.ecommerce.modules.recommend.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekApiUtil {

    public List<Map<String, Object>> getRecommendProducts(String userBehavior) {
        // 模拟推荐结果，不依赖外部库
        List<Map<String, Object>> recommendProducts = new ArrayList<>();
        
        try {
            // 模拟生成推荐商品
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", i);
                product.put("name", "推荐商品" + i);
                product.put("price", 100 + i * 10);
                product.put("image", "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=product%20" + i + "&image_size=square");
                recommendProducts.add(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return recommendProducts;
    }

}
