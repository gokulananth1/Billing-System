# VoltVault Electronics Shop Billing & POS System

A standalone Desktop Point of Sale (POS), Inventory, and Warranty Management System written in **Java (Java 21 SE)** specifically engineered for retail electronics stores.

---

## Key Highlights

- **Zero-Dependency Architecture**: Built on standard Java 21 SE (Swing, NIO, AWT Printing). No Maven, Gradle, or third-party JAR installations needed.
- **Serial / IMEI Tracking**: Mandatory serial/IMEI capture for phones, laptops, TVs, and serialized gadgets during billing.
- **Warranty Claim Engine**: Instant warranty status lookup by Serial Number, IMEI, or customer phone number with remaining days calculation.
- **GST / Tax Breakdown**: Automatic calculation of Subtotal, Item-level and Bill-level discounts, CGST, SGST, and Grand Total.
- **A4 Tax Invoices & Thermal POS Receipts**:
  - Full compliant A4 HTML tax invoice with shop logo, GSTIN, HSN codes, serial registry, and signature lines.
  - Formatted thermal POS slip for 80mm/58mm receipt printers with native system printing support (`PrinterJob`).
- **Low-Stock Alerts**: Visual badges and automated alerts for items reaching critically low stock ($\le 3$ units).
- **Persistent File Storage**: Fast thread-safe JSON-backed persistence under the `data/` directory.

---

## System Requirements

- **Java Development Kit (JDK)**: Java 17, 21, or higher (`javac` and `java` available on PATH).
- **Operating System**: Windows (compatible with macOS and Linux as well).

---

## Quick Start (Windows)

### 1. Compile the Application
Double-click `compile.bat` or run:
```cmd
javac -encoding UTF-8 -d bin -sourcepath src src\com\electro\Main.java
```

### 2. Launch the Application
Double-click `run.bat` or run:
```cmd
java -cp bin com.electro.Main
```

---

## Application Modules & Features

### 1. Billing POS Tab
- **Catalog Search**: Instant search by Name, Brand, Model, or SKU/Barcode with category filters (Smartphones, Laptops, Audio, TV & Home, Accessories).
- **Serial / IMEI Entry**: Automatically prompts cashier for serial/IMEI registration when adding serialized items (includes an **"Auto-Generate"** shortcut for demo speed).
- **Customer Directory**: Captures customer name, phone, email, and billing address. Auto-fills past customer details upon typing the phone number.
- **Discounts & Taxes**: Supports per-item discounts and overall bill percentage discounts.
- **Multiple Payment Modes**: Cash, Credit/Debit Card, UPI / QR Code, EMI / Finance, Net Banking.
- **Immediate Invoice Preview**: Opens A4 invoice preview modal with options to Print Thermal POS or open/save as PDF in the default browser.

### 2. Inventory & Stock Tab
- View all catalog items with real-time stock levels and low-stock indicators.
- **Add Product**: Modal form to add items with warranty terms and serial requirement flags.
- **Edit Product**: Update prices, cost prices, tax percentages, or specifications.
- **Restock**: One-click restock tool to replenish stock quantities.
- **Delete Product**: Safe removal with confirmation.

### 3. Sales Invoices Tab
- Chronological archive of all past sales with date, time, customer details, payment method, and grand totals.
- Filter past invoices by Invoice ID, customer phone number, or date.
- Lower preview pane displays line items and assigned serial numbers for the selected transaction.
- Actions to **View / Reprint Bill** or **Open in Browser/PDF**.

### 4. Warranty & Serials Tab
- Instant post-sale support tool for customer warranty verification.
- Search by Serial Number, IMEI, or Customer Phone.
- Visual warranty card displays:
  - Active status with exact remaining days (e.g. `ACTIVE (312 days remaining)`) or `EXPIRED`.
  - Original purchase invoice, date, customer name, and contact details.

### 5. Analytics Dashboard Tab
- Real-time KPI summary:
  - **Total Revenue**
  - **Invoices Billed**
  - **Units Sold**
  - **Low Stock Items**
- Breakdown tables for **Sales by Category** and **Top Selling Electronics**.

### 6. Shop Settings Tab
- Customize Store Name, Tagline, Address, Phone, Email, GSTIN/Tax ID, Currency Symbol (₹, $, €, etc.), Invoice Footer Message, and Terms & Conditions.
- Changes reflect immediately across all tabs and generated invoices.

---

## Directory Layout

```
Electronic Billing System/
├── src/
│   └── com/
│       └── electro/
│           ├── Main.java                        # App launcher & native look-and-feel
│           ├── model/
│           │   ├── Product.java                 # Product data model
│           │   ├── CartItem.java                # Line item with serial numbers
│           │   ├── Customer.java                # Customer info
│           │   ├── Invoice.java                 # Sales invoice with tax breakdown
│           │   ├── WarrantyRecord.java          # Serial/IMEI warranty record
│           │   └── ShopSettings.java            # Store preferences & GSTIN
│           ├── service/
│           │   ├── SimpleJson.java              # Zero-dependency JSON parser/serializer
│           │   ├── DataStore.java               # File storage & sample catalog seeder
│           │   ├── BillingService.java          # POS cart lifecycle & checkout logic
│           │   ├── InventoryService.java        # Catalog CRUD & stock alerts
│           │   ├── WarrantyService.java         # Warranty registry & query engine
│           │   └── InvoiceGenerator.java        # HTML A4 & thermal receipt generator
│           └── ui/
│               ├── UITheme.java                 # Modern color scheme, typography, button styles
│               ├── MainFrame.java               # Main window & tab coordinator
│               ├── BillingPanel.java            # Split POS checkout panel
│               ├── InventoryPanel.java          # Product inventory manager
│               ├── InvoiceHistoryPanel.java     # Sales history & reprint viewer
│               ├── WarrantyPanel.java           # Warranty claim status & serial lookup
│               ├── AnalyticsPanel.java          # Business dashboard & sales charts
│               ├── SettingsPanel.java           # Store details & invoice header configuration
│               ├── SerialInputDialog.java       # Serial/IMEI entry & validation dialog
│               └── InvoicePreviewDialog.java     # Interactive receipt preview & print window
├── bin/                                         # Compiled bytecode (.class files)
├── data/                                        # Persistent JSON files (auto-generated)
│   ├── products.json
│   ├── invoices.json
│   ├── customers.json
│   ├── warranties.json
│   └── settings.json
├── invoices/                                    # Generated HTML tax invoices
├── compile.bat                                  # Batch build script
├── run.bat                                      # Batch launch script
└── README.md
```
