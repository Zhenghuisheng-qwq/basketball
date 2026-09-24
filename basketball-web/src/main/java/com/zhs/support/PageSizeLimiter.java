package com.zhs.support;

public final class PageSizeLimiter {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PageSizeLimiter() {
    }

    public static int clamp(Integer pageSize) {
        return clamp(pageSize, DEFAULT_SIZE, MAX_SIZE);
    }

    public static int clamp(Integer pageSize, int defaultSize, int maxSize) {
        if (pageSize == null || pageSize < 1) {
            return defaultSize;
        }
        return Math.min(pageSize, maxSize);
    }
}