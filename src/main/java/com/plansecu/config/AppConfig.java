package com.plansecu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {

    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    /** Horloge injectable (facilite les tests). */
    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
