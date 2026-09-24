package com.zhs.config;

/**
 * Generated CRUD path matcher. Block generator endpoints only.
 */
public final class GeneratedCrudPaths {

    private GeneratedCrudPaths() {
    }

    public static boolean isGeneratedCrud(String requestUri) {
        String path = normalizePath(requestUri);
        if (path == null || !path.startsWith("/basket/")) {
            return false;
        }
        return path.endsWith("/getByPage")
                || path.endsWith("/getById")
                || path.contains("/getById/")
                || path.endsWith("/addOrUpdate")
                || path.contains("/delete/")
                || path.endsWith("/deleteByIds");
    }

    static String normalizePath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return null;
        }
        String path = requestUri.trim();
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}