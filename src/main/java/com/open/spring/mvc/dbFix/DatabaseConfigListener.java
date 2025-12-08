package com.open.spring.mvc.dbFix;

import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfigListener {
    
    @Bean
    public JpaProperties jpaProperties() {
        JpaProperties properties = new JpaProperties();
        
        File dbFile = new File("volumes/sqlite.db");
        
        Map<String, String> hibernateProps = new HashMap<>();
        
        if (dbFile.exists()) {
            hibernateProps.put("hibernate.hbm2ddl.auto", "none");
        } else {
            hibernateProps.put("hibernate.hbm2ddl.auto", "update");
        }
        
        properties.setProperties(hibernateProps);
        
        return properties;
    }
}