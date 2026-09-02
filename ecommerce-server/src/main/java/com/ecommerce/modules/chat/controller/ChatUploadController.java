package com.ecommerce.modules.chat.controller;

import com.ecommerce.common.result.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatUploadController {

    @PostMapping("/upload")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }

        try {
            String resourcePath = "D:/Desktop/ecommerce-platform/ecommerce-server/src/main/resources/static/chat_images";
            File dir = new File(resourcePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String newFilename = UUID.randomUUID().toString() + extension;
            
            File destFile = new File(dir, newFilename);
            file.transferTo(destFile);
            
            String imageUrl = "/chat_images/" + newFilename;
            
            return Result.success(java.util.Map.of("url", imageUrl));
            
        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }
}
