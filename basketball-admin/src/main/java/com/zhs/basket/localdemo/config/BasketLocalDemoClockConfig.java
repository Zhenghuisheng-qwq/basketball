package com.zhs.basket.localdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("local")
public class BasketLocalDemoClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
