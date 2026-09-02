package com.ecommerce.modules.banner.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.common.result.Result;
import com.ecommerce.modules.banner.entity.Banner;
import com.ecommerce.modules.banner.mapper.BannerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/banner")
public class BannerController {

    @Autowired
    private BannerMapper bannerMapper;

    @GetMapping("/list")
    public Result getBannerList() {
        QueryWrapper<Banner> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order");
        List<Banner> banners = bannerMapper.selectList(wrapper);
        return Result.success(banners);
    }

    @GetMapping("/active")
    public Result getActiveBanners() {
        List<Banner> banners = bannerMapper.selectActiveBanners();
        return Result.success(banners);
    }

    @PostMapping("/add")
    public Result addBanner(@RequestBody Banner banner) {
        banner.setCreateTime(new Date());
        banner.setUpdateTime(new Date());
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        int result = bannerMapper.insert(banner);
        if (result > 0) {
            return Result.success(banner);
        } else {
            return Result.fail("添加失败");
        }
    }

    @PostMapping("/update")
    public Result updateBanner(@RequestBody Banner banner) {
        banner.setUpdateTime(new Date());
        int result = bannerMapper.updateById(banner);
        if (result > 0) {
            return Result.success("更新成功");
        } else {
            return Result.fail("更新失败");
        }
    }

    @DeleteMapping("/delete")
    public Result deleteBanner(@RequestParam String id) {
        Long bannerId = Long.parseLong(id);
        int result = bannerMapper.deleteById(bannerId);
        if (result > 0) {
            return Result.success("删除成功");
        } else {
            return Result.fail("删除失败");
        }
    }

    @PostMapping("/upload")
    public Result uploadBannerImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.fail("请选择要上传的文件");
            }
            
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + ext;

            String projectRoot = "D:/Desktop/ecommerce-platform/ecommerce-server";
            String resourcePath = projectRoot + "/src/main/resources/static/Bannerimg";
            File bannerDir = new File(resourcePath);
            if (!bannerDir.exists()) {
                bannerDir.mkdirs();
            }
            
            File destFile = new File(bannerDir, fileName);
            file.transferTo(destFile);
            
            String filePath = "/Bannerimg/" + fileName;
            return Result.success(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("上传失败");
        }
    }
}
