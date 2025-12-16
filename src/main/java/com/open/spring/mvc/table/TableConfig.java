package com.open.spring.mvc.table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete configuration for an auto-generated entity table
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableConfig {
    private String entityName;              // e.g., "game", "person"
    private String entityDisplayName;       // e.g., "Game", "Person"
    private String entityDisplayNamePlural; // e.g., "Games", "People"
    private String tableId;                 // HTML table ID
    private List<TableColumn> columns;      // Column configurations
    private boolean hasImportExport;        // Enable import/export
    private String updatePath;              // Path for update button
    private String deletePath;              // Path for delete button
    private String updateRolesPath;         // Optional: path for update roles
    private String createNewPath;           // Path for "Create New" button
    private String createNewLabel;          // Label for create button
    
    public TableConfig(String entityName, String entityDisplayName, String entityDisplayNamePlural, String tableId) {
        this.entityName = entityName;
        this.entityDisplayName = entityDisplayName;
        this.entityDisplayNamePlural = entityDisplayNamePlural;
        this.tableId = tableId;
        this.columns = new ArrayList<>();
        this.hasImportExport = true;
    }
    
    public TableConfig addColumn(TableColumn column) {
        this.columns.add(column);
        return this;
    }
    
    public TableConfig withPaths(String updatePath, String deletePath) {
        this.updatePath = updatePath;
        this.deletePath = deletePath;
        return this;
    }
    
    public TableConfig withCreateNew(String path, String label) {
        this.createNewPath = path;
        this.createNewLabel = label;
        return this;
    }
}
