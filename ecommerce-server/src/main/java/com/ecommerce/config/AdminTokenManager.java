package com.ecommerce.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdminTokenManager {

    private static final Map<String, String> tokenAdminMap = new ConcurrentHashMap<>();

    public static void put(String token, String adminId) {
        tokenAdminMap.put(token, adminId);
    }

    public static String getAdminId(String token) {
        return tokenAdminMap.get(token);
    }

    public static void remove(String token) {
        tokenAdminMap.remove(token);
    }

    public static boolean contains(String token) {
        return tokenAdminMap.containsKey(token);
    }
}
