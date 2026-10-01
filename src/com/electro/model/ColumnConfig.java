package com.electro.model;

/**
 * Represents the configuration, display details, and metadata of a table column.
 * Supports full CRUD operations (Create, Read, Update, Delete) by Administrators.
 */
public class ColumnConfig {
    private String key;          // Internal unique identifier (e.g. "ID", "SKU", "Brand", "Product Name", "Color")
    private String displayName;  // Admin's chosen display title (e.g. "Item Code", "Barcode", "Device Color")
    private String description;  // Purpose or explanation of the column
    private boolean system;      // true if core system column, false if custom created by Admin
    private boolean visible;     // true if displayed in the table, false if hidden

    public ColumnConfig() {
        this.visible = true;
        this.system = false;
        this.description = "";
    }

    public ColumnConfig(String key, String displayName, String description, boolean system, boolean visible) {
        this.key = key != null ? key.trim() : "";
        this.displayName = displayName != null ? displayName.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.system = system;
        this.visible = visible;
    }

    public String getKey() {
        return key != null ? key : "";
    }

    public void setKey(String key) {
        this.key = key != null ? key.trim() : "";
    }

    public String getDisplayName() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }
        return getKey();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName.trim() : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + getKey() + ")" + (system ? " [System]" : " [Custom]");
    }
}
