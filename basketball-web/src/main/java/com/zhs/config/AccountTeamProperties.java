package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "team")
public class AccountTeamProperties {

    private Invitation invitation = new Invitation();

    private int defaultMaxMemberCount = 20;

    @Data
    public static class Invitation {
        private int expireDays = 7;
    }
}
