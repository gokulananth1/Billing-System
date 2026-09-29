package com.electro.test;

import com.electro.model.*;
import com.electro.service.*;
import com.electro.ui.InvoicePreviewDialog;
import com.electro.ui.WarrantyPanel;

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

            boolean cashierLogin = auth.login("cashier", "cashier123");
            assert cashierLogin : "Cashier login with correct password must succeed";
            assert auth.getCurrentUser().isCashier() : "Cashier role check must return true";
            assert !auth.getCurrentUser().isAdmin() : "Cashier is not admin";
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
            System.out.println("[PASS] 11b. Security verification: Invalid logins blocked & User CRUD tested.");

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

            System.out.println("==================================================");
            System.out.println(" ALL 19 SYSTEM TESTS PASSED SUCCESSFULLY! [OK]");
            System.out.println("==================================================");

        } catch (Throwable t) {
            System.err.println("[FAIL] Test failed with error: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
