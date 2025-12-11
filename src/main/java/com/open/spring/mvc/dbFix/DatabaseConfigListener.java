package com.open.spring.mvc.dbFix;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
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
    public DataSource dataSource() {
        // Determine driver class name based on URL
        String driverClassName;
        if (datasourceUrl != null && datasourceUrl.startsWith("jdbc:mysql")) {
            driverClassName = "com.mysql.cj.jdbc.Driver";
        } else {
            // Default to SQLite
            driverClassName = "org.sqlite.JDBC";
        }
        
        // Build DataSource with correct driver
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.driverClassName(driverClassName);
        builder.url(datasourceUrl);
        builder.username(datasourceUsername);
        builder.password(datasourcePassword);
        
        // Configure HikariCP properties for MySQL (SQLite doesn't use connection pooling the same way)
        if (datasourceUrl != null && datasourceUrl.startsWith("jdbc:mysql")) {
            HikariDataSource dataSource = (HikariDataSource) builder.build();
            dataSource.setConnectionTimeout(60000);
            dataSource.setMaximumPoolSize(5);
            dataSource.setMinimumIdle(5);
            dataSource.setValidationTimeout(3000);
            dataSource.setMaxLifetime(1800000);
            return dataSource;
        }
        
        return builder.build();
    }
    
    @Bean
    @Primary
    public JpaProperties jpaProperties() {
        JpaProperties properties = new JpaProperties();
        
        Map<String, String> hibernateProps = new HashMap<>();
        
        boolean isMySQL = datasourceUrl != null && datasourceUrl.startsWith("jdbc:mysql");
        
        if (isMySQL) {
            // MySQL configuration
            // Use update to auto-create tables if they don't exist (safe - only creates, doesn't drop)
            hibernateProps.put("hibernate.hbm2ddl.auto", "update");
            // Configure Hibernate to automatically quote identifiers for MySQL (handles reserved keywords like 'groups')
            hibernateProps.put("hibernate.globally_quoted_identifiers", "true");
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