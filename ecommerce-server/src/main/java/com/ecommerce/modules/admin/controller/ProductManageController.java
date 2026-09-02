package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.product.entity.Product;
import com.ecommerce.modules.product.entity.ProductSpec;
import com.ecommerce.modules.product.mapper.ProductMapper;
import com.ecommerce.modules.product.mapper.ProductSpecMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/product")
public class ProductManageController {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductSpecMapper productSpecMapper;

    @GetMapping("/list")
    public Result getProductList(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) Integer categoryId,
                                 @RequestParam(required = false) Double minPrice,
                                 @RequestParam(required = false) Double maxPrice,
                                 @RequestParam(required = false) Integer minStock,
                                 @RequestParam(required = false) Integer maxStock,
                                 @RequestParam(required = false) Integer status) {
        List<Product> products;
        
        if (name != null || categoryId != null || minPrice != null || maxPrice != null ||
            minStock != null || maxStock != null || status != null) {
            products = productMapper.searchProducts(name, categoryId, minPrice, maxPrice, minStock, maxStock, status);
        } else if (keyword != null && !keyword.isEmpty()) {
            products = productMapper.selectByKeyword(keyword);
        } else {
            products = productMapper.selectAll();
        }
        return Result.success(products);
    }

    @PostMapping("/add")
    public Result addProduct(@RequestBody Product product) {
        productMapper.insert(product);
        return Result.success("添加成功");
    }

    @PostMapping("/update")
    public Result updateProduct(@RequestBody Product product) {
        Product oldProduct = productMapper.selectById(product.getId());
        productMapper.update(product);
        
        if (oldProduct != null && product.getStock() != null && !product.getStock().equals(oldProduct.getStock())) {
            List<ProductSpec> specs = productSpecMapper.selectByProductId(product.getId());
            for (ProductSpec spec : specs) {
                spec.setStock(product.getStock());
                productSpecMapper.update(spec);
            }
        }
        
        return Result.success("更新成功");
    }

    @PostMapping("/updateStatus")
    public Result updateProductStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        Object statusObj = params.get("status");
        Integer status;
        if (statusObj instanceof Integer) {
            status = (Integer) statusObj;
        } else {
            status = Integer.parseInt(statusObj.toString());
        }
        productMapper.updateStatus(id, status);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete")
    public Result deleteProduct(@RequestParam Long productId) {
        productMapper.delete(productId);
        return Result.success("删除成功");
    }

    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        try {
            String filePath = saveFile(file);
            return Result.success(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "上传失败");
        }
    }

    @PostMapping("/uploadMultiple")
    public Result uploadMultiple(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return Result.error(400, "请选择要上传的文件");
            }
            StringBuilder imageUrls = new StringBuilder();
            for (int i = 0; i < files.length; i++) {
                String filePath = saveFile(files[i]);
                if (i > 0) {
                    imageUrls.append(",");
                }
                imageUrls.append(filePath);
            }
            return Result.success(imageUrls.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "上传失败");
        }
    }

    @PostMapping("/uploadSingle")
    public Result uploadSingle(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error(400, "请选择要上传的文件");
            }
            String filePath = saveFile(file);
            return Result.success(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "上传失败");
        }
    }

    private String saveFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + ext;

        String projectRoot = "D:/Desktop/ecommerce-platform/ecommerce-server";
        String resourcePath = projectRoot + "/src/main/resources/static/images";
        File srcDir = new File(resourcePath);
        if (!srcDir.exists()) {
            srcDir.mkdirs();
        }
        File srcDest = new File(srcDir, fileName);
        
        String targetPath = projectRoot + "/target/classes/static/images";
        File targetDir = new File(targetPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File targetDest = new File(targetDir, fileName);
        
        file.transferTo(srcDest);
        java.nio.file.Files.copy(srcDest.toPath(), targetDest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        return "images/" + fileName;
    }

    @PostMapping("/batchUpdateStatus")
    public Result batchUpdateStatus(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Object> idList = (List<Object>) params.get("ids");
        Integer status = Integer.parseInt(params.get("status").toString());
        
        int successCount = 0;
        for (Object idObj : idList) {
            Long productId = Long.parseLong(idObj.toString());
            productMapper.updateStatus(productId, status);
            successCount++;
        }
        
        String statusText = status == 1 ? "上架" : "下架";
        return Result.success(String.format("批量%s完成，成功%d个", statusText, successCount));
    }

}
