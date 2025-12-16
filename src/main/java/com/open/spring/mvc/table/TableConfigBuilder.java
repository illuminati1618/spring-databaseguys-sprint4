package com.open.spring.mvc.table;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Automatically builds TableConfig from entity classes using reflection
 */
public class TableConfigBuilder {
    
    /**
     * Auto-generate table config from entity class
     * Discovers all fields, determines types, sets smart defaults
     * 
     * Usage:
     *   TableConfig config = TableConfigBuilder.fromEntity(Game.class)
     *       .withEntityName("game")
     *       .withPaths("/game/edit", "/game/delete")
     *       .build();
     */
    public static Builder fromEntity(Class<?> entityClass) {
        return new Builder(entityClass);
    }
    
    public static class Builder {
        private Class<?> entityClass;
        private String entityName;
        private String entityDisplayName;
        private String entityDisplayNamePlural;
        private String tableId;
        private String updatePath;
        private String deletePath;
        private String updateRolesPath;
        private String createNewPath;
        private String createNewLabel;
        private boolean hasImportExport = true;
        private int maxVisibleColumns = 5; // Show first 5 columns by default
        private Set<String> excludeFields = new HashSet<>();
        private Map<String, TableColumn.ColumnType> fieldTypeOverrides = new HashMap<>();
        
        public Builder(Class<?> entityClass) {
            this.entityClass = entityClass;
            
            // Auto-generate entity name from class name
            String className = entityClass.getSimpleName();
            this.entityName = className.toLowerCase();
            this.entityDisplayName = className;
            this.entityDisplayNamePlural = className + "s"; // Simple pluralization
            this.tableId = this.entityName + "Table";
        }
        
        public Builder withEntityName(String name) {
            this.entityName = name;
            return this;
        }
        
        public Builder withDisplayNames(String singular, String plural) {
            this.entityDisplayName = singular;
            this.entityDisplayNamePlural = plural;
            return this;
        }
        
        public Builder withTableId(String tableId) {
            this.tableId = tableId;
            return this;
        }
        
        public Builder withPaths(String updatePath, String deletePath) {
            this.updatePath = updatePath;
            this.deletePath = deletePath;
            return this;
        }
        
        public Builder withUpdateRolesPath(String path) {
            this.updateRolesPath = path;
            return this;
        }
        
        public Builder withCreateNew(String path, String label) {
            this.createNewPath = path;
            this.createNewLabel = label;
            return this;
        }
        
        public Builder withImportExport(boolean enabled) {
            this.hasImportExport = enabled;
            return this;
        }
        
        public Builder withMaxVisibleColumns(int max) {
            this.maxVisibleColumns = max;
            return this;
        }
        
        public Builder excludeField(String fieldName) {
            this.excludeFields.add(fieldName);
            return this;
        }
        
        public Builder setFieldType(String fieldName, TableColumn.ColumnType type) {
            this.fieldTypeOverrides.put(fieldName, type);
            return this;
        }
        
        public TableConfig build() {
            TableConfig config = new TableConfig(
                entityName,
                entityDisplayName,
                entityDisplayNamePlural,
                tableId
            );
            
            // Auto-discover fields from entity class
            List<TableColumn> columns = discoverColumns();
            
            // Add auto-discovered columns
            for (TableColumn column : columns) {
                config.addColumn(column);
            }
            
            // Add action column
            config.addColumn(TableColumn.actions("Action"));
            
            // Add import/export column if enabled
            if (hasImportExport) {
                config.addColumn(TableColumn.importExport());
            }
            
            // Set paths
            config.setUpdatePath(updatePath);
            config.setDeletePath(deletePath);
            config.setUpdateRolesPath(updateRolesPath);
            config.setCreateNewPath(createNewPath);
            config.setCreateNewLabel(createNewLabel);
            config.setHasImportExport(hasImportExport);
            
            return config;
        }
        
        private List<TableColumn> discoverColumns() {
            List<TableColumn> columns = new ArrayList<>();
            int visibleCount = 0;
            
            // Get all declared fields from the class and its superclasses
            List<Field> allFields = getAllFields(entityClass);
            
            for (Field field : allFields) {
                // Skip transient, static, or excluded fields
                if (shouldSkipField(field)) {
                    continue;
                }
                
                String fieldName = field.getName();
                String label = generateLabel(fieldName);
                boolean visible = visibleCount < maxVisibleColumns;
                
                // Determine column type
                TableColumn.ColumnType columnType = determineColumnType(field);
                
                // Check for overrides
                if (fieldTypeOverrides.containsKey(fieldName)) {
                    columnType = fieldTypeOverrides.get(fieldName);
                }
                
                TableColumn column = new TableColumn(fieldName, label, visible, columnType);
                
                // Special handling for links
                if (fieldName.equals("uid")) {
                    column.setLinkPrefix("https://github.com/");
                    column.setType(TableColumn.ColumnType.LINK);
                }
                
                columns.add(column);
                
                if (visible) {
                    visibleCount++;
                }
            }
            
            return columns;
        }
        
        private List<Field> getAllFields(Class<?> clazz) {
            List<Field> fields = new ArrayList<>();
            
            // Add fields from current class
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            
            // Add fields from superclass if exists
            if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
                fields.addAll(getAllFields(clazz.getSuperclass()));
            }
            
            return fields;
        }
        
        private boolean shouldSkipField(Field field) {
            // Skip if in exclude list
            if (excludeFields.contains(field.getName())) {
                return true;
            }
            
            // Skip static fields
            if (Modifier.isStatic(field.getModifiers())) {
                return true;
            }
            
            // Skip transient fields
            if (field.isAnnotationPresent(Transient.class)) {
                return true;
            }
            
            // Skip common framework fields
            String fieldName = field.getName();
            if (fieldName.equals("serialVersionUID")) {
                return true;
            }
            
            // Skip collections and complex relationships for now
            if (Collection.class.isAssignableFrom(field.getType()) || 
                Map.class.isAssignableFrom(field.getType())) {
                return true;
            }
            
            return false;
        }
        
        private TableColumn.ColumnType determineColumnType(Field field) {
            Class<?> type = field.getType();
            
            // Boolean → CHECKBOX
            if (type == Boolean.class || type == boolean.class) {
                return TableColumn.ColumnType.CHECKBOX;
            }
            
            // Numbers, Strings, Dates → TEXT
            if (type == String.class || 
                Number.class.isAssignableFrom(type) ||
                type.isPrimitive() ||
                type == LocalDate.class ||
                type == LocalDateTime.class ||
                type == Date.class) {
                return TableColumn.ColumnType.TEXT;
            }
            
            // Default to TEXT
            return TableColumn.ColumnType.TEXT;
        }
        
        private String generateLabel(String fieldName) {
            // Convert camelCase to Title Case
            // personId → Person ID
            // createdAt → Created At
            StringBuilder label = new StringBuilder();
            
            for (int i = 0; i < fieldName.length(); i++) {
                char c = fieldName.charAt(i);
                
                if (i == 0) {
                    label.append(Character.toUpperCase(c));
                } else if (Character.isUpperCase(c)) {
                    label.append(' ').append(c);
                } else {
                    label.append(c);
                }
            }
            
            return label.toString();
        }
    }
}
