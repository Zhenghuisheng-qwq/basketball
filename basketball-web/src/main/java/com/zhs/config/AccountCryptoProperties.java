package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "account.crypto")
public class AccountCryptoProperties {

    /** AES 密钥（Base64 或 UTF-8 原文，16/24/32 字节） */
    private String aesKey = "";
}
