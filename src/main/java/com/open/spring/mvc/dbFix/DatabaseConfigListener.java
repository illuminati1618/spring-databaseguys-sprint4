package com.open.spring.mvc.dbFix;

import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfigListener {
    
    private final Environment environment;
    
    public DatabaseConfigListener(Environment environment) {
        this.environment = environment;
    }
    
    @Bean
    public JpaProperties jpaProperties() {
        JpaProperties properties = new JpaProperties();
        
        // Only apply SQLite-specific configuration if not in production profile
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = false;
        for (String profile : activeProfiles) {
            if ("prod".equals(profile)) {
                isProduction = true;
                break;
            }
        }
        
        // Skip SQLite-specific configuration in production
        if (!isProduction) {
            File dbFile = new File("volumes/sqlite.db");
            
            Map<String, String> hibernateProps = new HashMap<>();
            
            if (dbFile.exists()) {
                hibernateProps.put("hibernate.hbm2ddl.auto", "none");
            } else {
                hibernateProps.put("hibernate.hbm2ddl.auto", "update");
            }
            
            properties.setProperties(hibernateProps);
        }
        // In production, let application-prod.properties handle the configuration
        
        return properties;
    }
}