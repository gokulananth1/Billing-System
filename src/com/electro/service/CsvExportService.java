package com.electro.service;

import com.electro.model.CartItem;
import com.electro.model.Customer;
import com.electro.model.Invoice;
import com.electro.model.Product;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Service to export sales transactions and product inventory to CSV format.
 * Generates Excel-friendly CSV files with UTF-8 BOM and proper CSV quote escaping.
 */
public class CsvExportService {

    public static void exportInvoicesToCsv(File targetFile, List<Invoice> invoices) throws Exception {
        if (targetFile == null) {
            throw new IllegalArgumentException("Target file cannot be null");
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {

            // Write UTF-8 BOM for Microsoft Excel compatibility
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            // CSV Header
            bw.write(String.join(",",
                    "\"Invoice ID\"",
                    "\"Date & Time\"",
                    "\"Status\"",
                    "\"Customer Name\"",
                    "\"Customer Phone\"",
                    "\"Customer Email\"",
                    "\"Classification\"",
                    "\"Total Units\"",
                    "\"Subtotal\"",
                    "\"Discount\"",
                    "\"Taxable Amount\"",
                    "\"CGST\"",
                    "\"SGST\"",
                    "\"Grand Total\"",
                    "\"Payment Mode\"",
                    "\"Payment Breakdown\"",
                    "\"Payment Reference\"",
                    "\"Refund Amount\"",
                    "\"Refund Reason\"",
                    "\"Refund Date\""
            ));
            bw.newLine();

            if (invoices != null) {
                for (Invoice inv : invoices) {
                    Customer c = inv.getCustomer();
                    String custName = c != null ? c.getName() : "Walk-in";
                    String custPhone = c != null ? c.getPhone() : "";
                    String custEmail = c != null ? c.getEmail() : "";
                    String custType = c != null ? c.getCustomerType() : "RETAIL";

                    bw.write(String.join(",",
                            escapeCsv(inv.getInvoiceId()),
                            escapeCsv(inv.getDateTime()),
                            escapeCsv(inv.getStatus()),
                            escapeCsv(custName),
                            escapeCsv(custPhone),
                            escapeCsv(custEmail),
                            escapeCsv(custType),
                            String.valueOf(inv.getTotalUnits()),
                            String.format("%.2f", inv.getSubtotal()),
                            String.format("%.2f", inv.getTotalDiscount()),
                            String.format("%.2f", inv.getTaxableAmount()),
                            String.format("%.2f", inv.getCgstAmount()),
                            String.format("%.2f", inv.getSgstAmount()),
                            String.format("%.2f", inv.getGrandTotal()),
                            escapeCsv(inv.getPaymentMethod()),
                            escapeCsv(inv.getPaymentBreakdown()),
                            escapeCsv(inv.getPaymentReference()),
                            String.format("%.2f", inv.getRefundAmount()),
                            escapeCsv(inv.getRefundReason()),
                            escapeCsv(inv.getRefundDateTime())
                    ));
                    bw.newLine();
                }
            }
        }
    }

    public static void exportInventoryToCsv(File targetFile, List<Product> products) throws Exception {
        if (targetFile == null) {
            throw new IllegalArgumentException("Target file cannot be null");
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {

            // Write UTF-8 BOM for Microsoft Excel compatibility
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            // CSV Header
            bw.write(String.join(",",
                    "\"Product ID\"",
                    "\"SKU\"",
                    "\"Product Name\"",
                    "\"Brand\"",
                    "\"Category\"",
                    "\"Model Number\"",
                    "\"Cost Price\"",
                    "\"Retail Price\"",
                    "\"Wholesale Price\"",
                    "\"GST Rate (%)\"",
                    "\"Stock Quantity\"",
                    "\"Low Stock Warning\"",
                    "\"Warranty (Months)\"",
                    "\"Requires Serial / IMEI\""
            ));
            bw.newLine();

            if (products != null) {
                for (Product p : products) {
                    bw.write(String.join(",",
                            escapeCsv(p.getId()),
                            escapeCsv(p.getSku()),
                            escapeCsv(p.getName()),
                            escapeCsv(p.getBrand()),
                            escapeCsv(p.getCategory()),
                            escapeCsv(p.getModelNumber()),
                            String.format("%.2f", p.getCostPrice()),
                            String.format("%.2f", p.getSellingPrice()),
                            String.format("%.2f", p.getWholesalePrice()),
                            String.format("%.1f", p.getTaxRate()),
                            String.valueOf(p.getStockQuantity()),
                            escapeCsv(p.isLowStock() ? "YES" : "NO"),
                            String.valueOf(p.getWarrantyMonths()),
                            escapeCsv(p.isRequiresSerial() ? "Yes" : "No")
                    ));
                    bw.newLine();
                }
            }
        }
    }

    private static String escapeCsv(String str) {
        if (str == null) return "\"\"";
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }
}
