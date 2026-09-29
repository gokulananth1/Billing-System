package com.electro.model;

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
}
