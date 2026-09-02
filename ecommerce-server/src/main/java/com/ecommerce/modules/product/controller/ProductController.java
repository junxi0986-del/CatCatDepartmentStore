package com.ecommerce.modules.product.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.product.entity.Product;
import com.ecommerce.modules.product.entity.ProductSpec;
import com.ecommerce.modules.product.mapper.ProductMapper;
import com.ecommerce.modules.product.mapper.ProductSpecMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSpecMapper productSpecMapper;

    @GetMapping("/list")
    public Result getProductList(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId) {
        
        List<Product> products = productMapper.selectAll();
        
        // 只显示上架商品（status = 1）
        products = products.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .toList();
        
        // 按分类筛选
        if (categoryId != null && !categoryId.isEmpty() && !"0".equals(categoryId)) {
            try {
                Long catId = Long.parseLong(categoryId);
                products = products.stream()
                        .filter(p -> p.getCategoryId() != null && p.getCategoryId().equals(catId))
                        .toList();
            } catch (NumberFormatException e) {
                // categoryId 不是有效数字，忽略
            }
        }
        
        // 按关键词筛选
        if (keyword != null && !keyword.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(lowerKeyword))
                    .toList();
        }
        
        // 分页处理
        int total = products.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<Product> pageProducts;
        if (start >= total) {
            pageProducts = new ArrayList<>();
        } else {
            pageProducts = products.subList(start, end);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageProducts);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result getProductDetail(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        // 检查商品是否上架
        if (product.getStatus() == null || product.getStatus() != 1) {
            return Result.error(404, "商品不存在或已下架");
        }
        return Result.success(product);
    }

    @GetMapping("/specs/{productId}")
    public Result getProductSpecs(@PathVariable Long productId) {
        try {
            List<ProductSpec> specs = productSpecMapper.selectByProductId(productId);
            return Result.success(specs);
        } catch (Exception e) {
            // 如果数据库中不存在product_spec表，返回空列表
            return Result.success(new ArrayList<>());
        }
    }

    @GetMapping("/category/{categoryId}")
    public Result getProductsByCategory(@PathVariable Long categoryId) {
        // 这里可以添加按分类查询的逻辑
        List<Product> products = productMapper.selectAll();
        // 过滤出指定分类的商品
        List<Product> filteredProducts = new ArrayList<>();
        for (Product product : products) {
            if (product.getCategoryId().equals(categoryId)) {
                filteredProducts.add(product);
            }
        }
        return Result.success(filteredProducts);
    }

}
