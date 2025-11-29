package com.example.demo.common.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void add(String token, long ttlMillis) {
        long expireTime = System.currentTimeMillis() + ttlMillis;
        blacklist.put(token, expireTime);
    }

    public boolean contains(String token) {
        Long expireTime = blacklist.get(token);
        if (expireTime == null) {
            return false;
        }

        if (expireTime < System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }

        return true;
    }

    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟清理一次
    public void cleanExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < currentTime);
    }
//    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();
//
//    /**
//     * 添加token到黑名单
//     * @param token token字符串
//     * @param ttlMillis token剩余有效时间（毫秒）
//     */
//    public void add(String token, long ttlMillis) {
//        long expireTime = System.currentTimeMillis() + ttlMillis;
//        blacklist.put(token, expireTime);
//    }
//
//    /**
//     * 检查token是否在黑名单中
//     * @param token token字符串
//     * @return 是否在黑名单中
//     */
//    public boolean contains(String token) {
//        // 先检查是否存在，如果存在再检查是否过期
//        Long expireTime = blacklist.get(token);
//        if (expireTime == null) {
//            return false;
//        }
//
//        // 如果已过期，移除并返回false
//        if (expireTime < System.currentTimeMillis()) {
//            blacklist.remove(token);
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * 从黑名单中移除token
//     * @param token token字符串
//     */
//    public void remove(String token) {
//        blacklist.remove(token);
//    }
//
//    /**
//     * 定期清理过期token，每30分钟执行一次
//     */
//    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟
//    public void cleanExpiredTokens() {
//        long currentTime = System.currentTimeMillis();
//        blacklist.entrySet().removeIf(entry -> entry.getValue() < currentTime);
//    }
//
//    /**
//     * 获取黑名单大小
//     * @return 黑名单中的token数量
//     */
//    public int size() {
//        return blacklist.size();
//    }
}