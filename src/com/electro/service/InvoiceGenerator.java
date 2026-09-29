package com.electro.service;

import com.electro.model.*;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates styled HTML tax invoices, thermal POS receipts, and handles system printing.
 */
public class InvoiceGenerator {

    public static String generateHtmlInvoice(Invoice invoice, ShopSettings settings) {
        String sym = settings.getCurrencySymbol();
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<title>Tax Invoice - ")
          .append(invoice.getInvoiceId()).append("</title>\n")
          .append("<style>\n")
          .append("  body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; color: #1e293b; background: #fff; line-height: 1.5; }\n")
          .append("  .invoice-box { max-width: 800px; margin: auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }\n")
          .append("  .header { display: flex; justify-content: space-between; border-bottom: 2px solid #2563eb; padding-bottom: 15px; margin-bottom: 20px; }\n")
          .append("  .header h1 { margin: 0; color: #1e3a8a; font-size: 24px; font-weight: 800; text-transform: uppercase; }\n")
          .append("  .header .tagline { font-size: 13px; color: #64748b; margin-top: 2px; }\n")
          .append("  .shop-details { font-size: 12px; color: #475569; margin-top: 6px; }\n")
          .append("  .invoice-meta { text-align: right; font-size: 13px; }\n")
          .append("  .invoice-meta .inv-number { font-size: 18px; font-weight: bold; color: #2563eb; }\n")
          .append("  .info-grid { display: flex; justify-content: space-between; margin-bottom: 20px; background: #f8fafc; padding: 12px; border-radius: 6px; border: 1px solid #edf2f7; }\n")
          .append("  .info-col { width: 48%; font-size: 13px; }\n")
          .append("  .info-col h3 { margin: 0 0 6px 0; font-size: 13px; text-transform: uppercase; color: #475569; letter-spacing: 0.5px; }\n")
          .append("  table.items-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 13px; }\n")
          .append("  table.items-table th { background: #f1f5f9; color: #334155; text-align: left; padding: 10px 8px; border-bottom: 2px solid #cbd5e1; font-weight: 600; }\n")
          .append("  table.items-table td { padding: 10px 8px; border-bottom: 1px solid #e2e8f0; vertical-align: top; }\n")
          .append("  .item-title { font-weight: 600; color: #0f172a; }\n")
          .append("  .item-serials { font-size: 11px; color: #0284c7; margin-top: 3px; font-family: monospace; background: #f0f9ff; padding: 2px 6px; border-radius: 4px; display: inline-block; }\n")
          .append("  .item-warranty { font-size: 11px; color: #16a34a; font-weight: 600; }\n")
          .append("  .summary-wrap { display: flex; justify-content: flex-end; margin-bottom: 20px; }\n")
          .append("  .summary-table { width: 320px; border-collapse: collapse; font-size: 13px; }\n")
          .append("  .summary-table td { padding: 6px 8px; }\n")
          .append("  .summary-table .total-row { font-size: 16px; font-weight: bold; background: #eff6ff; color: #1d4ed8; border-top: 2px solid #3b82f6; border-bottom: 2px solid #3b82f6; }\n")
          .append("  .terms { margin-top: 25px; padding-top: 15px; border-top: 1px solid #e2e8f0; font-size: 11px; color: #64748b; }\n")
          .append("  .terms h4 { margin: 0 0 6px 0; color: #334155; text-transform: uppercase; font-size: 11px; }\n")
          .append("  .signatures { display: flex; justify-content: space-between; margin-top: 40px; padding-top: 15px; font-size: 12px; color: #475569; }\n")
          .append("  .sig-block { text-align: center; border-top: 1px dashed #94a3b8; width: 180px; padding-top: 6px; }\n")
          .append("  @media print {\n")
          .append("    body { margin: 0; }\n")
          .append("    .invoice-box { border: none; box-shadow: none; padding: 0; }\n")
          .append("  }\n")
          .append("</style>\n</head>\n<body>\n")
          .append("<div class=\"invoice-box\">\n");

        if (invoice.isRefunded()) {
            sb.append("  <div style=\"background:#fee2e2; border:2px solid #ef4444; color:#b91c1c; padding:10px 14px; border-radius:6px; margin-bottom:16px; font-weight:bold; font-size:15px; text-align:center;\">\n")
              .append("    *** INVOICE REFUNDED &amp; CANCELLED ***\n")
              .append("    <div style=\"font-size:11px; font-weight:normal; margin-top:4px;\">Refund Date: <strong>").append(escapeHtml(invoice.getRefundDateTime())).append("</strong> | Reason: <strong>").append(escapeHtml(invoice.getRefundReason())).append("</strong></div>\n")
              .append("  </div>\n");
        }

        // Header
        sb.append("  <div class=\"header\">\n")
          .append("    <div>\n")
          .append("      <h1>").append(escapeHtml(settings.getStoreName())).append("</h1>\n")
          .append("      <div class=\"tagline\">").append(escapeHtml(settings.getTagline())).append("</div>\n")
          .append("      <div class=\"shop-details\">\n")
          .append("        ").append(escapeHtml(settings.getAddress())).append("<br>\n")
          .append("        Phone: ").append(escapeHtml(settings.getPhone())).append(" | Email: ").append(escapeHtml(settings.getEmail())).append("<br>\n")
          .append("        <strong>GSTIN:</strong> ").append(escapeHtml(settings.getGstin())).append("\n")
          .append("      </div>\n")
          .append("    </div>\n")
          .append("    <div class=\"invoice-meta\">\n")
          .append("      <div style=\"color:#64748b; font-size:11px; text-transform:uppercase;\">Tax Invoice</div>\n")
          .append("      <div class=\"inv-number\">").append(escapeHtml(invoice.getInvoiceId())).append("</div>\n")
          .append("      <div style=\"margin-top:4px;\">Date: <strong>").append(escapeHtml(invoice.getDateTime())).append("</strong></div>\n");
        if (invoice.isRefunded()) {
            sb.append("      <div style=\"color:#dc2626; font-weight:bold;\">Status: REFUNDED</div>\n");
        }
        sb.append("      <div>Payment: <strong>").append(escapeHtml(invoice.getPaymentMethod().toUpperCase())).append("</strong></div>\n");
        if (invoice.getPaymentBreakdown() != null && !invoice.getPaymentBreakdown().isEmpty()) {
            sb.append("      <div style=\"font-size:11px; color:#475569;\">Split: ").append(escapeHtml(invoice.getPaymentBreakdown())).append("</div>\n");
        }
        if (invoice.getPaymentReference() != null && !invoice.getPaymentReference().isEmpty() && !invoice.getPaymentReference().equals(invoice.getPaymentBreakdown())) {
            sb.append("      <div>Ref: ").append(escapeHtml(invoice.getPaymentReference())).append("</div>\n");
        }
        sb.append("    </div>\n")
          .append("  </div>\n");

        // Customer & Order Info
        Customer c = invoice.getCustomer();
        sb.append("  <div class=\"info-grid\">\n")
          .append("    <div class=\"info-col\">\n")
          .append("      <h3>Billed To (Customer)</h3>\n")
          .append("      <strong>").append(escapeHtml(c != null && !c.getName().isEmpty() ? c.getName() : "Walk-in Customer")).append("</strong><br>\n");
        if (c != null && c.getPhone() != null && !c.getPhone().isEmpty()) {
            sb.append("      Phone: ").append(escapeHtml(c.getPhone())).append("<br>\n");
        }
        if (c != null && c.getEmail() != null && !c.getEmail().isEmpty()) {
            sb.append("      Email: ").append(escapeHtml(c.getEmail())).append("<br>\n");
        }
        if (c != null && c.getAddress() != null && !c.getAddress().isEmpty()) {
            sb.append("      Address: ").append(escapeHtml(c.getAddress())).append("<br>\n");
        }
        if (c != null && c.getGstin() != null && !c.getGstin().isEmpty()) {
            sb.append("      Customer GSTIN: ").append(escapeHtml(c.getGstin())).append("<br>\n");
        }
        if (c != null && c.getCustomerType() != null) {
            String tier = c.isWholesale() ? "Wholesale (B2B)" : "Retail Customer";
            sb.append("      Customer Type: <strong>").append(tier).append("</strong><br>\n");
        }
        sb.append("    </div>\n")
          .append("    <div class=\"info-col\" style=\"text-align: right;\">\n")
          .append("      <h3>Warranty & Verification</h3>\n")
          .append("      Store Registered Invoice<br>\n")
          .append("      Authorized Electronics Retailer<br>\n")
          .append("      Total Items: <strong>").append(invoice.getTotalUnits()).append(" Units</strong>\n")
          .append("    </div>\n")
          .append("  </div>\n");

        // Items Table
        sb.append("  <table class=\"items-table\">\n")
          .append("    <thead>\n")
          .append("      <tr>\n")
          .append("        <th style=\"width:30px;\">#</th>\n")
          .append("        <th>Description & Serial/IMEI</th>\n")
          .append("        <th style=\"width:80px;\">Warranty</th>\n")
          .append("        <th style=\"width:45px; text-align:center;\">Qty</th>\n")
          .append("        <th style=\"width:90px; text-align:right;\">Unit Price</th>\n")
          .append("        <th style=\"width:60px; text-align:center;\">GST</th>\n")
          .append("        <th style=\"width:100px; text-align:right;\">Total</th>\n")
          .append("      </tr>\n")
          .append("    </thead>\n")
          .append("    <tbody>\n");

        int index = 1;
        for (CartItem item : invoice.getItems()) {
            Product p = item.getProduct();
            sb.append("      <tr>\n")
              .append("        <td>").append(index++).append("</td>\n")
              .append("        <td>\n")
              .append("          <div class=\"item-title\">").append(escapeHtml(p.getBrand())).append(" ").append(escapeHtml(p.getName())).append("</div>\n")
              .append("          <div style=\"font-size:11px; color:#64748b;\">Model: ").append(escapeHtml(p.getModelNumber())).append(" | SKU: ").append(escapeHtml(p.getSku())).append("</div>\n");

            if (!item.getSerialNumbers().isEmpty()) {
                sb.append("          <div class=\"item-serials\">S/N: ")
                  .append(escapeHtml(String.join(", ", item.getSerialNumbers())))
                  .append("</div>\n");
            }

            sb.append("        </td>\n")
              .append("        <td><span class=\"item-warranty\">").append(p.getWarrantyMonths()).append(" Months</span></td>\n")
              .append("        <td style=\"text-align:center;\">").append(item.getQuantity()).append("</td>\n")
              .append("        <td style=\"text-align:right;\">").append(sym).append(String.format("%,.2f", item.getUnitPrice())).append("</td>\n")
              .append("        <td style=\"text-align:center;\">").append((int)p.getTaxRate()).append("%</td>\n")
              .append("        <td style=\"text-align:right;\"><strong>").append(sym).append(String.format("%,.2f", item.getLineTotal())).append("</strong></td>\n")
              .append("      </tr>\n");
        }

        sb.append("    </tbody>\n")
          .append("  </table>\n");

        // Summary Table
        sb.append("  <div class=\"summary-wrap\">\n")
          .append("    <table class=\"summary-table\">\n")
          .append("      <tr>\n")
          .append("        <td>Gross Subtotal:</td>\n")
          .append("        <td style=\"text-align:right;\">").append(sym).append(String.format("%,.2f", invoice.getSubtotal())).append("</td>\n")
          .append("      </tr>\n");

        if (invoice.getTotalDiscount() > 0) {
            sb.append("      <tr>\n")
              .append("        <td style=\"color:#dc2626;\">Total Discount:</td>\n")
              .append("        <td style=\"text-align:right; color:#dc2626;\">-").append(sym).append(String.format("%,.2f", invoice.getTotalDiscount())).append("</td>\n")
              .append("      </tr>\n");
        }

        sb.append("      <tr>\n")
          .append("        <td>Taxable Value:</td>\n")
          .append("        <td style=\"text-align:right;\">").append(sym).append(String.format("%,.2f", invoice.getTaxableAmount())).append("</td>\n")
          .append("      </tr>\n")
          .append("      <tr>\n")
          .append("        <td>CGST:</td>\n")
          .append("        <td style=\"text-align:right;\">").append(sym).append(String.format("%,.2f", invoice.getCgstAmount())).append("</td>\n")
          .append("      </tr>\n")
          .append("      <tr>\n")
          .append("        <td>SGST:</td>\n")
          .append("        <td style=\"text-align:right;\">").append(sym).append(String.format("%,.2f", invoice.getSgstAmount())).append("</td>\n")
          .append("      </tr>\n")
          .append("      <tr class=\"total-row\">\n")
          .append("        <td>NET PAYABLE:</td>\n")
          .append("        <td style=\"text-align:right;\">").append(sym).append(String.format("%,.2f", invoice.getGrandTotal())).append("</td>\n")
          .append("      </tr>\n")
          .append("    </table>\n")
          .append("  </div>\n");

        // Terms and conditions
        sb.append("  <div class=\"terms\">\n")
          .append("    <h4>Terms & Conditions</h4>\n")
          .append("    <pre style=\"font-family: inherit; margin: 0; white-space: pre-wrap;\">")
          .append(escapeHtml(settings.getTermsAndConditions()))
          .append("</pre>\n")
          .append("    <div style=\"margin-top:10px; font-weight:600; text-align:center; color:#2563eb;\">")
          .append(escapeHtml(settings.getInvoiceFooter()))
          .append("</div>\n")
          .append("  </div>\n");

        // Signatures
        sb.append("  <div class=\"signatures\">\n")
          .append("    <div class=\"sig-block\">Customer Signature</div>\n")
          .append("    <div class=\"sig-block\">Authorized Signatory<br><strong>").append(escapeHtml(settings.getStoreName())).append("</strong></div>\n")
          .append("  </div>\n");

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    public static String generateThermalReceipt(Invoice invoice, ShopSettings settings) {
        String sym = settings.getCurrencySymbol();
        StringBuilder sb = new StringBuilder();
        String line = "------------------------------------------\n";
        String dline = "==========================================\n";

        sb.append(center(settings.getStoreName().toUpperCase(), 42)).append("\n");
        sb.append(center(settings.getTagline(), 42)).append("\n");
        sb.append(center(settings.getAddress(), 42)).append("\n");
        sb.append(center("Tel: " + settings.getPhone(), 42)).append("\n");
        sb.append(center("GSTIN: " + settings.getGstin(), 42)).append("\n");
        sb.append(dline);
        if (invoice.isRefunded()) {
            sb.append("******************************************\n");
            sb.append("***      INVOICE REFUNDED / VOID       ***\n");
            sb.append("******************************************\n");
            sb.append("Refund Date: ").append(invoice.getRefundDateTime()).append("\n");
            sb.append("Reason     : ").append(invoice.getRefundReason()).append("\n");
            sb.append(dline);
        }
        sb.append("Invoice: ").append(invoice.getInvoiceId()).append("\n");
        sb.append("Date   : ").append(invoice.getDateTime()).append("\n");
        Customer c = invoice.getCustomer();
        sb.append("Cust   : ").append(c != null ? c.getName() : "Walk-in").append(" (")
          .append(c != null ? c.getPhone() : "-").append(")\n");
        sb.append("Tier   : ").append(c != null && c.isWholesale() ? "WHOLESALE (B2B)" : "RETAIL").append("\n");
        sb.append("Mode   : ").append(invoice.getPaymentMethod().toUpperCase()).append("\n");
        if (invoice.getPaymentBreakdown() != null && !invoice.getPaymentBreakdown().isEmpty()) {
            sb.append("Split  : ").append(invoice.getPaymentBreakdown()).append("\n");
        }
        sb.append(line);
        sb.append(String.format("%-22s %3s %6s %8s\n", "ITEM", "QTY", "RATE", "TOTAL"));
        sb.append(line);

        for (CartItem item : invoice.getItems()) {
            Product p = item.getProduct();
            String name = p.getBrand() + " " + p.getName();
            if (name.length() > 22) name = name.substring(0, 20) + "..";
            sb.append(String.format("%-22s %3d %6.0f %8.2f\n", name, item.getQuantity(), item.getUnitPrice(), item.getLineTotal()));
            if (!item.getSerialNumbers().isEmpty()) {
                sb.append(" S/N: ").append(String.join(",", item.getSerialNumbers())).append("\n");
            }
            sb.append(" War: ").append(p.getWarrantyMonths()).append("M Warranty\n");
        }

        sb.append(line);
        sb.append(String.format("%-28s %12.2f\n", "Subtotal:", invoice.getSubtotal()));
        if (invoice.getTotalDiscount() > 0) {
            sb.append(String.format("%-28s %12.2f\n", "Discount:", -invoice.getTotalDiscount()));
        }
        sb.append(String.format("%-28s %12.2f\n", "Taxable Value:", invoice.getTaxableAmount()));
        sb.append(String.format("%-28s %12.2f\n", "CGST:", invoice.getCgstAmount()));
        sb.append(String.format("%-28s %12.2f\n", "SGST:", invoice.getSgstAmount()));
        sb.append(dline);
        sb.append(String.format("%-25s %s%12.2f\n", "GRAND TOTAL:", sym, invoice.getGrandTotal()));
        sb.append(dline);
        sb.append(center(settings.getInvoiceFooter(), 42)).append("\n");
        sb.append(center("Keep this bill for warranty claims.", 42)).append("\n\n\n");

        return sb.toString();
    }

    public static File saveHtmlInvoiceToFile(Invoice invoice, ShopSettings settings) {
        try {
            Path invoicesDir = Path.of("invoices");
            if (!Files.exists(invoicesDir)) {
                Files.createDirectories(invoicesDir);
            }
            String filename = invoice.getInvoiceId() + ".html";
            Path filePath = invoicesDir.resolve(filename);
            String html = generateHtmlInvoice(invoice, settings);
            Files.writeString(filePath, html, StandardCharsets.UTF_8);
            return filePath.toFile();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void printReceipt(Invoice invoice, ShopSettings settings) {
        String receiptText = generateThermalReceipt(invoice, settings);
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                g2d.setFont(new Font("Monospaced", Font.PLAIN, 9));

                String[] lines = receiptText.split("\n");
                int y = 20;
                for (String l : lines) {
                    g2d.drawString(l, 10, y);
                    y += 12;
                }
                return PAGE_EXISTS;
            }
        });

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
    }

    private static String center(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private static String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;");
    }
}
