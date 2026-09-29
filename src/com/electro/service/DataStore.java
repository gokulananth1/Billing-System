package com.electro.service;

import com.electro.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Thread-safe persistent file-backed data store.
 * Stores state in standard JSON files under data/ directory.
 */
public class DataStore {
    private static DataStore instance;

    private final Path dataDir;
    private final Path productsFile;
    private final Path invoicesFile;
    private final Path customersFile;
    private final Path warrantiesFile;
    private final Path settingsFile;

    private final Map<String, Product> products = new LinkedHashMap<>();
    private final List<Invoice> invoices = new ArrayList<>();
    private final Map<String, Customer> customers = new LinkedHashMap<>(); // key: phone
    private final Map<String, WarrantyRecord> warranties = new LinkedHashMap<>(); // key: serialNumber
    private ShopSettings settings = new ShopSettings();

    private DataStore() {
        this.dataDir = Paths.get("data");
        this.productsFile = dataDir.resolve("products.json");
        this.invoicesFile = dataDir.resolve("invoices.json");
        this.customersFile = dataDir.resolve("customers.json");
        this.warrantiesFile = dataDir.resolve("warranties.json");
        this.settingsFile = dataDir.resolve("settings.json");

        initDirectories();
        loadAll();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private void initDirectories() {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadAll() {
        loadSettings();
        loadProducts();
        loadCustomers();
        loadInvoices();
        loadWarranties();

        if (!Files.exists(productsFile)) {
            seedSampleProducts();
            saveProducts();
        }
    }

    // --- PRODUCTS ---
    public synchronized List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public synchronized Product getProductById(String id) {
        return products.get(id);
    }

    public synchronized void saveProduct(Product product) {
        products.put(product.getId(), product);
        saveProducts();
    }

    public synchronized void deleteProduct(String id) {
        products.remove(id);
        saveProducts();
    }

    public synchronized void updateStock(String productId, int delta) {
        Product p = products.get(productId);
        if (p != null) {
            p.setStockQuantity(Math.max(0, p.getStockQuantity() + delta));
            saveProducts();
        }
    }

    // --- INVOICES ---
    public synchronized List<Invoice> getAllInvoices() {
        return new ArrayList<>(invoices);
    }

    public synchronized void addInvoice(Invoice invoice) {
        invoices.add(0, invoice); // newest first
        saveInvoices();
    }

    public synchronized Invoice getInvoiceById(String id) {
        for (Invoice inv : invoices) {
            if (inv.getInvoiceId().equalsIgnoreCase(id)) {
                return inv;
            }
        }
        return null;
    }

    // --- CUSTOMERS ---
    public synchronized List<Customer> getAllCustomers() {
        return new ArrayList<>(customers.values());
    }

    public synchronized Customer getCustomerByPhone(String phone) {
        if (phone == null) return null;
        return customers.get(phone.trim());
    }

    public synchronized void saveCustomer(Customer customer) {
        if (customer != null && customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
            customers.put(customer.getPhone().trim(), customer);
            saveCustomers();
        }
    }

    // --- WARRANTIES ---
    public synchronized List<WarrantyRecord> getAllWarranties() {
        return new ArrayList<>(warranties.values());
    }

    public synchronized WarrantyRecord getWarrantyBySerial(String serial) {
        if (serial == null) return null;
        return warranties.get(serial.trim().toUpperCase());
    }

    public synchronized List<WarrantyRecord> getWarrantiesByPhone(String phone) {
        List<WarrantyRecord> list = new ArrayList<>();
        if (phone == null) return list;
        String pClean = phone.trim();
        for (WarrantyRecord rec : warranties.values()) {
            if (rec.getCustomerPhone() != null && rec.getCustomerPhone().contains(pClean)) {
                list.add(rec);
            }
        }
        return list;
    }

    public synchronized void addWarranty(WarrantyRecord record) {
        if (record != null && record.getSerialNumber() != null) {
            warranties.put(record.getSerialNumber().trim().toUpperCase(), record);
            saveWarranties();
        }
    }

    // --- SETTINGS ---
    public synchronized ShopSettings getSettings() {
        return settings;
    }

    public synchronized void saveSettings(ShopSettings newSettings) {
        this.settings = newSettings;
        saveSettingsToFile();
    }

    // --- RESET OPERATIONS ---
    public synchronized void resetSalesData() {
        invoices.clear();
        warranties.clear();
        saveInvoices();
        saveWarranties();
    }

    public synchronized void removeAllProducts() {
        products.clear();
        saveProducts();
    }

    public synchronized void restoreDefaultProducts() {
        seedSampleProducts();
        saveProducts();
    }

    public synchronized String createBackup(String notes) throws IOException {
        Path backupDir = Paths.get("backups");
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now());
        Path targetDir = backupDir.resolve("backup_" + timestamp);
        Files.createDirectories(targetDir);

        if (Files.exists(productsFile)) Files.copy(productsFile, targetDir.resolve("products.json"), StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(invoicesFile)) Files.copy(invoicesFile, targetDir.resolve("invoices.json"), StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(customersFile)) Files.copy(customersFile, targetDir.resolve("customers.json"), StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(warrantiesFile)) Files.copy(warrantiesFile, targetDir.resolve("warranties.json"), StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(settingsFile)) Files.copy(settingsFile, targetDir.resolve("settings.json"), StandardCopyOption.REPLACE_EXISTING);

        Path usersFile = dataDir.resolve("users.json");
        if (Files.exists(usersFile)) Files.copy(usersFile, targetDir.resolve("users.json"), StandardCopyOption.REPLACE_EXISTING);

        String meta = "Timestamp: " + timestamp + "\nNotes: " + (notes != null ? notes : "Admin System Backup") + "\n";
        Files.writeString(targetDir.resolve("backup_meta.txt"), meta, StandardCharsets.UTF_8);

        return targetDir.toAbsolutePath().toString();
    }

    public synchronized void restoreBackup(Path backupDir) throws IOException {
        if (!Files.exists(backupDir) || !Files.isDirectory(backupDir)) {
            throw new IllegalArgumentException("Invalid backup directory: " + backupDir);
        }

        Path bProducts = backupDir.resolve("products.json");
        Path bInvoices = backupDir.resolve("invoices.json");
        Path bCustomers = backupDir.resolve("customers.json");
        Path bWarranties = backupDir.resolve("warranties.json");
        Path bSettings = backupDir.resolve("settings.json");
        Path bUsers = backupDir.resolve("users.json");

        if (Files.exists(bProducts)) Files.copy(bProducts, productsFile, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(bInvoices)) Files.copy(bInvoices, invoicesFile, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(bCustomers)) Files.copy(bCustomers, customersFile, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(bWarranties)) Files.copy(bWarranties, warrantiesFile, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(bSettings)) Files.copy(bSettings, settingsFile, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(bUsers)) Files.copy(bUsers, dataDir.resolve("users.json"), StandardCopyOption.REPLACE_EXISTING);

        loadSettings();
        loadProducts();
        loadCustomers();
        loadInvoices();
        loadWarranties();
    }

    public synchronized void factoryReset() {
        resetSalesData();
        removeAllProducts();
        seedSampleProducts();
        saveProducts();
        this.settings = new ShopSettings();
        saveSettingsToFile();
    }

    // --- PERSISTENCE LOGIC ---

    private void loadSettings() {
        if (!Files.exists(settingsFile)) return;
        try {
            String json = Files.readString(settingsFile, StandardCharsets.UTF_8);
            SimpleJson.JsonValue val = SimpleJson.parse(json);
            if (val.isObject()) {
                settings.setStoreName(val.get("storeName").asString(settings.getStoreName()));
                settings.setTagline(val.get("tagline").asString(settings.getTagline()));
                settings.setAddress(val.get("address").asString(settings.getAddress()));
                settings.setPhone(val.get("phone").asString(settings.getPhone()));
                settings.setEmail(val.get("email").asString(settings.getEmail()));
                settings.setGstin(val.get("gstin").asString(settings.getGstin()));
                settings.setCurrencySymbol(val.get("currencySymbol").asString(settings.getCurrencySymbol()));
                settings.setDefaultTaxRate(val.get("defaultTaxRate").asDouble(settings.getDefaultTaxRate()));
                settings.setInvoiceFooter(val.get("invoiceFooter").asString(settings.getInvoiceFooter()));
                settings.setTermsAndConditions(val.get("termsAndConditions").asString(settings.getTermsAndConditions()));
                settings.setInvoicePrefix(val.get("invoicePrefix").asString(settings.getInvoicePrefix()));
                settings.setEnableGstBilling(val.get("enableGstBilling").asBoolean(settings.isEnableGstBilling()));
                settings.setWholesaleDiscountPercent(val.get("wholesaleDiscountPercent").asDouble(settings.getWholesaleDiscountPercent()));
                settings.setLowStockThreshold(val.get("lowStockThreshold").asInt(settings.getLowStockThreshold()));
                settings.setStrictSerialTracking(val.get("strictSerialTracking").asBoolean(settings.isStrictSerialTracking()));
                settings.setDefaultWarrantyMonths(val.get("defaultWarrantyMonths").asInt(settings.getDefaultWarrantyMonths()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSettingsToFile() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"storeName\": \"").append(SimpleJson.escape(settings.getStoreName())).append("\",\n");
            sb.append("  \"tagline\": \"").append(SimpleJson.escape(settings.getTagline())).append("\",\n");
            sb.append("  \"address\": \"").append(SimpleJson.escape(settings.getAddress())).append("\",\n");
            sb.append("  \"phone\": \"").append(SimpleJson.escape(settings.getPhone())).append("\",\n");
            sb.append("  \"email\": \"").append(SimpleJson.escape(settings.getEmail())).append("\",\n");
            sb.append("  \"gstin\": \"").append(SimpleJson.escape(settings.getGstin())).append("\",\n");
            sb.append("  \"currencySymbol\": \"").append(SimpleJson.escape(settings.getCurrencySymbol())).append("\",\n");
            sb.append("  \"defaultTaxRate\": ").append(settings.getDefaultTaxRate()).append(",\n");
            sb.append("  \"invoiceFooter\": \"").append(SimpleJson.escape(settings.getInvoiceFooter())).append("\",\n");
            sb.append("  \"termsAndConditions\": \"").append(SimpleJson.escape(settings.getTermsAndConditions())).append("\",\n");
            sb.append("  \"invoicePrefix\": \"").append(SimpleJson.escape(settings.getInvoicePrefix())).append("\",\n");
            sb.append("  \"enableGstBilling\": ").append(settings.isEnableGstBilling()).append(",\n");
            sb.append("  \"wholesaleDiscountPercent\": ").append(settings.getWholesaleDiscountPercent()).append(",\n");
            sb.append("  \"lowStockThreshold\": ").append(settings.getLowStockThreshold()).append(",\n");
            sb.append("  \"strictSerialTracking\": ").append(settings.isStrictSerialTracking()).append(",\n");
            sb.append("  \"defaultWarrantyMonths\": ").append(settings.getDefaultWarrantyMonths()).append("\n");
            sb.append("}\n");
            Files.writeString(settingsFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProducts() {
        if (!Files.exists(productsFile)) return;
        try {
            String json = Files.readString(productsFile, StandardCharsets.UTF_8);
            SimpleJson.JsonValue val = SimpleJson.parse(json);
            products.clear();
            for (SimpleJson.JsonValue item : val.asArray()) {
                double retail = item.get("sellingPrice").asDouble(0);
                double wholesale = item.get("wholesalePrice").asDouble(Math.round(retail * 0.92 * 100.0) / 100.0);
                Product p = new Product(
                        item.get("id").asString(""),
                        item.get("sku").asString(""),
                        item.get("name").asString(""),
                        item.get("brand").asString(""),
                        item.get("category").asString(""),
                        item.get("modelNumber").asString(""),
                        item.get("costPrice").asDouble(0),
                        retail,
                        item.get("taxRate").asDouble(18.0),
                        item.get("stockQuantity").asInt(0),
                        item.get("warrantyMonths").asInt(12),
                        item.get("requiresSerial").asBoolean(true)
                );
                p.setWholesalePrice(wholesale);
                products.put(p.getId(), p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProducts() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            int i = 0;
            for (Product p : products.values()) {
                if (i > 0) sb.append(",\n");
                sb.append("  {\n");
                sb.append("    \"id\": \"").append(SimpleJson.escape(p.getId())).append("\",\n");
                sb.append("    \"sku\": \"").append(SimpleJson.escape(p.getSku())).append("\",\n");
                sb.append("    \"name\": \"").append(SimpleJson.escape(p.getName())).append("\",\n");
                sb.append("    \"brand\": \"").append(SimpleJson.escape(p.getBrand())).append("\",\n");
                sb.append("    \"category\": \"").append(SimpleJson.escape(p.getCategory())).append("\",\n");
                sb.append("    \"modelNumber\": \"").append(SimpleJson.escape(p.getModelNumber())).append("\",\n");
                sb.append("    \"costPrice\": ").append(p.getCostPrice()).append(",\n");
                sb.append("    \"sellingPrice\": ").append(p.getSellingPrice()).append(",\n");
                sb.append("    \"wholesalePrice\": ").append(p.getWholesalePrice()).append(",\n");
                sb.append("    \"taxRate\": ").append(p.getTaxRate()).append(",\n");
                sb.append("    \"stockQuantity\": ").append(p.getStockQuantity()).append(",\n");
                sb.append("    \"warrantyMonths\": ").append(p.getWarrantyMonths()).append(",\n");
                sb.append("    \"requiresSerial\": ").append(p.isRequiresSerial()).append("\n");
                sb.append("  }");
                i++;
            }
            sb.append("\n]\n");
            Files.writeString(productsFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCustomers() {
        if (!Files.exists(customersFile)) return;
        try {
            String json = Files.readString(customersFile, StandardCharsets.UTF_8);
            SimpleJson.JsonValue val = SimpleJson.parse(json);
            customers.clear();
            for (SimpleJson.JsonValue item : val.asArray()) {
                Customer c = new Customer(
                        item.get("id").asString(""),
                        item.get("name").asString(""),
                        item.get("phone").asString(""),
                        item.get("email").asString(""),
                        item.get("address").asString(""),
                        item.get("gstin").asString(""),
                        item.get("customerType").asString("RETAIL")
                );
                if (c.getPhone() != null && !c.getPhone().isEmpty()) {
                    customers.put(c.getPhone().trim(), c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCustomers() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            int i = 0;
            for (Customer c : customers.values()) {
                if (i > 0) sb.append(",\n");
                sb.append("  {\n");
                sb.append("    \"id\": \"").append(SimpleJson.escape(c.getId())).append("\",\n");
                sb.append("    \"name\": \"").append(SimpleJson.escape(c.getName())).append("\",\n");
                sb.append("    \"phone\": \"").append(SimpleJson.escape(c.getPhone())).append("\",\n");
                sb.append("    \"email\": \"").append(SimpleJson.escape(c.getEmail())).append("\",\n");
                sb.append("    \"address\": \"").append(SimpleJson.escape(c.getAddress())).append("\",\n");
                sb.append("    \"gstin\": \"").append(SimpleJson.escape(c.getGstin())).append("\",\n");
                sb.append("    \"customerType\": \"").append(SimpleJson.escape(c.getCustomerType())).append("\"\n");
                sb.append("  }");
                i++;
            }
            sb.append("\n]\n");
            Files.writeString(customersFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInvoices() {
        if (!Files.exists(invoicesFile)) return;
        try {
            String json = Files.readString(invoicesFile, StandardCharsets.UTF_8);
            SimpleJson.JsonValue val = SimpleJson.parse(json);
            invoices.clear();
            for (SimpleJson.JsonValue item : val.asArray()) {
                SimpleJson.JsonValue cVal = item.get("customer");
                Customer c = new Customer(
                        cVal.get("id").asString(""),
                        cVal.get("name").asString(""),
                        cVal.get("phone").asString(""),
                        cVal.get("email").asString(""),
                        cVal.get("address").asString(""),
                        cVal.get("gstin").asString(""),
                        cVal.get("customerType").asString("RETAIL")
                );

                List<CartItem> items = new ArrayList<>();
                for (SimpleJson.JsonValue ci : item.get("items").asArray()) {
                    SimpleJson.JsonValue pVal = ci.get("product");
                    double retail = pVal.get("sellingPrice").asDouble(0);
                    double wholesale = pVal.get("wholesalePrice").asDouble(Math.round(retail * 0.92 * 100.0) / 100.0);
                    Product p = new Product(
                            pVal.get("id").asString(""),
                            pVal.get("sku").asString(""),
                            pVal.get("name").asString(""),
                            pVal.get("brand").asString(""),
                            pVal.get("category").asString(""),
                            pVal.get("modelNumber").asString(""),
                            pVal.get("costPrice").asDouble(0),
                            retail,
                            pVal.get("taxRate").asDouble(18.0),
                            pVal.get("stockQuantity").asInt(0),
                            pVal.get("warrantyMonths").asInt(12),
                            pVal.get("requiresSerial").asBoolean(true)
                    );
                    p.setWholesalePrice(wholesale);
                    CartItem cartItem = new CartItem(p, ci.get("quantity").asInt(1));
                    cartItem.setUnitPrice(ci.get("unitPrice").asDouble(p.getSellingPrice()));
                    cartItem.setDiscountPercent(ci.get("discountPercent").asDouble(0));
                    List<String> serials = new ArrayList<>();
                    for (SimpleJson.JsonValue s : ci.get("serialNumbers").asArray()) {
                        serials.add(s.asString(""));
                    }
                    cartItem.setSerialNumbers(serials);
                    items.add(cartItem);
                }

                Invoice inv = new Invoice(
                        item.get("invoiceId").asString(""),
                        c,
                        items,
                        item.get("subtotal").asDouble(0),
                        item.get("totalDiscount").asDouble(0),
                        item.get("taxableAmount").asDouble(0),
                        item.get("cgstAmount").asDouble(0),
                        item.get("sgstAmount").asDouble(0),
                        item.get("grandTotal").asDouble(0),
                        item.get("paymentMethod").asString("Cash"),
                        item.get("paymentReference").asString(""),
                        item.get("notes").asString("")
                );
                inv.setDateTime(item.get("dateTime").asString(""));
                inv.setStatus(item.get("status").asString("COMPLETED"));
                inv.setRefundAmount(item.get("refundAmount").asDouble(0.0));
                inv.setRefundReason(item.get("refundReason").asString(""));
                inv.setRefundDateTime(item.get("refundDateTime").asString(""));
                inv.setPaymentBreakdown(item.get("paymentBreakdown").asString(""));
                invoices.add(inv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveInvoicesData() {
        saveInvoices();
    }

    public synchronized void saveWarrantiesData() {
        saveWarranties();
    }

    public synchronized void saveProductsData() {
        saveProducts();
    }

    private void saveInvoices() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                if (i > 0) sb.append(",\n");
                sb.append("  {\n");
                sb.append("    \"invoiceId\": \"").append(SimpleJson.escape(inv.getInvoiceId())).append("\",\n");
                sb.append("    \"dateTime\": \"").append(SimpleJson.escape(inv.getDateTime())).append("\",\n");
                sb.append("    \"status\": \"").append(SimpleJson.escape(inv.getStatus())).append("\",\n");
                sb.append("    \"refundAmount\": ").append(inv.getRefundAmount()).append(",\n");
                sb.append("    \"refundReason\": \"").append(SimpleJson.escape(inv.getRefundReason())).append("\",\n");
                sb.append("    \"refundDateTime\": \"").append(SimpleJson.escape(inv.getRefundDateTime())).append("\",\n");
                sb.append("    \"paymentMethod\": \"").append(SimpleJson.escape(inv.getPaymentMethod())).append("\",\n");
                sb.append("    \"paymentBreakdown\": \"").append(SimpleJson.escape(inv.getPaymentBreakdown())).append("\",\n");
                sb.append("    \"paymentReference\": \"").append(SimpleJson.escape(inv.getPaymentReference())).append("\",\n");
                sb.append("    \"notes\": \"").append(SimpleJson.escape(inv.getNotes())).append("\",\n");
                sb.append("    \"subtotal\": ").append(inv.getSubtotal()).append(",\n");
                sb.append("    \"totalDiscount\": ").append(inv.getTotalDiscount()).append(",\n");
                sb.append("    \"taxableAmount\": ").append(inv.getTaxableAmount()).append(",\n");
                sb.append("    \"cgstAmount\": ").append(inv.getCgstAmount()).append(",\n");
                sb.append("    \"sgstAmount\": ").append(inv.getSgstAmount()).append(",\n");
                sb.append("    \"grandTotal\": ").append(inv.getGrandTotal()).append(",\n");

                // Customer
                Customer c = inv.getCustomer() != null ? inv.getCustomer() : new Customer("", "Walk-in Customer", "", "", "", "");
                sb.append("    \"customer\": {\n");
                sb.append("      \"id\": \"").append(SimpleJson.escape(c.getId())).append("\",\n");
                sb.append("      \"name\": \"").append(SimpleJson.escape(c.getName())).append("\",\n");
                sb.append("      \"phone\": \"").append(SimpleJson.escape(c.getPhone())).append("\",\n");
                sb.append("      \"email\": \"").append(SimpleJson.escape(c.getEmail())).append("\",\n");
                sb.append("      \"address\": \"").append(SimpleJson.escape(c.getAddress())).append("\",\n");
                sb.append("      \"gstin\": \"").append(SimpleJson.escape(c.getGstin())).append("\",\n");
                sb.append("      \"customerType\": \"").append(SimpleJson.escape(c.getCustomerType())).append("\"\n");
                sb.append("    },\n");

                // Items
                sb.append("    \"items\": [\n");
                for (int j = 0; j < inv.getItems().size(); j++) {
                    CartItem ci = inv.getItems().get(j);
                    if (j > 0) sb.append(",\n");
                    sb.append("      {\n");
                    sb.append("        \"quantity\": ").append(ci.getQuantity()).append(",\n");
                    sb.append("        \"unitPrice\": ").append(ci.getUnitPrice()).append(",\n");
                    sb.append("        \"discountPercent\": ").append(ci.getDiscountPercent()).append(",\n");

                    // Serials
                    sb.append("        \"serialNumbers\": [");
                    for (int s = 0; s < ci.getSerialNumbers().size(); s++) {
                        if (s > 0) sb.append(", ");
                        sb.append("\"").append(SimpleJson.escape(ci.getSerialNumbers().get(s))).append("\"");
                    }
                    sb.append("],\n");

                    Product p = ci.getProduct();
                    sb.append("        \"product\": {\n");
                    sb.append("          \"id\": \"").append(SimpleJson.escape(p.getId())).append("\",\n");
                    sb.append("          \"sku\": \"").append(SimpleJson.escape(p.getSku())).append("\",\n");
                    sb.append("          \"name\": \"").append(SimpleJson.escape(p.getName())).append("\",\n");
                    sb.append("          \"brand\": \"").append(SimpleJson.escape(p.getBrand())).append("\",\n");
                    sb.append("          \"category\": \"").append(SimpleJson.escape(p.getCategory())).append("\",\n");
                    sb.append("          \"modelNumber\": \"").append(SimpleJson.escape(p.getModelNumber())).append("\",\n");
                    sb.append("          \"costPrice\": ").append(p.getCostPrice()).append(",\n");
                    sb.append("          \"sellingPrice\": ").append(p.getSellingPrice()).append(",\n");
                    sb.append("          \"wholesalePrice\": ").append(p.getWholesalePrice()).append(",\n");
                    sb.append("          \"taxRate\": ").append(p.getTaxRate()).append(",\n");
                    sb.append("          \"warrantyMonths\": ").append(p.getWarrantyMonths()).append(",\n");
                    sb.append("          \"requiresSerial\": ").append(p.isRequiresSerial()).append("\n");
                    sb.append("        }\n");
                    sb.append("      }");
                }
                sb.append("\n    ]\n");
                sb.append("  }");
            }
            sb.append("\n]\n");
            Files.writeString(invoicesFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadWarranties() {
        if (!Files.exists(warrantiesFile)) return;
        try {
            String json = Files.readString(warrantiesFile, StandardCharsets.UTF_8);
            SimpleJson.JsonValue val = SimpleJson.parse(json);
            warranties.clear();
            for (SimpleJson.JsonValue item : val.asArray()) {
                WarrantyRecord wr = new WarrantyRecord();
                wr.setSerialNumber(item.get("serialNumber").asString(""));
                wr.setInvoiceId(item.get("invoiceId").asString(""));
                wr.setProductId(item.get("productId").asString(""));
                wr.setProductName(item.get("productName").asString(""));
                wr.setBrand(item.get("brand").asString(""));
                wr.setCustomerName(item.get("customerName").asString(""));
                wr.setCustomerPhone(item.get("customerPhone").asString(""));
                wr.setPurchaseDate(item.get("purchaseDate").asString(""));
                wr.setWarrantyMonths(item.get("warrantyMonths").asInt(12));
                wr.setExpiryDate(item.get("expiryDate").asString(""));
                wr.setStatus(item.get("status").asString("ACTIVE"));
                if (!wr.getSerialNumber().isEmpty()) {
                    warranties.put(wr.getSerialNumber().trim().toUpperCase(), wr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveWarranties() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            int i = 0;
            for (WarrantyRecord wr : warranties.values()) {
                if (i > 0) sb.append(",\n");
                sb.append("  {\n");
                sb.append("    \"serialNumber\": \"").append(SimpleJson.escape(wr.getSerialNumber())).append("\",\n");
                sb.append("    \"invoiceId\": \"").append(SimpleJson.escape(wr.getInvoiceId())).append("\",\n");
                sb.append("    \"productId\": \"").append(SimpleJson.escape(wr.getProductId())).append("\",\n");
                sb.append("    \"productName\": \"").append(SimpleJson.escape(wr.getProductName())).append("\",\n");
                sb.append("    \"brand\": \"").append(SimpleJson.escape(wr.getBrand())).append("\",\n");
                sb.append("    \"customerName\": \"").append(SimpleJson.escape(wr.getCustomerName())).append("\",\n");
                sb.append("    \"customerPhone\": \"").append(SimpleJson.escape(wr.getCustomerPhone())).append("\",\n");
                sb.append("    \"purchaseDate\": \"").append(SimpleJson.escape(wr.getPurchaseDate())).append("\",\n");
                sb.append("    \"warrantyMonths\": ").append(wr.getWarrantyMonths()).append(",\n");
                sb.append("    \"expiryDate\": \"").append(SimpleJson.escape(wr.getExpiryDate())).append("\",\n");
                sb.append("    \"status\": \"").append(SimpleJson.escape(wr.getStatus())).append("\"\n");
                sb.append("  }");
                i++;
            }
            sb.append("\n]\n");
            Files.writeString(warrantiesFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seedSampleProducts() {
        products.put("P1001", new Product("P1001", "IPH-15P-128", "iPhone 15 Pro (128GB)", "Apple", "Smartphones", "A2848", 115000, 134900, 18.0, 8, 12, true));
        products.put("P1002", new Product("P1002", "SAM-S24U-512", "Galaxy S24 Ultra (512GB)", "Samsung", "Smartphones", "SM-S928B", 108000, 129999, 18.0, 6, 12, true));
        products.put("P1003", new Product("P1003", "PIX-8P-128", "Pixel 8 Pro (128GB)", "Google", "Smartphones", "GC3VE", 68000, 84000, 18.0, 5, 12, true));

        products.put("P1004", new Product("P1004", "MAC-A15-M3", "MacBook Air 15\" (M3, 16GB/512GB)", "Apple", "Laptops", "MRYU3", 115000, 134900, 18.0, 4, 12, true));
        products.put("P1005", new Product("P1005", "DEL-XPS-9530", "XPS 15 9530 (i7, 32GB, 1TB RTX4060)", "Dell", "Laptops", "XPS-9530", 160000, 189990, 18.0, 3, 24, true));
        products.put("P1006", new Product("P1006", "LEN-X1C-G11", "ThinkPad X1 Carbon Gen 11", "Lenovo", "Laptops", "21HM00", 140000, 165000, 18.0, 4, 36, true));

        products.put("P1007", new Product("P1007", "SON-WH1000M5", "WH-1000XM5 Wireless Headphones", "Sony", "Audio", "WH1000XM5/B", 22000, 29990, 18.0, 12, 12, true));
        products.put("P1008", new Product("P1008", "APP-AIRPOD-P2", "AirPods Pro (2nd Gen USB-C)", "Apple", "Audio", "MTJV3", 19500, 24900, 18.0, 15, 12, true));
        products.put("P1009", new Product("P1009", "JBL-FLIP-6", "Flip 6 Portable Bluetooth Speaker", "JBL", "Audio", "JBLFLIP6BLK", 6800, 9999, 18.0, 10, 12, true));

        products.put("P1010", new Product("P1010", "SAM-TV-55Q60", "55\" QLED 4K Smart TV", "Samsung", "TV & Home", "QA55Q60DAK", 48000, 64990, 28.0, 5, 24, true));
        products.put("P1011", new Product("P1011", "LG-OLED-65C3", "65\" OLED evo 4K Cinema TV", "LG", "TV & Home", "OLED65C3PSA", 145000, 184990, 28.0, 2, 36, true)); // Low stock test!

        products.put("P1012", new Product("P1012", "ANK-GAN-65W", "735 65W GaN Fast Charger (3-Port)", "Anker", "Accessories", "A2668", 2200, 3499, 18.0, 25, 18, false));
        products.put("P1013", new Product("P1013", "APP-USB-240W", "USB-C Woven Charge Cable (2m)", "Apple", "Accessories", "MU2G3ZM/A", 1800, 2900, 18.0, 30, 12, false));
        products.put("P1014", new Product("P1014", "LOG-MX3S-BLK", "MX Master 3S Wireless Performance Mouse", "Logitech", "Accessories", "910-006561", 6200, 8995, 18.0, 14, 24, true));
        products.put("P1015", new Product("P1015", "SAN-SSD-1TB", "Extreme Portable 1TB NVMe SSD", "SanDisk", "Accessories", "SDSSDE61-1T00", 7500, 10999, 18.0, 11, 36, true));
    }
}
