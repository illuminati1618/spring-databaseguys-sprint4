package com.open.spring.mvc.table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for a single table column
 * Used by the auto-generation template system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableColumn {
    private String field;           // Entity field name (e.g., "id", "type")
    private String label;           // Display label (e.g., "ID", "Type")
    private boolean visible;        // Start visible or hidden
    private ColumnType type;        // How to render this column
    private String linkPrefix;      // For link type, URL prefix
    private String customFormat;    // For custom rendering
    
    public enum ColumnType {
        TEXT,           // Simple text display
        LINK,           // Clickable link
        IMAGE,          // Image (like profile picture)
        CHECKBOX,       // Boolean as checkbox image
        ACTIONS,        // Action buttons (edit/delete)
        IMPORT_EXPORT,  // Import/export controls
        CUSTOM          // Custom HTML
    }
    
    // Convenience constructors
    public TableColumn(String field, String label, boolean visible, ColumnType type) {
        this.field = field;
        this.label = label;
        this.visible = visible;
        this.type = type;
    }
    
    public TableColumn(String field, String label, ColumnType type) {
        this(field, label, true, type);
    }
    
    public static TableColumn text(String field, String label, boolean visible) {
        return new TableColumn(field, label, visible, ColumnType.TEXT);
    }
    
    public static TableColumn link(String field, String label, String linkPrefix, boolean visible) {
        TableColumn col = new TableColumn(field, label, visible, ColumnType.LINK);
        col.setLinkPrefix(linkPrefix);
        return col;
    }
    
    public static TableColumn checkbox(String field, String label, boolean visible) {
        return new TableColumn(field, label, visible, ColumnType.CHECKBOX);
    }
    
    public static TableColumn actions(String label) {
        return new TableColumn("actions", label, true, ColumnType.ACTIONS);
    }
    
    public static TableColumn importExport() {
        return new TableColumn("importExport", "Import/Export", false, ColumnType.IMPORT_EXPORT);
    }
}
