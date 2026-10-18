package com.zhs.framework.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;

/**
 * cnpa-e2e 启动前拒绝生产数据源和生产 Redis 隧道。其他 profile 不介入。
 */
public class CnpaE2eIsolationGuard implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("cnpa-e2e")) {
            return;
        }
        String jdbc = String.join(" ",
                value(environment, "spring.datasource.url"),
                value(environment, "spring.datasource.druid.master.url"));
        String redis = value(environment, "spring.data.redis.host") + ":" + value(environment, "spring.data.redis.port");
        if (jdbc.contains("111.229.241.29")) {
            throw new IllegalStateException("cnpa-e2e 拒绝启动：数据源指向生产 MySQL 111.229.241.29");
        }
        if (redis.endsWith(":16379")) {
            throw new IllegalStateException("cnpa-e2e 拒绝启动：Redis 指向生产隧道 16379");
        }
    }

    private static String value(ConfigurableEnvironment environment, String key) {
        String raw = environment.getProperty(key);
        return raw == null ? "" : raw;
    }
}
