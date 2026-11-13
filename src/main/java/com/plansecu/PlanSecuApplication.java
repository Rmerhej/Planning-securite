package com.plansecu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlanSecuApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
        Locale.setDefault(Locale.FRANCE);
        SpringApplication.run(PlanSecuApplication.class, args);
    }
}
