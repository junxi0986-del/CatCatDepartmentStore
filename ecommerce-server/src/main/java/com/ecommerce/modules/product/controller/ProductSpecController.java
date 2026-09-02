package com.ecommerce.modules.product.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.product.entity.ProductSpec;
import com.ecommerce.modules.product.mapper.ProductSpecMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product/spec")
public class ProductSpecController {

    @Resource
    private ProductSpecMapper productSpecMapper;

    @GetMapping("/list/{productId}")
    public Result getSpecsByProductId(@PathVariable Long productId) {
        try {
            List<ProductSpec> specs = productSpecMapper.selectByProductId(productId);
            return Result.success(specs);
        } catch (Exception e) {
            return Result.success(new java.util.ArrayList<>());
        }
    }

    @GetMapping("/names/{productId}")
    public Result getSpecNames(@PathVariable Long productId) {
        try {
            List<String> specNames = productSpecMapper.selectSpecNamesByProductId(productId);
            return Result.success(specNames);
        } catch (Exception e) {
            return Result.success(new java.util.ArrayList<>());
        }
    }

    @PostMapping("/add")
    public Result addSpec(@RequestBody ProductSpec spec) {
        try {
            productSpecMapper.insert(spec);
            return Result.success("规格添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "规格添加失败");
        }
    }

    @PostMapping("/update")
    public Result updateSpec(@RequestBody ProductSpec spec) {
        try {
            productSpecMapper.update(spec);
            return Result.success("规格更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "规格更新失败");
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteSpec(@PathVariable Long id) {
        try {
            productSpecMapper.deleteById(id);
            return Result.success("规格删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "规格删除失败");
        }
    }

    @DeleteMapping("/deleteAll/{productId}")
    public Result deleteAllSpecs(@PathVariable Long productId) {
        try {
            productSpecMapper.deleteByProductId(productId);
            return Result.success("所有规格删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "删除失败");
        }
    }

    @PostMapping("/batchAdd")
    public Result batchAddSpecs(@RequestBody List<ProductSpec> specs) {
        try {
            for (ProductSpec spec : specs) {
                productSpecMapper.insert(spec);
            }
            return Result.success("批量规格添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "批量添加失败");
        }
    }

    @PostMapping("/syncStock/{productId}")
    public Result syncStock(@PathVariable Long productId, @RequestParam Integer stock) {
        try {
            List<ProductSpec> specs = productSpecMapper.selectByProductId(productId);
            for (ProductSpec spec : specs) {
                spec.setStock(stock);
                productSpecMapper.update(spec);
            }
            return Result.success("库存同步成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "库存同步失败");
        }
    }
}
