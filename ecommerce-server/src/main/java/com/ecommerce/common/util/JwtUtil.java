package com.ecommerce.common.util;

import java.util.Map;
import java.util.UUID;

public class JwtUtil {
    private static final String SECRET = "ecommerce-secret-key";
    private static final long EXPIRE = 86400000; // 24小时

    public static String generateToken(Map<String, Object> claims) {
        // 简单的 token 生成，实际项目中应该使用更安全的方式
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    public static Map<String, Object> parseToken(String token) {
        // 简单的 token 解析，实际项目中应该使用更安全的方式
        return null;
    }

}
