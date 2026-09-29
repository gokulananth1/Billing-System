package com.electro.service;

import com.electro.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service orchestrating Point of Sale (POS) cart, checkout, and invoice generation.
 */
public class BillingService {
    private final DataStore store;
    private final WarrantyService warrantyService;
    public enum CustomerClassification {
        RETAIL("Retail Customer (MRP)"),
        WHOLESALE("Wholesale / B2B (Discounted Rate)");

        private final String displayName;
        CustomerClassification(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
        @Override
        public String toString() {
            return displayName;
        }
    }

    private final List<CartItem> cart = new ArrayList<>();
    private double overallDiscountPercent = 0.0;
    private CustomerClassification customerClassification = CustomerClassification.RETAIL;

    public BillingService() {
        this.store = DataStore.getInstance();
        this.warrantyService = new WarrantyService();
    }

    public synchronized CustomerClassification getCustomerClassification() {
        return customerClassification;
    }

    public synchronized void setCustomerClassification(CustomerClassification classification) {
        this.customerClassification = classification != null ? classification : CustomerClassification.RETAIL;
        // Recalculate price for all current items in the cart based on classification!
        for (CartItem item : cart) {
            double price = (this.customerClassification == CustomerClassification.WHOLESALE)
                    ? item.getProduct().getWholesalePrice()
                    : item.getProduct().getSellingPrice();
            item.setUnitPrice(price);
        }
    }

    public synchronized List<CartItem> getCart() {
        return new ArrayList<>(cart);
    }

    public synchronized void clearCart() {
        cart.clear();
        overallDiscountPercent = 0.0;
    }

    public synchronized void addToCart(Product product, int quantity) {
        if (product == null || quantity <= 0) return;

        // Check if product already exists in cart
        for (CartItem item : cart) {
            if (item.getProduct().getId().equals(product.getId())) {
                int newQty = item.getQuantity() + quantity;
                if (newQty <= product.getStockQuantity()) {
                    item.setQuantity(newQty);
                }
                return;
            }
        }

        // New item
        if (quantity <= product.getStockQuantity()) {
            CartItem item = new CartItem(product, quantity);
            double price = (this.customerClassification == CustomerClassification.WHOLESALE)
                    ? product.getWholesalePrice()
                    : product.getSellingPrice();
            item.setUnitPrice(price);
            cart.add(item);
        }
    }

    public synchronized void updateQuantity(String productId, int newQuantity) {
        for (Iterator<CartItem> it = cart.iterator(); it.hasNext();) {
            CartItem item = it.next();
            if (item.getProduct().getId().equals(productId)) {
                if (newQuantity <= 0) {
                    it.remove();
                } else if (newQuantity <= item.getProduct().getStockQuantity()) {
                    item.setQuantity(newQuantity);
                    // Trim serials if quantity reduced
                    while (item.getSerialNumbers().size() > newQuantity) {
                        item.getSerialNumbers().remove(item.getSerialNumbers().size() - 1);
                    }
                }
                return;
            }
        }
    }

