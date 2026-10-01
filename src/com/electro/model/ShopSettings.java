package com.electro.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global shop settings, tax policies, invoice header configuration, and system rules.
 */
public class ShopSettings {
    private String storeName;
    private String tagline;
    private String address;
    private String phone;
    private String email;
    private String gstin;
    private String currencySymbol;
    private double defaultTaxRate;
    private String invoiceFooter;
    private String termsAndConditions;

    // Advanced Administrative Controls
    private String invoicePrefix;
    private boolean enableGstBilling;
    private double wholesaleDiscountPercent;
    private int lowStockThreshold;
    private boolean strictSerialTracking;
    private int defaultWarrantyMonths;

    public ShopSettings() {
        this.storeName = "VoltVault Electronics Hub";
        this.tagline = "Premium Gadgets, Mobiles & Smart Appliances";
        this.address = "Shop #42, Electronic City, Main Road, Tech Hub";
        this.phone = "+91 98765 43210";
        this.email = "support@voltvault.example.com";
        this.gstin = "29ABCDE1234F1Z5";
        this.currencySymbol = "\u20B9"; // Default ₹ (Rupee)
        this.defaultTaxRate = 18.0;
        this.invoiceFooter = "Thank you for shopping at VoltVault Electronics!";
        this.termsAndConditions = "1. Goods once sold are covered by manufacturer warranty as stated.\n"
                + "2. Physical damage, water damage, or electrical surge voids warranty.\n"
                + "3. Original bill and Serial Number are mandatory for warranty claims.";

        this.invoicePrefix = "INV";
        this.enableGstBilling = true;
        this.wholesaleDiscountPercent = 8.0;
        this.lowStockThreshold = 4;
        this.strictSerialTracking = true;
        this.defaultWarrantyMonths = 12;
    }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public double getDefaultTaxRate() { return defaultTaxRate; }
    public void setDefaultTaxRate(double defaultTaxRate) { this.defaultTaxRate = defaultTaxRate; }

    public String getInvoiceFooter() { return invoiceFooter; }
    public void setInvoiceFooter(String invoiceFooter) { this.invoiceFooter = invoiceFooter; }

    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public String getInvoicePrefix() { return invoicePrefix; }
    public void setInvoicePrefix(String invoicePrefix) { this.invoicePrefix = invoicePrefix; }

    public boolean isEnableGstBilling() { return enableGstBilling; }
    public void setEnableGstBilling(boolean enableGstBilling) { this.enableGstBilling = enableGstBilling; }

