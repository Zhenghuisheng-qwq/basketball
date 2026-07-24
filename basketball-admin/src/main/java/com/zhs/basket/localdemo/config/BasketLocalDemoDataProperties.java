package com.zhs.basket.localdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地演示数据初始化配置，仅 local Profile 加载。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cnpa.local-demo")
public class BasketLocalDemoDataProperties {

    /** 是否启用本地演示数据接口 */
    private boolean enabled = false;

    /** 演示数据版本标识，用于幂等与安全清理 */
    private String marker = "CNPA_LOCAL_DEMO_V1";

    /** 演示账号明文密码，来自环境变量 CNPA_LOCAL_DEMO_PASSWORD */
    private String defaultPassword = "";
}
