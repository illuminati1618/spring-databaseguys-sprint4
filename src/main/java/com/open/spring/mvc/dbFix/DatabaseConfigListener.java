package com.open.spring.mvc.dbFix;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfigListener {
    
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    
    @Bean
    @Primary
    public JpaProperties jpaProperties() {
        JpaProperties properties = new JpaProperties();
        
        Map<String, String> hibernateProps = new HashMap<>();
        
        boolean isMySQL = datasourceUrl != null && datasourceUrl.startsWith("jdbc:mysql");
        
        if (isMySQL) {
            // MySQL configuration
            hibernateProps.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            hibernateProps.put("hibernate.hbm2ddl.auto", "none"); // Use none for production MySQL
        } else {
            // SQLite configuration (default, including when datasourceUrl is null/undefined)
            hibernateProps.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
            
            // Check if SQLite database file exists
            File dbFile = new File("volumes/sqlite.db");
            if (dbFile.exists()) {
                hibernateProps.put("hibernate.hbm2ddl.auto", "none");
            } else {
                hibernateProps.put("hibernate.hbm2ddl.auto", "update");
            }
        }
        
        properties.setProperties(hibernateProps);
        
        return properties;
    }
}