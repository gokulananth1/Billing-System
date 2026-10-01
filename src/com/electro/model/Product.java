package com.electro.model;

/**
 * Represents an electronics product in the catalog.
 */
public class Product {
    private String id;
    private String sku;
    private String name;
    private String brand;
    private String category;
    private String modelNumber;
    private double costPrice;
    private double sellingPrice;     // Retail price (MRP)
    private double wholesalePrice;   // Wholesale / bulk price (typically 5-15% less than retail)
    private double taxRate; // e.g. 18.0 for 18% GST
    private int stockQuantity;
    private int warrantyMonths; // e.g. 12 for 1 year, 24 for 2 years
    private boolean requiresSerial; // Smartphones, laptops, TVs require serial/IMEI
    private java.util.Map<String, String> customFields = new java.util.LinkedHashMap<>();

    public Product() {}

    public Product(String id, String sku, String name, String brand, String category,
                   String modelNumber, double costPrice, double sellingPrice,
                   double taxRate, int stockQuantity, int warrantyMonths, boolean requiresSerial) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.modelNumber = modelNumber;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        // Default wholesale price: 8% below retail (can be overridden from inventory)
        this.wholesalePrice = Math.round(sellingPrice * 0.92 * 100.0) / 100.0;
        this.taxRate = taxRate;
        this.stockQuantity = stockQuantity;
        this.warrantyMonths = warrantyMonths;
        this.requiresSerial = requiresSerial;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public double getSellingPrice() { return sellingPrice; }
    public double getRetailPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public double getWholesalePrice() {
        // Fallback: if wholesale was never set (0 or negative), derive from retail
        return (wholesalePrice > 0) ? wholesalePrice : Math.round(sellingPrice * 0.92 * 100.0) / 100.0;
    }
    public void setWholesalePrice(double wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public boolean isRequiresSerial() { return requiresSerial; }
    public void setRequiresSerial(boolean requiresSerial) { this.requiresSerial = requiresSerial; }

    public boolean isLowStock() {
        int threshold = 3;
        try {
            threshold = com.electro.service.DataStore.getInstance().getSettings().getLowStockThreshold();
        } catch (Exception ignored) {}
        return stockQuantity > 0 && stockQuantity <= (threshold > 0 ? threshold : 3);
    }

    public boolean isOutOfStock() {
        return stockQuantity <= 0;
    }

    public java.util.Map<String, String> getCustomFields() {
        if (customFields == null) {
            customFields = new java.util.LinkedHashMap<>();
        }
        return customFields;
    }

    public void setCustomFields(java.util.Map<String, String> customFields) {
        this.customFields = customFields != null ? new java.util.LinkedHashMap<>(customFields) : new java.util.LinkedHashMap<>();
    }

    public String getCustomField(String key) {
        if (key == null || customFields == null) return "";
        return customFields.getOrDefault(key, "");
    }

    public void setCustomField(String key, String value) {
        if (key == null || key.trim().isEmpty()) return;
        if (customFields == null) {
            customFields = new java.util.LinkedHashMap<>();
        }
        if (value != null && !value.trim().isEmpty()) {
            customFields.put(key.trim(), value.trim());
        } else {
            customFields.remove(key.trim());
        }
    }

    @Override
    public String toString() {
        return brand + " " + name + " (" + modelNumber + ")";
    }
}
