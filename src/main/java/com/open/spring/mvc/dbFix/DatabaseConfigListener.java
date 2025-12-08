package com.open.spring.mvc.dbFix;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfigListener {
    
    @Autowired
    private Environment environment;
    
    @Bean
    public JpaProperties jpaProperties() {
        JpaProperties properties = new JpaProperties();
        
        Map<String, String> hibernateProps = new HashMap<>();
        
        // Check if ddl-auto is explicitly set via environment variable
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        
        if (ddlAuto != null && !ddlAuto.isEmpty()) {
            // Use explicitly set value
            hibernateProps.put("hibernate.hbm2ddl.auto", ddlAuto);
        } else {
            // Auto-detect based on database type by checking datasource URL
            String url = environment.getProperty("spring.datasource.url", "");
            boolean isMySQL = url.contains("mysql") || url.contains("jdbc:mysql");
            
            // Also check DB_TYPE environment variable
            String dbType = environment.getProperty("DB_TYPE");
            if (dbType != null && "mysql".equalsIgnoreCase(dbType)) {
                isMySQL = true;
            }
            
            if (isMySQL) {
                // MySQL: use update to auto-create tables from JPA entities
                hibernateProps.put("hibernate.hbm2ddl.auto", "update");
                System.out.println("Detected MySQL database - setting Hibernate DDL auto to 'update'");
            } else {
                // SQLite: use none (ModelInit handles table creation manually)
                hibernateProps.put("hibernate.hbm2ddl.auto", "none");
                System.out.println("Detected SQLite database - setting Hibernate DDL auto to 'none'");
            }
        }
        
        properties.setProperties(hibernateProps);
        
        return properties;
    }
}