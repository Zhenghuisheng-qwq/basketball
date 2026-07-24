package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "account.file")
public class AccountFileProperties {

    /** 存储类型：local / oss 等 */
    private String storageType = "local";

    /** 本地存储根目录 */
    private String localRoot = "./upload";

    /** 公开文件访问前缀（可选，为空则使用相对路径） */
    private String publicBaseUrl;

    /** 图片最大字节数 */
    private long maxImageSize = 5242880L;
}