    public double getWholesaleDiscountPercent() { return wholesaleDiscountPercent; }
    public void setWholesaleDiscountPercent(double wholesaleDiscountPercent) { this.wholesaleDiscountPercent = wholesaleDiscountPercent; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public boolean isStrictSerialTracking() { return strictSerialTracking; }
    public void setStrictSerialTracking(boolean strictSerialTracking) { this.strictSerialTracking = strictSerialTracking; }

    public int getDefaultWarrantyMonths() { return defaultWarrantyMonths; }
    public void setDefaultWarrantyMonths(int defaultWarrantyMonths) { this.defaultWarrantyMonths = defaultWarrantyMonths; }

    // --- CUSTOM COLUMN DISPLAY NAMES ---
    // --- COLUMN CONFIGURATIONS & CRUD OPERATIONS ---
    private Map<String, String> customColumnNames = new java.util.LinkedHashMap<>();
    private java.util.List<ColumnConfig> columnConfigs = new java.util.ArrayList<>();

    {
        initDefaultColumnConfigs();
    }

    public void initDefaultColumnConfigs() {
        columnConfigs.clear();
        columnConfigs.add(new ColumnConfig("ID", "ID", "Unique system identification code for product", true, true));
        columnConfigs.add(new ColumnConfig("SKU", "SKU", "Stock Keeping Unit or barcode scan number", true, true));
        columnConfigs.add(new ColumnConfig("Brand", "Brand", "Manufacturer or brand label", true, true));
        columnConfigs.add(new ColumnConfig("Product Name", "Product Name", "Item description and commercial product title", true, true));
        columnConfigs.add(new ColumnConfig("Category", "Category", "Product classification or department", true, true));
        columnConfigs.add(new ColumnConfig("Model", "Model", "Hardware model or variant number", true, true));
        columnConfigs.add(new ColumnConfig("Cost", "Cost", "Purchase acquisition cost (Admin only)", true, true));
        columnConfigs.add(new ColumnConfig("Retail", "Retail", "Retail selling price / Maximum Retail Price (MRP)", true, true));
        columnConfigs.add(new ColumnConfig("Wholesale", "Wholesale", "Wholesale bulk B2B discounted rate", true, true));
        columnConfigs.add(new ColumnConfig("GST", "GST", "Applicable Goods & Services Tax percentage", true, true));
        columnConfigs.add(new ColumnConfig("Stock", "Stock", "Current available quantity in warehouse / shop", true, true));
        columnConfigs.add(new ColumnConfig("Warranty", "Warranty", "Warranty coverage duration (Months)", true, true));
        columnConfigs.add(new ColumnConfig("Serial Req", "Serial Req", "Flag indicating unique IMEI / Serial tracking", true, true));
    }

    public java.util.List<ColumnConfig> getColumnConfigs() {
        if (columnConfigs == null || columnConfigs.isEmpty()) {
            initDefaultColumnConfigs();
        }
        return new java.util.ArrayList<>(columnConfigs);
    }

    public void setColumnConfigs(java.util.List<ColumnConfig> configs) {
        if (configs != null && !configs.isEmpty()) {
            this.columnConfigs = new java.util.ArrayList<>(configs);
            // Sync with customColumnNames
            syncCustomColumnNamesFromConfigs();
        } else {
            initDefaultColumnConfigs();
        }
    }

    public ColumnConfig getColumnConfig(String key) {
        if (key == null) return null;
        for (ColumnConfig c : getColumnConfigs()) {
            if (c.getKey().equalsIgnoreCase(key.trim())) {
                return c;
            }
        }
        // Check without spaces
        String clean = key.replaceAll("\\s+", "");
        for (ColumnConfig c : getColumnConfigs()) {
            if (c.getKey().replaceAll("\\s+", "").equalsIgnoreCase(clean)) {
                return c;
            }
        }
        return null;
    }

    // CREATE (Admin creates custom column)
    public boolean addColumnConfig(ColumnConfig config) {
        if (config == null || config.getKey() == null || config.getKey().trim().isEmpty()) {
            return false;
        }
        String key = config.getKey().trim();
        for (ColumnConfig existing : getColumnConfigs()) {
            if (existing.getKey().equalsIgnoreCase(key)) {
                return false; // Already exists
            }
        }
        config.setKey(key);
        if (config.getDisplayName() == null || config.getDisplayName().trim().isEmpty()) {
            config.setDisplayName(key);
        }
        columnConfigs.add(config);
        syncCustomColumnNamesFromConfigs();
        return true;
    }

    // UPDATE (Admin updates column display name, description, visibility)
    public boolean updateColumnConfig(String key, String newDisplayName, String newDescription, boolean visible) {
        ColumnConfig config = getColumnConfig(key);
        if (config == null) return false;

        if (newDisplayName != null && !newDisplayName.trim().isEmpty()) {
            config.setDisplayName(newDisplayName.trim());
        }
        if (newDescription != null) {
            config.setDescription(newDescription.trim());
        }
        config.setVisible(visible);
        syncCustomColumnNamesFromConfigs();
        return true;
    }

    // DELETE (Admin deletes custom column, or resets system column)
    public boolean deleteColumnConfig(String key) {
        if (key == null) return false;
        for (int i = 0; i < columnConfigs.size(); i++) {
            ColumnConfig c = columnConfigs.get(i);
            if (c.getKey().equalsIgnoreCase(key.trim()) || c.getKey().replaceAll("\\s+", "").equalsIgnoreCase(key.replaceAll("\\s+", ""))) {
                if (c.isSystem()) {
                    // System columns cannot be removed entirely, but reset to default
                    c.setDisplayName(c.getKey());
                    c.setVisible(true);
                    syncCustomColumnNamesFromConfigs();
                    return true;
                } else {
                    columnConfigs.remove(i);
                    syncCustomColumnNamesFromConfigs();
                    return true;
                }
            }
        }
        return false;
    }

    private void syncCustomColumnNamesFromConfigs() {
        if (customColumnNames == null) {
            customColumnNames = new java.util.LinkedHashMap<>();
        }
        customColumnNames.clear();
        for (ColumnConfig c : columnConfigs) {
            if (!c.getDisplayName().equalsIgnoreCase(c.getKey())) {
                customColumnNames.put(c.getKey(), c.getDisplayName());
            }
        }
    }

    public Map<String, String> getCustomColumnNames() {
        return new java.util.LinkedHashMap<>(customColumnNames);
    }

    public void setCustomColumnNames(Map<String, String> names) {
        this.customColumnNames = (names != null) ? new java.util.LinkedHashMap<>(names) : new java.util.LinkedHashMap<>();
        if (columnConfigs == null || columnConfigs.isEmpty()) {
            initDefaultColumnConfigs();
        }
        for (ColumnConfig c : columnConfigs) {
            if (customColumnNames.containsKey(c.getKey())) {
                c.setDisplayName(customColumnNames.get(c.getKey()));
            } else {
                String cleanKey = c.getKey().replaceAll("\\s+", "");
                for (Map.Entry<String, String> entry : customColumnNames.entrySet()) {
                    if (entry.getKey().replaceAll("\\s+", "").equalsIgnoreCase(cleanKey)) {
                        c.setDisplayName(entry.getValue());
                        break;
                    }
                }
            }
        }
    }

    public String getColumnDisplayName(String defaultName) {
        if (defaultName == null) return "";
        ColumnConfig cfg = getColumnConfig(defaultName);
        if (cfg != null && cfg.getDisplayName() != null && !cfg.getDisplayName().trim().isEmpty()) {
            return cfg.getDisplayName().trim();
        }
        if (customColumnNames != null) {
            if (customColumnNames.containsKey(defaultName)) {
                String val = customColumnNames.get(defaultName);
                if (val != null && !val.trim().isEmpty()) return val.trim();
            }
            String cleanDefault = defaultName.replaceAll("\\s+", "");
            for (Map.Entry<String, String> entry : customColumnNames.entrySet()) {
                if (entry.getKey().replaceAll("\\s+", "").equalsIgnoreCase(cleanDefault)) {
                    String val = entry.getValue();
                    if (val != null && !val.trim().isEmpty()) return val.trim();
                }
            }
        }
        return defaultName;
    }

    public void setColumnDisplayName(String defaultName, String displayName) {
        if (defaultName == null || defaultName.trim().isEmpty()) return;
        ColumnConfig cfg = getColumnConfig(defaultName);
        if (cfg != null) {
            cfg.setDisplayName(displayName != null && !displayName.trim().isEmpty() ? displayName.trim() : cfg.getKey());
        }
        if (customColumnNames == null) {
            customColumnNames = new java.util.LinkedHashMap<>();
        }
        if (displayName != null && !displayName.trim().isEmpty() && !displayName.trim().equalsIgnoreCase(defaultName.trim())) {
            customColumnNames.put(defaultName.trim(), displayName.trim());
        } else {
            customColumnNames.remove(defaultName.trim());
        }
    }

    public boolean hasCustomColumnName(String defaultName) {
        if (defaultName == null) return false;
        ColumnConfig cfg = getColumnConfig(defaultName);
        if (cfg != null && !cfg.getDisplayName().equalsIgnoreCase(cfg.getKey())) {
            return true;
        }
        if (customColumnNames == null) return false;
        if (customColumnNames.containsKey(defaultName) && !customColumnNames.get(defaultName).trim().isEmpty()) {
            return true;
        }
        String clean = defaultName.replaceAll("\\s+", "");
        for (Map.Entry<String, String> entry : customColumnNames.entrySet()) {
            if (entry.getKey().replaceAll("\\s+", "").equalsIgnoreCase(clean) && !entry.getValue().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void resetColumnNamesToDefault() {
        if (customColumnNames != null) {
            customColumnNames.clear();
        }
        initDefaultColumnConfigs();
    }
}
