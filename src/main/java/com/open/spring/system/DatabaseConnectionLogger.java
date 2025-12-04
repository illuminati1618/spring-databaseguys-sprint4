package com.open.spring.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Configuration
@Order(1)
public class DatabaseConnectionLogger implements CommandLineRunner {

    @Value("${spring.datasource.url:not-set}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:not-set}")
    private String datasourceUsername;

    @Value("${spring.datasource.driver-class-name:not-set}")
    private String driverClassName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private final DataSource dataSource;

    public DatabaseConnectionLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("DATABASE CONNECTION DEBUG INFO");
        System.out.println("========================================");
        System.out.println("Active Profile: " + activeProfile);
        System.out.println("Driver Class: " + driverClassName);
        System.out.println("Database URL: " + datasourceUrl);
        System.out.println("Database Username: " + datasourceUsername);
        System.out.println("Database Password: " + (datasourceUsername.equals("not-set") ? "not-set" : "***"));
        System.out.println("========================================");
        
        try {
            System.out.println("Attempting database connection...");
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                System.out.println("✓ Connection successful!");
                System.out.println("Database Product: " + metaData.getDatabaseProductName());
                System.out.println("Database Version: " + metaData.getDatabaseProductVersion());
                System.out.println("Driver Name: " + metaData.getDriverName());
                System.out.println("Driver Version: " + metaData.getDriverVersion());
                System.out.println("URL: " + metaData.getURL());
                System.out.println("Username: " + metaData.getUserName());
            }
        } catch (Exception e) {
            System.err.println("✗ Connection failed!");
            System.err.println("Error: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("========================================");
    }
}

