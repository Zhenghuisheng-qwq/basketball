package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 结算截图识别。走百炼 OpenAI 兼容接口，换模型时只改 base-url 和 model。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cnpa.stats-vision")
public class StatsVisionProperties {

    private boolean enabled = true;

    /** 不含 /chat/completions 的根地址。 */
    private String baseUrl = "https://ws-sauyaoq4apm7zyhc.cn-beijing.maas.aliyuncs.com/compatible-mode/v1";

    private String apiKey = "";

    private String model = "qwen3.7-plus";

    private int timeoutSeconds = 120;

    /** 双方技术统计的 JSON 很长，不能沿用文案润色的 180。 */
    private int maxTokens = 4096;
}
