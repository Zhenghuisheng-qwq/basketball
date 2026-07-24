package com.zhs.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 腾讯云 COS 配置。
 */
@Component
@ConfigurationProperties(prefix = "tencent.cos")
public class TencentCosProperties {

    private String secretId = "";

    private String secretKey = "";

    /** 地域，如 ap-shanghai */
    private String region = "ap-shanghai";

    /** 存储桶名称 */
    private String bucket = "";

    /** 访问域名（不含协议） */
    private String domain = "";

    private boolean https = true;

    /** 对象键前缀 */
    private String keyPrefix = "basketball";

    public boolean isConfigured() {
        return StringUtils.hasText(secretId)
                && StringUtils.hasText(secretKey)
                && StringUtils.hasText(region)
                && StringUtils.hasText(bucket)
                && StringUtils.hasText(domain);
    }

    public String buildAccessUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        String scheme = https ? "https" : "http";
        String normalizedDomain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return scheme + "://" + normalizedDomain + "/" + key;
    }

    public String normalizeKeyPrefix() {
        if (!StringUtils.hasText(keyPrefix)) {
            return "";
        }
        String prefix = keyPrefix.trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