    public synchronized void removeItem(String productId) {
        cart.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    public synchronized void setItemSerials(String productId, List<String> serials) {
        for (CartItem item : cart) {
            if (item.getProduct().getId().equals(productId)) {
                item.setSerialNumbers(new ArrayList<>(serials));
                return;
            }
        }
    }

    public synchronized void setItemDiscount(String productId, double discountPercent) {
        for (CartItem item : cart) {
            if (item.getProduct().getId().equals(productId)) {
                item.setDiscountPercent(Math.max(0, Math.min(100, discountPercent)));
                return;
            }
        }
    }

    public synchronized void setOverallDiscountPercent(double percent) {
        this.overallDiscountPercent = Math.max(0, Math.min(100, percent));
    }

    public synchronized double getOverallDiscountPercent() {
        return overallDiscountPercent;
    }

    public synchronized double calculateSubtotal() {
        double subtotal = 0;
        for (CartItem item : cart) {
            subtotal += item.getRawSubtotal();
        }
        return subtotal;
    }

    public synchronized double calculateItemDiscounts() {
        double discounts = 0;
        for (CartItem item : cart) {
            discounts += item.getDiscountAmount();
        }
        return discounts;
    }

    public synchronized double calculateOverallDiscountAmount() {
        double subtotalAfterItemDiscounts = calculateSubtotal() - calculateItemDiscounts();
        return (subtotalAfterItemDiscounts * overallDiscountPercent) / 100.0;
    }

    public synchronized double calculateTotalDiscounts() {
        return calculateItemDiscounts() + calculateOverallDiscountAmount();
    }

    public synchronized double calculateTaxableAmount() {
        return Math.max(0, calculateSubtotal() - calculateTotalDiscounts());
    }

    public synchronized double calculateTotalTax() {
        if (!store.getSettings().isEnableGstBilling()) {
            return 0.0;
        }
        double totalTax = 0;
        // Distribute overall discount proportionately across items to calculate precise GST
        double totalSub = calculateSubtotal() - calculateItemDiscounts();
        double ratio = totalSub > 0 ? (1.0 - (overallDiscountPercent / 100.0)) : 1.0;

        for (CartItem item : cart) {
            double effectiveTaxable = item.getTaxableAmount() * ratio;
            totalTax += (effectiveTaxable * item.getTaxRate()) / 100.0;
        }
        return totalTax;
    }

    public synchronized double calculateCgst() {
        return calculateTotalTax() / 2.0;
    }

    public synchronized double calculateSgst() {
        return calculateTotalTax() / 2.0;
    }

    public synchronized double calculateGrandTotal() {
        return Math.round((calculateTaxableAmount() + calculateTotalTax()) * 100.0) / 100.0;
    }

    public synchronized String validateCartForCheckout() {
        if (cart.isEmpty()) {
            return "Cart is empty. Please add products to bill.";
        }
        for (CartItem item : cart) {
            Product p = item.getProduct();
            if (item.getQuantity() > p.getStockQuantity()) {
                return "Insufficient stock for " + p.getName() + " (Available: " + p.getStockQuantity() + ")";
            }
            if (p.isRequiresSerial() && store.getSettings().isStrictSerialTracking()) {
                if (item.getSerialNumbers().size() != item.getQuantity()) {
                    return "Serial/IMEI number missing for " + p.getName() + 
                           " (Requires " + item.getQuantity() + ", provided " + item.getSerialNumbers().size() + ")";
                }
                // Check uniqueness in cart
                Set<String> unique = new HashSet<>(item.getSerialNumbers());
                if (unique.size() != item.getSerialNumbers().size()) {
                    return "Duplicate serial numbers entered for " + p.getName();
                }
            }
        }
        return null; // Valid!
    }

    public synchronized Invoice checkout(Customer customer, String paymentMethod, String paymentRef, String notes) {
        return checkout(customer, paymentMethod, paymentRef, notes, "");
    }

    public synchronized Invoice checkout(Customer customer, String paymentMethod, String paymentRef, String notes, String paymentBreakdown) {
        String validationError = validateCartForCheckout();
        if (validationError != null) {
            throw new IllegalStateException(validationError);
        }

        // Generate Invoice ID
        String prefix = store.getSettings().getInvoicePrefix();
        if (prefix == null || prefix.trim().isEmpty()) prefix = "INV";
        String datePrefix = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        int count = store.getAllInvoices().size() + 101;
        String invoiceId = prefix.trim().toUpperCase() + "-" + datePrefix + "-" + count;

        if (customer == null) {
            customer = new Customer("C-GUEST", "Walk-in Customer", "", "", "", "", customerClassification.name());
        } else {
            customer.setCustomerType(customerClassification.name());
            if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
                store.saveCustomer(customer);
            }
        }

        // Deduct inventory stock and register warranties
        LocalDate today = LocalDate.now();
        List<CartItem> billedItems = new ArrayList<>();
        for (CartItem item : cart) {
            Product p = item.getProduct();
            store.updateStock(p.getId(), -item.getQuantity());

            if (p.isRequiresSerial() && item.getSerialNumbers() != null) {
                for (String serial : item.getSerialNumbers()) {
                    warrantyService.registerWarranty(serial, invoiceId, p, customer, today);
                }
            }
            billedItems.add(item);
        }

        Invoice invoice = new Invoice(
                invoiceId,
                customer,
                billedItems,
                calculateSubtotal(),
                calculateTotalDiscounts(),
                calculateTaxableAmount(),
                calculateCgst(),
                calculateSgst(),
                calculateGrandTotal(),
                paymentMethod,
                paymentRef,
                notes
        );
        if (paymentBreakdown != null && !paymentBreakdown.trim().isEmpty()) {
            invoice.setPaymentBreakdown(paymentBreakdown.trim());
        }

        store.addInvoice(invoice);
        clearCart();
        return invoice;
    }

    public synchronized boolean processRefund(String invoiceId, String reason, boolean returnStockToInventory) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) return false;
        Invoice invoice = store.getInvoiceById(invoiceId.trim());
        if (invoice == null || invoice.isRefunded()) {
            return false;
        }

        invoice.setStatus("REFUNDED");
        invoice.setRefundAmount(invoice.getGrandTotal());
        invoice.setRefundReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Customer Return / Refund");
        invoice.setRefundDateTime(LocalDateTime.now().format(Invoice.FORMATTER));

        if (returnStockToInventory && invoice.getItems() != null) {
            for (CartItem item : invoice.getItems()) {
                if (item.getProduct() != null && item.getProduct().getId() != null) {
                    store.updateStock(item.getProduct().getId(), item.getQuantity());
                }
            }
        }

        // Invalidate associated warranty registrations
        warrantyService.voidWarrantyForInvoice(invoice.getInvoiceId());

        store.saveInvoicesData();
        return true;
    }
}
