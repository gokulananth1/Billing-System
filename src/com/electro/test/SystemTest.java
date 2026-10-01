package com.electro.test;

import com.electro.model.*;
import com.electro.service.*;
import com.electro.ui.ColumnConfigDialog;
import com.electro.ui.InvoicePreviewDialog;
import com.electro.ui.LoginDialog;
import com.electro.ui.WarrantyPanel;

import javax.swing.DefaultListModel;
import javax.swing.JTable;
import java.io.File;
import java.util.List;

/**
 * Automated headless test suite verifying all core billing, inventory, warranty, and invoice features.
 */
public class SystemTest {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" Starting Automated System Tests for Billing App ");
        System.out.println("==================================================");

        try {
            // 1. Test DataStore & Sample Catalog Seeding
            DataStore store = DataStore.getInstance();
            if (store.getAllProducts().size() < 10) {
                store.restoreDefaultProducts();
            }
            List<Product> initialProducts = store.getAllProducts();
            assert initialProducts.size() >= 10 : "Catalog should be seeded with at least 10 products";
            System.out.println("[PASS] 1. DataStore initialized with " + initialProducts.size() + " electronic products.");

            // 2. Test Inventory Service
            InventoryService invService = new InventoryService();
            List<Product> phones = invService.searchProducts("iPhone", "Smartphones");
            assert !phones.isEmpty() : "Should find iPhone under Smartphones";
            Product iphone = phones.get(0);
            if (iphone.getStockQuantity() < 2) {
                invService.restockProduct(iphone.getId(), 8);
                iphone = invService.getProductById(iphone.getId());
            }
            int initialStock = iphone.getStockQuantity();
            System.out.println("[PASS] 2. Product found: " + iphone.getName() + " (Initial stock: " + initialStock + ")");

            // 3. Test Billing POS Cart Lifecycle
            BillingService billingService = new BillingService();
            billingService.clearCart();
            billingService.addToCart(iphone, 1);

            // iPhone requires serial
            String errBeforeSerial = billingService.validateCartForCheckout();
            assert errBeforeSerial != null && errBeforeSerial.contains("Serial/IMEI number missing") : "Validation must reject checkout without serial";
            System.out.println("[PASS] 3a. Serial validation correctly caught missing serial: " + errBeforeSerial);

            // Assign serial number
            String testSerial = "SN-TEST-IPHONE-001";
            billingService.setItemSerials(iphone.getId(), List.of(testSerial));
            String errAfterSerial = billingService.validateCartForCheckout();
            assert errAfterSerial == null : "Validation should pass once serial is provided";
            System.out.println("[PASS] 3b. Serial validation passed after serial assignment.");

            // 4. Test Financial Calculations
            double subtotal = billingService.calculateSubtotal();
            double tax = billingService.calculateTotalTax();
            double grandTotal = billingService.calculateGrandTotal();
            assert Math.abs((subtotal + tax) - grandTotal) < 0.05 : "Grand total must equal subtotal + tax";
            System.out.println("[PASS] 4. Calculations verified: Subtotal=" + subtotal + ", Tax=" + tax + ", Grand Total=" + grandTotal);

            // 5. Test Checkout Execution
            Customer customer = new Customer("C-101", "Alex Turing", "9876543210", "alex@example.com", "Bangalore, India", "");
            Invoice invoice = billingService.checkout(customer, "UPI", "UPI-REF-998877", "Test sale");
            assert invoice != null : "Invoice must be generated";
            assert invoice.getInvoiceId().startsWith("INV-") : "Invoice ID format check";
            assert invoice.getDateTime().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} (AM|PM)") : "Invoice timing must be in 12hrs format (AM/PM): " + invoice.getDateTime();
            Invoice legacyInv = new Invoice();
            legacyInv.setDateTime("2026-09-29 14:30:00");
            assert legacyInv.getDateTime().equals("2026-09-29 02:30:00 PM") : "Legacy 24hr time must normalize to 12hr format: " + legacyInv.getDateTime();
            System.out.println("[PASS] 5. Checkout successful (12hr format: " + invoice.getDateTime() + "). Generated Invoice: " + invoice.getInvoiceId());

            // 6. Verify Stock Decrement
            Product updatedIphone = store.getProductById(iphone.getId());
            assert updatedIphone.getStockQuantity() == initialStock - 1 : "Stock must decrement by 1";
            System.out.println("[PASS] 6. Stock decremented properly. New Stock: " + updatedIphone.getStockQuantity());

            // 7. Verify Warranty Registration
            WarrantyService warrantyService = new WarrantyService();
            WarrantyRecord wr = warrantyService.findBySerial(testSerial);
            assert wr != null : "Warranty record must exist for serial";
            assert !wr.isExpired() : "New warranty must be active";
            assert wr.getCustomerName().equals("Alex Turing") : "Customer name must match";
            System.out.println("[PASS] 7. Warranty record verified: " + wr.getStatusDescription());

            // 8. Test Invoice HTML & Thermal Receipt Generation
            ShopSettings settings = store.getSettings();
            String html = InvoiceGenerator.generateHtmlInvoice(invoice, settings);
            assert html.contains(invoice.getInvoiceId()) : "HTML invoice must contain invoice ID";
            assert html.contains(testSerial) : "HTML invoice must contain serial number";

            String thermal = InvoiceGenerator.generateThermalReceipt(invoice, settings);
            assert thermal.contains("Alex Turing") : "Thermal receipt must contain customer";

            File htmlFile = InvoiceGenerator.saveHtmlInvoiceToFile(invoice, settings);
            assert htmlFile != null && htmlFile.exists() : "HTML file must be saved on disk";
            System.out.println("[PASS] 8. HTML Invoice generated and verified: " + htmlFile.getAbsolutePath());

            // 9. Test Reset Sales Data
            store.resetSalesData();
            assert store.getAllInvoices().isEmpty() : "Invoices must be empty after reset";
            assert store.getAllWarranties().isEmpty() : "Warranties must be empty after reset";
            System.out.println("[PASS] 9. Reset Sales Data successfully wiped all invoices and warranty records.");

            // Re-seed demo transactions so the UI analytics has data
            billingService.clearCart();
            billingService.setCustomerClassification(BillingService.CustomerClassification.RETAIL);
            billingService.addToCart(iphone, 1);
            billingService.setItemSerials(iphone.getId(), List.of("IMEI356789123456789"));
            Customer c1 = new Customer("C-101", "Arun Kumar", "9876543210", "arun@example.com", "Indiranagar, Bangalore", "", "RETAIL");
            billingService.checkout(c1, "UPI", "UPI-982348234", "Retail Customer Purchase");

            billingService.clearCart();
            billingService.setCustomerClassification(BillingService.CustomerClassification.WHOLESALE);
            Product p2 = store.getProductById("P1002");
            p2.setStockQuantity(Math.max(p2.getStockQuantity(), 5));
            billingService.addToCart(p2, 1);
            billingService.setItemSerials(p2.getId(), List.of("IMEI998877665544332"));
            Customer c2 = new Customer("C-102", "Apex Electronics Ltd", "9845012345", "sales@apexelectronics.in", "Peenya, Bangalore", "29AABCU9603R1ZM", "WHOLESALE");
            billingService.checkout(c2, "Bank Transfer", "NEFT-8839211", "B2B Wholesale Order");

            // 10. Test Authentication & Role Management
            AuthService auth = AuthService.getInstance();
            boolean adminLogin = auth.login("admin", "admin123");
            assert adminLogin : "Admin login with correct password must succeed";
            assert auth.getCurrentUser().isAdmin() : "Admin role check must return true";
            assert !auth.getCurrentUser().isCashier() : "Admin is not cashier";
            System.out.println("[PASS] 10a. Admin authentication and role verification successful.");

            if (auth.getUserByUsername("test_cashier") == null) {
                auth.createUser("test_cashier", "cashier123", "Test Cashier Staff", User.Role.CASHIER);
            }
            boolean cashierLogin = auth.login("test_cashier", "cashier123");
            assert cashierLogin : "Cashier login with correct password must succeed";
            assert auth.getCurrentUser().isCashier() : "Cashier role check must return true";
            assert !auth.getCurrentUser().isAdmin() : "Cashier is not admin";
            auth.logout();
            auth.deleteUser("test_cashier");
            System.out.println("[PASS] 10b. Cashier authentication and permission boundaries verified.");

            // 10c. Test Store Owner Authentication & Role Boundaries
            boolean ownerLogin = auth.login("owner", "owner123");
            assert ownerLogin : "Store Owner login with correct password must succeed";
            assert auth.getCurrentUser().isStoreOwner() : "Store Owner role check must return true";
            assert auth.getCurrentUser().canAccessAnalytics() : "Store Owner must have access to Analytics";
            assert !auth.getCurrentUser().canAccessSettings() : "Store Owner must NOT have access to Shop Settings";
            assert !auth.getCurrentUser().isAdmin() : "Store Owner is not Admin";
            System.out.println("[PASS] 10c. Store Owner authentication & permissions verified (Full access except Shop Settings).");

            // 11. Test Security: Reject Invalid Credentials, User CRUD & Edit Full Name
            boolean badPass = auth.login("admin", "wrongpass");
            assert !badPass : "Invalid password must be rejected";

            boolean badUser = auth.login("unknownuser", "password");
            assert !badUser : "Unknown user must be rejected";

            if (auth.getUserByUsername("tester_cashier") != null) {
                auth.deleteUser("tester_cashier");
            }
            auth.createUser("tester_cashier", "securepass", "Tester Cashier", User.Role.CASHIER);
            assert auth.login("tester_cashier", "securepass") : "Newly created user must be able to log in";

            // Test updating Cashier's Full Name
            auth.updateFullName("tester_cashier", "Updated Cashier Name");
            User updatedCashier = auth.getUserByUsername("tester_cashier");
            assert "Updated Cashier Name".equals(updatedCashier.getFullName()) : "Cashier full name must be updated";
            System.out.println("[PASS] 11a. Cashier full name edit verified: " + updatedCashier.getFullName());

            auth.login("admin", "admin123");
            assert auth.deleteUser("tester_cashier") : "Admin must be able to delete staff user";

            // Test Restriction: Cannot create users with role ADMIN
            boolean adminCreateBlocked = false;
            try {
                auth.createUser("fake_admin", "pass123", "Fake Admin", User.Role.ADMIN);
            } catch (IllegalArgumentException ex) {
                adminCreateBlocked = true;
            }
            assert adminCreateBlocked : "Creating a user with ADMIN role must be strictly forbidden";

            // Test Restriction: Cannot delete users with role ADMIN
            boolean adminDeleteBlocked = false;
            try {
                auth.deleteUser("admin");
            } catch (IllegalStateException ex) {
                adminDeleteBlocked = true;
            }
            assert adminDeleteBlocked : "Deleting an ADMIN user account must be strictly forbidden";
            assert auth.getUserByUsername("admin") != null : "Admin account must still exist and be protected";

            System.out.println("[PASS] 11b. Security verification: Invalid logins blocked, ADMIN creation/deletion strictly restricted, & User CRUD tested.");

            // 12. Test Retail vs Wholesale Pricing Classification
            BillingService pricingTest = new BillingService();
            pricingTest.clearCart();
            pricingTest.setCustomerClassification(BillingService.CustomerClassification.RETAIL);
            iphone.setStockQuantity(Math.max(iphone.getStockQuantity(), 5));
            pricingTest.addToCart(iphone, 1);
            double retailPrice = pricingTest.getCart().get(0).getUnitPrice();
            assert Math.abs(retailPrice - iphone.getSellingPrice()) < 0.01 : "Retail unit price must match selling price";

            // Switch to Wholesale mode
            pricingTest.setCustomerClassification(BillingService.CustomerClassification.WHOLESALE);
            double wholesalePrice = pricingTest.getCart().get(0).getUnitPrice();
            assert Math.abs(wholesalePrice - iphone.getWholesalePrice()) < 0.01 : "Wholesale unit price must match wholesale price";
            assert wholesalePrice < retailPrice : "Wholesale price must be lower than retail price";
            System.out.println("[PASS] 12. Classification Pricing verified: Retail=" + retailPrice + " -> Wholesale=" + wholesalePrice);

            // 13. Test Remove All Stock Items (Admin inventory reset)
            invService.removeAllProducts();
            assert store.getAllProducts().isEmpty() : "All products must be removed";
            assert invService.getAllProducts().isEmpty() : "InventoryService must report empty catalog";
            System.out.println("[PASS] 13a. Remove All Stock Items verified: Catalog completely cleared.");

            // Restore catalog so app is ready for use
            invService.restoreDefaultProducts();
            assert store.getAllProducts().size() >= 15 : "Default products restored successfully";
            System.out.println("[PASS] 13b. Default catalog restoration verified.");

            // 14. Test Advanced Admin Controls: Backup Creation, GST Toggle & Custom Invoice Prefix
            String backupPath = store.createBackup("System Automated Test Backup");
            assert backupPath != null && new File(backupPath).exists() : "Backup folder must be created on disk";
            System.out.println("[PASS] 14a. System snapshot backup created: " + backupPath);

            ShopSettings currentSettings = store.getSettings();
            currentSettings.setInvoicePrefix("BILL");
            currentSettings.setEnableGstBilling(false);
            store.saveSettings(currentSettings);

            billingService.clearCart();
            billingService.addToCart(iphone, 1);
            assert billingService.calculateTotalTax() == 0.0 : "Tax must be 0 when GST billing is disabled";

            billingService.setItemSerials(iphone.getId(), List.of("IMEI-BILL-TEST-99"));
            Invoice customInv = billingService.checkout(new Customer("C-T1", "Admin Test", "9998887771", "", "", ""), "Cash", "", "Prefix test");
            assert customInv.getInvoiceId().startsWith("BILL-") : "Invoice ID must start with custom prefix BILL-";
            System.out.println("[PASS] 14b. Custom Invoice Prefix & GST Billing Toggle verified: " + customInv.getInvoiceId());

            // Restore settings
            currentSettings.setInvoicePrefix("INV");
            currentSettings.setEnableGstBilling(true);
            store.saveSettings(currentSettings);

            // 15. Test Split Payments (Cash + UPI / Card)
            billingService.clearCart();
            Product macbook = store.getProductById("P1004");
            billingService.addToCart(macbook, 1);
            String testMacSerial = "SN-MAC-TEST-9988";
            billingService.setItemSerials(macbook.getId(), List.of(testMacSerial));
            String splitBreakdown = "Cash: ₹34,900.00 | UPI: ₹100,000.00";
            Invoice splitInvoice = billingService.checkout(
                    new Customer("C-SPLIT", "Split Test Buyer", "9900112233", "", "", ""),
                    "Split Payment (Multi-Mode)",
                    "SPLIT-REF-8877",
                    "Split payment test",
                    splitBreakdown
            );
            assert splitInvoice != null : "Split invoice must be created";
            assert splitBreakdown.equals(splitInvoice.getPaymentBreakdown()) : "Split breakdown must match: " + splitInvoice.getPaymentBreakdown();
            String htmlSplit = InvoiceGenerator.generateHtmlInvoice(splitInvoice, store.getSettings());
            assert htmlSplit.contains("Split:") : "HTML invoice must contain Split payment breakdown";
            String thermalSplit = InvoiceGenerator.generateThermalReceipt(splitInvoice, store.getSettings());
            assert thermalSplit.contains("Split  :") : "Thermal receipt must contain Split payment line";
            System.out.println("[PASS] 15. Split Payment verified: " + splitInvoice.getPaymentBreakdown());

            // 16. Test Sales Returns & Refunds (Invoice Cancellation, Stock Restoration & Warranty Void)
            int stockBeforeRefund = store.getProductById(macbook.getId()).getStockQuantity();
            WarrantyRecord macWr = warrantyService.findBySerial(testMacSerial);
            assert macWr != null && !macWr.isVoided() : "Warranty must be active before return";

            boolean refundSuccess = billingService.processRefund(splitInvoice.getInvoiceId(), "Defective screen return", true);
            assert refundSuccess : "Refund processing must succeed";
            Invoice refundedInvoice = store.getInvoiceById(splitInvoice.getInvoiceId());
            assert refundedInvoice.isRefunded() : "Invoice must be marked as REFUNDED";
            assert "REFUNDED".equals(refundedInvoice.getStatus()) : "Invoice status must be REFUNDED";
            assert refundedInvoice.getRefundAmount() == refundedInvoice.getGrandTotal() : "Refund amount must match grand total";
            assert "Defective screen return".equals(refundedInvoice.getRefundReason()) : "Refund reason must match";
            int stockAfterRefund = store.getProductById(macbook.getId()).getStockQuantity();
            assert stockAfterRefund == stockBeforeRefund + 1 : "Stock must be restored by 1 after refund";
            assert macWr.isVoided() : "Warranty record must be marked VOID_RETURNED after return";
            String htmlRefund = InvoiceGenerator.generateHtmlInvoice(refundedInvoice, store.getSettings());
            assert htmlRefund.contains("INVOICE REFUNDED &amp; CANCELLED") : "HTML invoice must show refund banner";
            System.out.println("[PASS] 16. Sales Return & Refund verified: Stock restored (" + stockBeforeRefund + " -> " + stockAfterRefund + ") and warranty voided.");

            // 17. Test Export to CSV (Sales Invoices & Inventory Catalog)
            File testSalesCsv = new File("invoices", "test_sales_export.csv");
            File testInvCsv = new File("invoices", "test_inventory_export.csv");
            try {
                CsvExportService.exportInvoicesToCsv(testSalesCsv, store.getAllInvoices());
                assert testSalesCsv.exists() && testSalesCsv.length() > 50 : "Sales CSV must be generated with content";
                String salesCsvContent = java.nio.file.Files.readString(testSalesCsv.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                assert salesCsvContent.contains("Invoice ID") && salesCsvContent.contains("Refund Reason") : "Sales CSV must contain proper headers";

                CsvExportService.exportInventoryToCsv(testInvCsv, store.getAllProducts());
                assert testInvCsv.exists() && testInvCsv.length() > 50 : "Inventory CSV must be generated with content";
                String invCsvContent = java.nio.file.Files.readString(testInvCsv.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                assert invCsvContent.contains("Product ID") && invCsvContent.contains("Low Stock Warning") : "Inventory CSV must contain proper headers";
                System.out.println("[PASS] 17. CSV Export for Sales & Inventory verified successfully.");
            } finally {
                if (testSalesCsv.exists()) testSalesCsv.delete();
                if (testInvCsv.exists()) testInvCsv.delete();
            }

            // 18. Test Direct Windows Print Preview Dialog setup
            assert new InvoicePreviewDialog(null, splitInvoice) != null : "InvoicePreviewDialog with Direct Print button must instantiate cleanly";
            System.out.println("[PASS] 18. Direct Print A4 Dialog initialized and verified.");

            // 19. Test Warranty Cancellation
            String cancelSerial = "SN-WARRANTY-CANCEL-001";
            warrantyService.registerWarranty(cancelSerial, "INV-TEST-CANCEL", macbook, new Customer("C-9", "Cancel User", "9123456780", "", "", ""), java.time.LocalDate.now());
            WarrantyRecord toCancel = warrantyService.findBySerial(cancelSerial);
            assert toCancel != null && !toCancel.isCancelled() : "Warranty must exist and not be cancelled initially";
            boolean cancelSuccess = warrantyService.cancelWarranty(cancelSerial);
            assert cancelSuccess : "Warranty cancellation must succeed";
            WarrantyRecord cancelledRec = warrantyService.findBySerial(cancelSerial);
            assert cancelledRec.isCancelled() : "Warranty must be marked as CANCELLED";
            assert "CANCELLED".equals(cancelledRec.getStatusDescription()) : "Warranty status description must be CANCELLED";
            assert new WarrantyPanel(warrantyService) != null : "WarrantyPanel with cancel button must instantiate cleanly";
            System.out.println("[PASS] 19. Warranty Cancellation verified: " + cancelSerial + " successfully cancelled.");

            // 20. Test Dynamic Category Reflection in Inventory & Stock / Billing
            String novelCategory = "Gaming Consoles " + System.currentTimeMillis();
            Product console = new Product(
                    "PROD-TEST-DYN-01",
                    "SKU-CONSOLE-01",
                    "PlayStation 5 Pro",
                    "Sony",
                    novelCategory,
                    "CFI-7000",
                    599.99,
                    699.99,
                    18.0,
                    10,
                    12,
                    true
            );
            invService.saveProduct(console);
            assert invService.getAllCategories().contains(novelCategory) : "New category must be present in inventoryService";

            com.electro.ui.InventoryPanel invPanel = new com.electro.ui.InventoryPanel(null, invService);
            invPanel.refreshTable();
            boolean invHasNovel = false;
            for (int i = 0; i < invPanel.getCategoryCombo().getItemCount(); i++) {
                if (novelCategory.equals(invPanel.getCategoryCombo().getItemAt(i))) {
                    invHasNovel = true;
                    break;
                }
            }
            assert invHasNovel : "Inventory category dropdown must dynamically include the new category";

            com.electro.ui.BillingPanel billPanel = new com.electro.ui.BillingPanel(null, billingService, invService);
            billPanel.refreshProductList();
            boolean billHasNovel = false;
            for (int i = 0; i < billPanel.getCategoryCombo().getItemCount(); i++) {
                if (novelCategory.equals(billPanel.getCategoryCombo().getItemAt(i))) {
                    billHasNovel = true;
                    break;
                }
            }
            assert billHasNovel : "Billing category dropdown must dynamically include the new category";

            // Now delete the product and verify dynamic removal from category dropdowns
            invService.deleteProduct(console.getId());
            assert !invService.getAllCategories().contains(novelCategory) : "Deleted category must no longer exist in inventoryService";
            invPanel.refreshTable();
            billPanel.refreshProductList();

            invHasNovel = false;
            for (int i = 0; i < invPanel.getCategoryCombo().getItemCount(); i++) {
                if (novelCategory.equals(invPanel.getCategoryCombo().getItemAt(i))) {
                    invHasNovel = true;
                    break;
                }
            }
            assert !invHasNovel : "Inventory category dropdown must dynamically remove the deleted category";

            billHasNovel = false;
            for (int i = 0; i < billPanel.getCategoryCombo().getItemCount(); i++) {
                if (novelCategory.equals(billPanel.getCategoryCombo().getItemAt(i))) {
                    billHasNovel = true;
                    break;
                }
            }
            assert !billHasNovel : "Billing category dropdown must dynamically remove the deleted category";
            System.out.println("[PASS] 20. Dynamic category reflection verified: '" + novelCategory + "' dynamically synced in Inventory and Billing dropdowns.");

            // 21. Test Direct Shop Owner Launch and Admin Login After Logout
            auth.logout();
            assert !auth.isLoggedIn() : "AuthService should have no active user after logout";

            boolean autoOwnerLogin = auth.loginAsDefaultOwner();
            assert autoOwnerLogin : "Direct auto-login as Store Owner must succeed";
            assert auth.getCurrentUser().isStoreOwner() : "Current user must be Store Owner";
            assert "owner".equalsIgnoreCase(auth.getCurrentUser().getUsername()) : "Username must be owner";
            assert auth.getCurrentUser().canAccessAnalytics() : "Store Owner must access Analytics";
            assert !auth.getCurrentUser().canAccessSettings() : "Store Owner must not access Shop Settings";

            // Simulate logout from MainFrame
            auth.logout();
            assert !auth.isLoggedIn() : "Must be logged out before Admin login";

            // Verify LoginDialog instantiates cleanly without errors and Admin can authenticate
            LoginDialog loginDlg = new LoginDialog(null);
            assert loginDlg != null : "LoginDialog must instantiate properly";
            loginDlg.dispose();

            boolean adminLoginAfterLogout = auth.login("admin", "admin123");
            assert adminLoginAfterLogout : "Admin login after logout must succeed";
            assert auth.getCurrentUser().isAdmin() : "Current user must be Administrator";
            assert auth.getCurrentUser().canAccessSettings() : "Admin must have full access to Shop Settings";
            System.out.println("[PASS] 21. Direct Shop Owner startup login and Admin post-logout login verified.");

            // 22. Test Admin Column Details Customization & CRUD Operations (ID, SKU, Brand, ProductName, etc.)
            settings = store.getSettings();

            // Read: Initial columns list
            List<ColumnConfig> initialConfigs = settings.getColumnConfigs();
            assert initialConfigs != null && initialConfigs.size() >= 13 : "Should have at least 13 default system columns";

            // Update: Edit system column display names
            settings.setColumnDisplayName("ID", "Item Code");
            settings.setColumnDisplayName("SKU", "Barcode");
            settings.setColumnDisplayName("Brand", "Manufacturer");
            settings.setColumnDisplayName("Product Name", "Item Description");
            settings.setColumnDisplayName("Retail", "Selling MRP");
            settings.setColumnDisplayName("Wholesale", "B2B Price");

            assert "Item Code".equals(settings.getColumnDisplayName("ID")) : "ID column name should be 'Item Code'";
            assert "Barcode".equals(settings.getColumnDisplayName("SKU")) : "SKU column name should be 'Barcode'";
            assert "Manufacturer".equals(settings.getColumnDisplayName("Brand")) : "Brand column name should be 'Manufacturer'";
            assert "Item Description".equals(settings.getColumnDisplayName("Product Name")) : "Product Name column should be 'Item Description'";
            assert "Item Description".equals(settings.getColumnDisplayName("ProductName")) : "ProductName alias should resolve to 'Item Description'";
            assert "Selling MRP".equals(settings.getColumnDisplayName("Retail")) : "Retail column should be 'Selling MRP'";
            assert "B2B Price".equals(settings.getColumnDisplayName("Wholesale")) : "Wholesale column should be 'B2B Price'";

            // Create: Admin creates a new custom column
            ColumnConfig customCol = new ColumnConfig("COLOR", "Color Variant", "Device physical color/finish", false, true);
            boolean added = settings.addColumnConfig(customCol);
            assert added : "Admin should be able to create new custom column";
            assert settings.getColumnConfig("COLOR") != null : "Custom column 'COLOR' should be retrievable";
            assert "Color Variant".equals(settings.getColumnConfig("COLOR").getDisplayName()) : "Custom column display name should match";

            // Assign custom field on product
            iphone.setCustomField("COLOR", "Titanium Gray");
            assert "Titanium Gray".equals(iphone.getCustomField("COLOR")) : "Product should retain custom field value";
            invService.saveProduct(iphone);

            // Test Persistence of Settings & Custom Columns
            store.saveSettings();

            // Test Inventory and Billing Table Updates
            invPanel.updateColumnHeaders();
            billPanel.updateColumnHeaders();

            // Test CSV Export reflecting Admin custom column headers
            java.io.File tempColCsv = java.io.File.createTempFile("inv_cols_test_", ".csv");
            tempColCsv.deleteOnExit();
            CsvExportService.exportInventoryToCsv(tempColCsv, invService.getAllProducts());
            String colCsvContent = new String(java.nio.file.Files.readAllBytes(tempColCsv.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            assert colCsvContent.contains("Item Code") : "CSV header should contain customized 'Item Code'";
            assert colCsvContent.contains("Barcode") : "CSV header should contain customized 'Barcode'";
            assert colCsvContent.contains("Manufacturer") : "CSV header should contain customized 'Manufacturer'";
            assert colCsvContent.contains("Item Description") : "CSV header should contain customized 'Item Description'";
            assert colCsvContent.contains("Selling MRP") : "CSV header should contain customized 'Selling MRP'";

            // Test ColumnConfigDialog instantiates and opens cleanly
            ColumnConfigDialog colDlg = new ColumnConfigDialog(null, null);
            assert colDlg != null : "ColumnConfigDialog must instantiate properly";
            colDlg.dispose();

            // Update: Admin updates custom column details
            boolean updated = settings.updateColumnConfig("COLOR", "Device Color", "Updated description", false);
            assert updated : "Admin should be able to update column config";
            assert "Device Color".equals(settings.getColumnConfig("COLOR").getDisplayName()) : "Display name should be updated";

            // Delete: Admin deletes custom column
            boolean deleted = settings.deleteColumnConfig("COLOR");
            assert deleted : "Admin should be able to delete custom column";
            assert settings.getColumnConfig("COLOR") == null : "Custom column should no longer exist after delete";

            // Reset back to standard defaults
            settings.resetColumnNamesToDefault();
            assert "Product Name".equals(settings.getColumnDisplayName("Product Name")) : "Should revert to 'Product Name'";
            assert "ID".equals(settings.getColumnDisplayName("ID")) : "Should revert to 'ID'";
            store.saveSettings();
            invPanel.updateColumnHeaders();
            billPanel.updateColumnHeaders();

            System.out.println("[PASS] 22. Admin column details full CRUD (Create custom column, Read configs, Update display names/visibility, Delete custom column, Persistence, and Dialog) verified.");

            // 23. Test Billing POS Barcode Auto-Fill and Quantity Increments
            billPanel.getBarcodeScanField().setText("IPH-15P-128");
            boolean scanOk = billPanel.handleBarcodeScan("IPH-15P-128");
            assert scanOk : "Barcode scanning should successfully find and add product by SKU/Barcode";

            // Verify product is now in cart table
            JTable cTable = billPanel.getCartTable();
            assert cTable.getRowCount() >= 1 : "Cart table must contain the scanned item";
            assert "IPH-15P-128".equals(cTable.getValueAt(0, 0)) : "First column must reflect the scanned barcode/SKU";
            assert cTable.getValueAt(0, 1).toString().contains("iPhone 15 Pro") : "Product details must auto-fill upon barcode entry";
            assert Integer.parseInt(cTable.getValueAt(0, 4).toString()) == 1 : "Initial quantity must be 1";

            // Verify increasing quantity by modifying count
            cTable.setValueAt(3, 0, 4); // update Qty column to 3
            // Trigger direct update to simulate user changing count in table cell
            billingService.updateQuantity(iphone.getId(), 3);
            billPanel.updateCartTable();
            assert Integer.parseInt(cTable.getValueAt(0, 4).toString()) == 3 : "Cart table must update to new quantity count (3)";

            // Verify scanning another item via Barcode lookup
            Product soundbar = store.getProductById("P1009"); // JBL Flip 6
            if (soundbar != null) {
                boolean scan2 = billPanel.handleBarcodeScan(soundbar.getSku());
                assert scan2 : "Should scan and add second item by barcode";
                assert cTable.getRowCount() == 2 : "Cart table must now have 2 rows";
            }

            // Verify entering product name directly auto-fills product details
            boolean scanByName = billPanel.handleBarcodeScan("MacBook Air");
            assert scanByName : "Should find and add product by entering Product Name";
            assert cTable.getRowCount() == 3 : "Cart table must now have 3 rows";
            assert cTable.getValueAt(2, 1).toString().contains("MacBook") : "Row 3 must contain MacBook product details";

            // Clean up cart after test
            billingService.clearCart();
            billPanel.updateCartTable();
            assert cTable.getRowCount() == 0 : "Cart table should be empty after clear";

            System.out.println("[PASS] 23. Billing POS barcode & product name auto-fill in 1st column and dynamic quantity increment verified.");

            // 24. Test Barcode/Item Scanner Product Items Popup & Search
            billPanel.updateProductSearchList("");
            DefaultListModel<Product> searchModel = billPanel.getProductSearchListModel();
            assert searchModel != null : "Product search list model must be initialized";
            assert searchModel.getSize() == store.getAllProducts().size() : "Product search list should contain all product items initially";

            // Test filtering by typing 'MacBook'
            billPanel.updateProductSearchList("MacBook");
            assert searchModel.getSize() >= 1 : "Should find matching products for 'MacBook'";
            assert searchModel.getElementAt(0).getName().contains("MacBook") : "First match should be MacBook";

            // Test filtering by SKU 'IPH-15P'
            billPanel.updateProductSearchList("IPH-15P");
            assert searchModel.getSize() >= 1 : "Should find matching products for 'IPH-15P'";
            assert searchModel.getElementAt(0).getSku().startsWith("IPH-15P") : "Match should be iPhone SKU";

            System.out.println("[PASS] 24. Barcode/Item scanner product items popup and dynamic search filter verified.");

            System.out.println("==================================================");
            System.out.println(" ALL 24 SYSTEM TESTS PASSED SUCCESSFULLY! [OK]");
            System.out.println("==================================================");

        } catch (Throwable t) {
            System.err.println("[FAIL] Test failed with error: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
