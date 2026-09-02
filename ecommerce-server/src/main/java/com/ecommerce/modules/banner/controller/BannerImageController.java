package com.ecommerce.modules.banner.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class BannerImageController {

    private static final String BANNER_DIR = "D:/Desktop/ecommerce-platform/ecommerce-server/src/main/resources/static/Bannerimg/";

    @GetMapping("/Bannerimg/{filename:.+}")
    public ResponseEntity<Resource> getBannerImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(BANNER_DIR + filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = "image/png";
                String lowerName = filename.toLowerCase();
                if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (lowerName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (lowerName.endsWith(".webp")) {
                    contentType = "image/webp";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .header("Cross-Origin-Resource-Policy", "cross-origin")
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
