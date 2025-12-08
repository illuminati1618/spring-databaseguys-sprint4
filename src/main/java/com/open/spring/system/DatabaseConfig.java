package com.open.spring.system;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Helper class to detect and work with different database types (SQLite vs MySQL).
 * Provides utilities to determine which database is being used and handle
 * database-specific SQL syntax differences.
 */
@Component
public class DatabaseConfig {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private Environment environment;
    
    /**
     * Detects if the current database is MySQL.
     * Checks the datasource URL or database product name.
     * 
     * @return true if using MySQL, false if using SQLite or other
     */
    public boolean isMySQL() {
        try {
            // Check environment variable first
            String dbType = environment.getProperty("DB_TYPE");
            if ("mysql".equalsIgnoreCase(dbType)) {
                return true;
            }
            
            // Check datasource URL
            String url = environment.getProperty("spring.datasource.url", "");
            if (url.contains("mysql") || url.contains("jdbc:mysql")) {
                return true;
            }
            
            // Check database metadata as fallback
            if (dataSource != null) {
                try (Connection conn = dataSource.getConnection()) {
                    DatabaseMetaData metaData = conn.getMetaData();
                    String productName = metaData.getDatabaseProductName();
                    if (productName != null && productName.toLowerCase().contains("mysql")) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            // If we can't determine, default to SQLite
            System.err.println("Warning: Could not determine database type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Detects if the current database is SQLite.
     * 
     * @return true if using SQLite, false otherwise
     */
    public boolean isSQLite() {
        return !isMySQL();
    }
    
    /**
     * Gets the appropriate primary key column definition based on database type.
     * 
     * @return SQL string for primary key column (e.g., "INTEGER PRIMARY KEY AUTOINCREMENT" for SQLite,
     *         "INT AUTO_INCREMENT PRIMARY KEY" for MySQL)
     */
    public String getPrimaryKeyDefinition() {
        if (isMySQL()) {
            return "id INT AUTO_INCREMENT PRIMARY KEY";
        } else {
            return "id INTEGER PRIMARY KEY AUTOINCREMENT";
        }
    }
    
    /**
     * Gets the appropriate default timestamp expression based on database type.
     * 
     * @return SQL string for default timestamp (e.g., "strftime('%Y-%m-%d %H:%M:%f','now')" for SQLite,
     *         "CURRENT_TIMESTAMP" for MySQL)
     */
    public String getDefaultTimestamp() {
        if (isMySQL()) {
            return "CURRENT_TIMESTAMP";
        } else {
            return "strftime('%Y-%m-%d %H:%M:%f','now')";
        }
    }
    
    /**
     * Gets the appropriate data type for TEXT columns.
     * 
     * @return "TEXT" for SQLite, "TEXT" for MySQL (both use TEXT)
     */
    public String getTextType() {
        return "TEXT";
    }
    
    /**
     * Gets the appropriate data type for INTEGER columns.
     * 
     * @return "INTEGER" for SQLite, "INT" for MySQL
     */
    public String getIntegerType() {
        if (isMySQL()) {
            return "INT";
        } else {
            return "INTEGER";
        }
    }
    
    /**
     * Gets the appropriate data type for REAL/FLOAT columns.
     * 
     * @return "REAL" for SQLite, "DOUBLE" for MySQL
     */
    public String getRealType() {
        if (isMySQL()) {
            return "DOUBLE";
        } else {
            return "REAL";
        }
    }
    
    /**
     * Gets the appropriate data type for DATETIME columns.
     * 
     * @return "DATETIME" for SQLite, "DATETIME" for MySQL (both use DATETIME)
     */
    public String getDateTimeType() {
        return "DATETIME";
    }
}
