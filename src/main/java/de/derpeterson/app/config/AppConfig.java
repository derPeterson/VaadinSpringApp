package de.derpeterson.app.config;

import de.derpeterson.app.i18n.CustomI18NProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public CustomI18NProvider customI18NProvider() {
        return new CustomI18NProvider();
    }
}