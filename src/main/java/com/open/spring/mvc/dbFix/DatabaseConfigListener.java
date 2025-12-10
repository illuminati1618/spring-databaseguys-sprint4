package com.open.spring.mvc.dbFix;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
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
    
    @Value("${spring.datasource.username}")
    private String datasourceUsername;
    
    @Value("${spring.datasource.password}")
    private String datasourcePassword;
    
    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        
        // Set all properties explicitly
        properties.setUrl(datasourceUrl);
        properties.setUsername(datasourceUsername);
        properties.setPassword(datasourcePassword);
        
        // Set driver class name based on URL
        if (datasourceUrl != null && datasourceUrl.startsWith("jdbc:mysql")) {
            properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            // Default to SQLite
            properties.setDriverClassName("org.sqlite.JDBC");
        }
        
        return properties;
    }
    
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