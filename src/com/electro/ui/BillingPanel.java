package com.electro.ui;

import com.electro.model.*;
import com.electro.service.BillingService;
import com.electro.service.DataStore;
import com.electro.service.InventoryService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Point of Sale (POS) panel for checkout and billing.
 */
public class BillingPanel extends JPanel {
    private final BillingService billingService;
    private final InventoryService inventoryService;
    private final DataStore dataStore;
    private final Window parentWindow;

    // Left Panel: Products
    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JTable productTable;
    private DefaultTableModel productTableModel;
    private List<Product> displayedProducts = new ArrayList<>();

    // Right Panel: Customer & Cart
    private JRadioButton rbRetail;
    private JRadioButton rbWholesale;
    private JLabel lblClassificationBadge;

    private JTextField custNameField;
    private JTextField custPhoneField;
    private JTextField custEmailField;
    private JTextField custAddressField;

    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private JComboBox<String> paymentMethodCombo;
    private JTextField paymentRefField;
    private JTextField overallDiscountField;
    private JButton btnConfigSplit;
    private String splitPaymentDetails = "";

    // Summary labels
    private JLabel lblSubtotal;
    private JLabel lblDiscount;
    private JLabel lblTaxable;
    private JLabel lblCgst;
    private JLabel lblSgst;
    private JLabel lblGrandTotal;

    public BillingPanel(Window parentWindow, BillingService billingService, InventoryService inventoryService) {
        this.parentWindow = parentWindow;
        this.billingService = billingService;
        this.inventoryService = inventoryService;
        this.dataStore = DataStore.getInstance();

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.COLOR_BG);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createProductBrowserPanel(), createCheckoutCartPanel());
        splitPane.setResizeWeight(0.52);
        splitPane.setBorder(null);
        splitPane.setDividerSize(6);

        add(splitPane, BorderLayout.CENTER);
        refreshProductList();
        updateCartTable();
    }

    // --- LEFT PANEL: PRODUCT BROWSER ---
    private JPanel createProductBrowserPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Header / Search Bar
        JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setBackground(UITheme.COLOR_PANEL_BG);

        JLabel title = new JLabel("Catalogs");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY_DARK);

        JPanel filters = new JPanel(new BorderLayout(6, 6));
        filters.setBackground(UITheme.COLOR_PANEL_BG);

        searchField = UITheme.createTextField(14);
        searchField.putClientProperty("JTextField.placeholderText", "Search name, brand, model, SKU...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refreshProductList();
            }
        });

        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UITheme.FONT_REGULAR);
        categoryCombo.addItem("All Categories");
        for (String cat : inventoryService.getAllCategories()) {
            categoryCombo.addItem(cat);
        }
        categoryCombo.addActionListener(e -> refreshProductList());

        filters.add(searchField, BorderLayout.CENTER);
        filters.add(categoryCombo, BorderLayout.EAST);

        topBar.add(title, BorderLayout.NORTH);
        topBar.add(filters, BorderLayout.SOUTH);
        panel.add(topBar, BorderLayout.NORTH);

        // Product Table
        String[] cols = {"SKU", "Item Description", "Category", "Price", "Stock", "Warranty"};
        productTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        productTable = new JTable(productTableModel);
        UITheme.styleTable(productTable);
        productTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(210);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        productTable.getColumnModel().getColumn(5).setPreferredWidth(75);

        productTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && productTable.getSelectedRow() >= 0) {
                    addProductFromTable(productTable.getSelectedRow());
                }
            }
        });

        panel.add(new JScrollPane(productTable), BorderLayout.CENTER);

        // Bottom Add Button
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        bottomBar.setBackground(UITheme.COLOR_PANEL_BG);

        JButton btnAdd = UITheme.createButton("+ Add Selected to Bill", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> {
            int row = productTable.getSelectedRow();
            if (row >= 0) {
                addProductFromTable(row);
            } else {
                JOptionPane.showMessageDialog(this, "Please select an item from the table first.", "Selection Needed", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        bottomBar.add(btnAdd);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    public void refreshProductList() {
        String query = searchField.getText();
        String cat = (String) categoryCombo.getSelectedItem();
        if ("All Categories".equals(cat)) cat = null;

        displayedProducts = inventoryService.searchProducts(query, cat);
        productTableModel.setRowCount(0);
        String sym = dataStore.getSettings().getCurrencySymbol();

        boolean isWholesale = (billingService.getCustomerClassification() == BillingService.CustomerClassification.WHOLESALE);
        if (productTable != null && productTable.getColumnModel().getColumnCount() > 3) {
            productTable.getColumnModel().getColumn(3).setHeaderValue(isWholesale ? "Rate (Wholesale)" : "Price (Retail)");
            productTable.getTableHeader().repaint();
        }

        for (Product p : displayedProducts) {
            String stockStr = p.getStockQuantity() <= 0 ? "OUT" : String.valueOf(p.getStockQuantity());
            if (p.isLowStock() && p.getStockQuantity() > 0) {
                stockStr += " (LOW)";
            }
            double priceToDisplay = isWholesale ? p.getWholesalePrice() : p.getSellingPrice();
            productTableModel.addRow(new Object[]{
                    p.getSku(),
                    p.getBrand() + " " + p.getName(),
                    p.getCategory(),
                    UITheme.formatCurrency(priceToDisplay, sym),
                    stockStr,
                    p.getWarrantyMonths() + " Mos"
            });
        }
    }

    private void addProductFromTable(int row) {
        if (row < 0 || row >= displayedProducts.size()) return;
        Product p = displayedProducts.get(row);

        if (p.getStockQuantity() <= 0) {
            JOptionPane.showMessageDialog(this, p.getName() + " is currently Out of Stock!", "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Add 1 to cart
        billingService.addToCart(p, 1);

        // If requires serial number, open Serial Dialog immediately
        if (p.isRequiresSerial()) {
            promptSerialEntry(p);
        }

        updateCartTable();
    }

    private void promptSerialEntry(Product p) {
        // Find cart item
        for (CartItem ci : billingService.getCart()) {
            if (ci.getProduct().getId().equals(p.getId())) {
                SerialInputDialog dlg = new SerialInputDialog(parentWindow, p, ci.getQuantity(), ci.getSerialNumbers());
                dlg.setVisible(true);
                if (dlg.getConfirmedSerials() != null) {
                    billingService.setItemSerials(p.getId(), dlg.getConfirmedSerials());
                }
                break;
            }
        }
    }

    // --- RIGHT PANEL: CART & CHECKOUT ---
    private JPanel createCheckoutCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Customer Details & Classification Section
        JPanel custSection = new JPanel(new BorderLayout(6, 8));
        custSection.setBackground(new Color(248, 250, 252));
        custSection.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(8, 12, 10, 12)
        ));

        // Row 1: Classification Selector (Retail vs Wholesale) & Status Badge
        JPanel classBar = new JPanel(new BorderLayout(8, 4));
        classBar.setOpaque(false);

        JPanel classLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        classLeft.setOpaque(false);

        JLabel lblClassPrompt = new JLabel("Pricing Tier / User Classification:");
        lblClassPrompt.setFont(UITheme.FONT_REGULAR_BOLD);
        lblClassPrompt.setForeground(UITheme.COLOR_PRIMARY_DARK);

        rbRetail = new JRadioButton("Retail (Standard MRP)", true);
        rbRetail.setFont(UITheme.FONT_REGULAR_BOLD);
        rbRetail.setOpaque(false);
        rbRetail.setCursor(new Cursor(Cursor.HAND_CURSOR));

        rbWholesale = new JRadioButton("Wholesale (B2B Bulk Rate)", false);
        rbWholesale.setFont(UITheme.FONT_REGULAR_BOLD);
        rbWholesale.setOpaque(false);
        rbWholesale.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ButtonGroup bgClass = new ButtonGroup();
        bgClass.add(rbRetail);
        bgClass.add(rbWholesale);

        rbRetail.addActionListener(e -> setClassification(BillingService.CustomerClassification.RETAIL));
        rbWholesale.addActionListener(e -> setClassification(BillingService.CustomerClassification.WHOLESALE));

        classLeft.add(lblClassPrompt);
        classLeft.add(rbRetail);
        classLeft.add(rbWholesale);

        lblClassificationBadge = new JLabel(" [ RETAIL PRICING ACTIVE ] ");
        lblClassificationBadge.setFont(UITheme.FONT_SMALL);
        lblClassificationBadge.setOpaque(true);
        lblClassificationBadge.setBackground(new Color(220, 252, 231));
        lblClassificationBadge.setForeground(new Color(22, 101, 52));
        lblClassificationBadge.setBorder(new CompoundBorder(
                new LineBorder(new Color(187, 247, 208), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));

        classBar.add(classLeft, BorderLayout.CENTER);
        classBar.add(lblClassificationBadge, BorderLayout.EAST);

        custSection.add(classBar, BorderLayout.NORTH);

        // Row 2: Explicitly labeled fields showing what data to enter
        JPanel fieldsGrid = new JPanel(new GridLayout(2, 2, 10, 6));
        fieldsGrid.setOpaque(false);

        custPhoneField = UITheme.createTextField(10);
        custPhoneField.setToolTipText("Enter customer 10-digit mobile number. Auto-fills existing customer profile.");
        custPhoneField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                autoFillCustomer(custPhoneField.getText().trim());
            }
        });

        custNameField = UITheme.createTextField(12);
        custNameField.setToolTipText("Enter individual customer full name or business / firm entity name.");

        custEmailField = UITheme.createTextField(12);
        custEmailField.setToolTipText("Enter customer email address for sending digital invoice & warranty (optional).");

        custAddressField = UITheme.createTextField(14);
        custAddressField.setToolTipText("Enter customer address, city, state and pincode for billing & delivery.");

        fieldsGrid.add(createFieldGroup("Phone Number (10 Digits - Auto Search):", custPhoneField));
        fieldsGrid.add(createFieldGroup("Customer / Company Name:", custNameField));
        fieldsGrid.add(createFieldGroup("Email Address (Optional):", custEmailField));
        fieldsGrid.add(createFieldGroup("Billing Address (City, Pincode):", custAddressField));

        custSection.add(fieldsGrid, BorderLayout.CENTER);

        panel.add(custSection, BorderLayout.NORTH);

        // Cart Table
        String[] cartCols = {"Item", "S/N or IMEI", "Qty", "Rate", "Disc%", "Total"};
        cartTableModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        cartTable = new JTable(cartTableModel);
        UITheme.styleTable(cartTable);
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(45);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(75);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(50);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(85);

        // Table toolbar actions (+, -, serials, delete)
        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        cartActions.setBackground(UITheme.COLOR_PANEL_BG);

        JButton btnPlus = UITheme.createButton("+ Qty", UITheme.COLOR_BG, UITheme.COLOR_TEXT_PRIMARY);
        btnPlus.setBorder(new LineBorder(UITheme.COLOR_BORDER, 1));
        btnPlus.addActionListener(e -> modifyCartQty(1));

        JButton btnMinus = UITheme.createButton("- Qty", UITheme.COLOR_BG, UITheme.COLOR_TEXT_PRIMARY);
        btnMinus.setBorder(new LineBorder(UITheme.COLOR_BORDER, 1));
        btnMinus.addActionListener(e -> modifyCartQty(-1));

        JButton btnSerials = UITheme.createButton("Assign S/N", new Color(2, 132, 199), Color.WHITE);
        btnSerials.addActionListener(e -> editSelectedSerials());

        JButton btnRemove = UITheme.createButton("Remove", UITheme.COLOR_DANGER, Color.WHITE);
        btnRemove.addActionListener(e -> removeSelectedCartItem());

        JButton btnClear = UITheme.createButton("Clear Cart", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnClear.addActionListener(e -> {
            billingService.clearCart();
            updateCartTable();
        });

        cartActions.add(btnPlus);
        cartActions.add(btnMinus);
        cartActions.add(btnSerials);
        cartActions.add(btnRemove);
        cartActions.add(btnClear);

        JPanel cartCenter = new JPanel(new BorderLayout(4, 4));
        cartCenter.setBackground(UITheme.COLOR_PANEL_BG);
        cartCenter.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        cartCenter.add(cartActions, BorderLayout.SOUTH);

        panel.add(cartCenter, BorderLayout.CENTER);

        // Bottom: Summary & Checkout
        panel.add(createCheckoutSummary(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCheckoutSummary() {
        JPanel summaryPanel = new JPanel(new BorderLayout(8, 8));
        summaryPanel.setBackground(new Color(248, 250, 252));
        summaryPanel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Payment Mode & Overall Discount
        JPanel paymentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        paymentRow.setBackground(new Color(248, 250, 252));

        paymentMethodCombo = new JComboBox<>(new String[]{"Cash", "Credit/Debit Card", "UPI / QR Code", "EMI / Finance", "Net Banking", "Split Payment (Multi-Mode)"});
        paymentMethodCombo.setFont(UITheme.FONT_REGULAR);

        btnConfigSplit = UITheme.createButton("Configure Split", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnConfigSplit.setFont(UITheme.FONT_SMALL);
        btnConfigSplit.setPreferredSize(new Dimension(110, 28));
        btnConfigSplit.setVisible(false);
        btnConfigSplit.addActionListener(e -> openSplitPaymentDialog());

        paymentMethodCombo.addActionListener(e -> {
            String selected = (String) paymentMethodCombo.getSelectedItem();
            if ("Split Payment (Multi-Mode)".equals(selected)) {
                btnConfigSplit.setVisible(true);
                if (billingService.getCart().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please add items to your cart before configuring split payments.", "Empty Cart", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    openSplitPaymentDialog();
                }
            } else {
                btnConfigSplit.setVisible(false);
                splitPaymentDetails = "";
                if (paymentRefField.getText().startsWith("Cash:") || paymentRefField.getText().startsWith("Split:")) {
                    paymentRefField.setText("");
                }
            }
            revalidate();
            repaint();
        });

        paymentRefField = UITheme.createTextField(10);
        paymentRefField.putClientProperty("JTextField.placeholderText", "Txn / UPI Ref ID");

        JLabel lblDisc = new JLabel("Bill Discount %:");
        lblDisc.setFont(UITheme.FONT_REGULAR_BOLD);
        overallDiscountField = UITheme.createTextField(4);
        overallDiscountField.setText("0");
        overallDiscountField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    double val = Double.parseDouble(overallDiscountField.getText().trim());
                    billingService.setOverallDiscountPercent(val);
                } catch (Exception ex) {
                    billingService.setOverallDiscountPercent(0);
                }
                updateSummaryLabels();
            }
        });

        paymentRow.add(new JLabel("Payment:"));
        paymentRow.add(paymentMethodCombo);
        paymentRow.add(btnConfigSplit);
        paymentRow.add(paymentRefField);
        paymentRow.add(lblDisc);
        paymentRow.add(overallDiscountField);

        // Numeric Breakdown
        JPanel figuresPanel = new JPanel(new GridLayout(3, 2, 10, 4));
        figuresPanel.setBackground(new Color(248, 250, 252));
        figuresPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        lblSubtotal = new JLabel("Subtotal: \u20B90.00");
        lblDiscount = new JLabel("Discount: \u20B90.00");
        lblDiscount.setForeground(UITheme.COLOR_DANGER);
        lblTaxable = new JLabel("Taxable: \u20B90.00");
        lblCgst = new JLabel("CGST: \u20B90.00");
        lblSgst = new JLabel("SGST: \u20B90.00");
        lblGrandTotal = new JLabel("GRAND TOTAL: \u20B90.00");
        lblGrandTotal.setFont(UITheme.FONT_SUBTITLE);
        lblGrandTotal.setForeground(UITheme.COLOR_PRIMARY_DARK);

        figuresPanel.add(lblSubtotal);
        figuresPanel.add(lblTaxable);
        figuresPanel.add(lblDiscount);
        figuresPanel.add(lblCgst);
        figuresPanel.add(lblSgst);
        figuresPanel.add(lblGrandTotal);

        // Checkout Button
        JButton btnCheckout = UITheme.createButton("PROCEED TO BILL & PRINT >", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnCheckout.setFont(UITheme.FONT_SUBTITLE);
        btnCheckout.setPreferredSize(new Dimension(0, 46));
        btnCheckout.addActionListener(e -> executeCheckout());

        summaryPanel.add(paymentRow, BorderLayout.NORTH);
        summaryPanel.add(figuresPanel, BorderLayout.CENTER);
        summaryPanel.add(btnCheckout, BorderLayout.SOUTH);

        return summaryPanel;
    }

    private void autoFillCustomer(String phone) {
        if (phone.length() >= 10) {
            Customer existing = dataStore.getCustomerByPhone(phone);
            if (existing != null) {
                if (custNameField.getText().trim().isEmpty()) custNameField.setText(existing.getName());
                if (custEmailField.getText().trim().isEmpty()) custEmailField.setText(existing.getEmail());
                if (custAddressField.getText().trim().isEmpty()) custAddressField.setText(existing.getAddress());
                if (existing.isWholesale()) {
                    setClassification(BillingService.CustomerClassification.WHOLESALE);
                } else {
                    setClassification(BillingService.CustomerClassification.RETAIL);
                }
            }
        }
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 3));
        group.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_REGULAR_BOLD);
        lbl.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        group.add(lbl, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private void setClassification(BillingService.CustomerClassification classification) {
        billingService.setCustomerClassification(classification);
        updateClassificationUI();
        updateCartTable();
        refreshProductList();
    }

    private void updateClassificationUI() {
        boolean isWholesale = (billingService.getCustomerClassification() == BillingService.CustomerClassification.WHOLESALE);
        if (isWholesale) {
            if (rbWholesale != null) rbWholesale.setSelected(true);
            if (lblClassificationBadge != null) {
                lblClassificationBadge.setText(" [ WHOLESALE B2B RATE ACTIVE (~8% OFF) ] ");
                lblClassificationBadge.setBackground(new Color(238, 242, 255));
                lblClassificationBadge.setForeground(new Color(79, 70, 229));
                lblClassificationBadge.setBorder(new CompoundBorder(
                        new LineBorder(new Color(199, 210, 254), 1, true),
                        new EmptyBorder(3, 8, 3, 8)
                ));
            }
            if (productTable != null && productTable.getColumnModel().getColumnCount() > 3) {
                productTable.getColumnModel().getColumn(3).setHeaderValue("Rate (Wholesale)");
                productTable.getTableHeader().repaint();
            }
        } else {
            if (rbRetail != null) rbRetail.setSelected(true);
            if (lblClassificationBadge != null) {
                lblClassificationBadge.setText(" [ RETAIL PRICING ACTIVE ] ");
                lblClassificationBadge.setBackground(new Color(220, 252, 231));
                lblClassificationBadge.setForeground(new Color(22, 101, 52));
                lblClassificationBadge.setBorder(new CompoundBorder(
                        new LineBorder(new Color(187, 247, 208), 1, true),
                        new EmptyBorder(3, 8, 3, 8)
                ));
            }
            if (productTable != null && productTable.getColumnModel().getColumnCount() > 3) {
                productTable.getColumnModel().getColumn(3).setHeaderValue("Price (Retail)");
                productTable.getTableHeader().repaint();
            }
        }
    }

    private void modifyCartQty(int delta) {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= billingService.getCart().size()) return;
        CartItem item = billingService.getCart().get(row);
        int target = item.getQuantity() + delta;

        if (target <= 0) {
            billingService.removeItem(item.getProduct().getId());
        } else if (target > item.getProduct().getStockQuantity()) {
            JOptionPane.showMessageDialog(this, "Cannot exceed available stock (" + item.getProduct().getStockQuantity() + ")", "Stock Limit", JOptionPane.WARNING_MESSAGE);
            return;
        } else {
            billingService.updateQuantity(item.getProduct().getId(), target);
            if (item.getProduct().isRequiresSerial() && target > item.getSerialNumbers().size()) {
                promptSerialEntry(item.getProduct());
            }
        }
        updateCartTable();
    }

    private void editSelectedSerials() {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= billingService.getCart().size()) return;
        CartItem item = billingService.getCart().get(row);
        promptSerialEntry(item.getProduct());
        updateCartTable();
    }

    private void removeSelectedCartItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= billingService.getCart().size()) return;
        CartItem item = billingService.getCart().get(row);
        billingService.removeItem(item.getProduct().getId());
        updateCartTable();
    }

    public void updateCartTable() {
        cartTableModel.setRowCount(0);
        String sym = dataStore.getSettings().getCurrencySymbol();

        for (CartItem ci : billingService.getCart()) {
            Product p = ci.getProduct();
            String serials = ci.getSerialNumbers().isEmpty() ? 
                    (p.isRequiresSerial() ? "(! Missing S/N)" : "-") : 
                    String.join(", ", ci.getSerialNumbers());

            cartTableModel.addRow(new Object[]{
                    p.getBrand() + " " + p.getName(),
                    serials,
                    ci.getQuantity(),
                    UITheme.formatCurrency(ci.getUnitPrice(), sym),
                    ci.getDiscountPercent() > 0 ? (ci.getDiscountPercent() + "%") : "-",
                    UITheme.formatCurrency(ci.getLineTotal(), sym)
            });
        }

        updateSummaryLabels();
    }

    private void updateSummaryLabels() {
        String sym = dataStore.getSettings().getCurrencySymbol();
        lblSubtotal.setText("Subtotal: " + UITheme.formatCurrency(billingService.calculateSubtotal(), sym));
        lblDiscount.setText("Discount: -" + UITheme.formatCurrency(billingService.calculateTotalDiscounts(), sym));
        lblTaxable.setText("Taxable: " + UITheme.formatCurrency(billingService.calculateTaxableAmount(), sym));
        lblCgst.setText("CGST: " + UITheme.formatCurrency(billingService.calculateCgst(), sym));
        lblSgst.setText("SGST: " + UITheme.formatCurrency(billingService.calculateSgst(), sym));
        lblGrandTotal.setText("TOTAL: " + UITheme.formatCurrency(billingService.calculateGrandTotal(), sym));
    }

    private void openSplitPaymentDialog() {
        double grandTotal = billingService.calculateGrandTotal();
        if (grandTotal <= 0) {
            JOptionPane.showMessageDialog(this, "Cart is currently empty. Please add items before splitting payment.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        SplitPaymentDialog dlg = new SplitPaymentDialog(parentWindow, grandTotal, dataStore.getSettings().getCurrencySymbol());
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            splitPaymentDetails = dlg.getSplitSummary();
            paymentRefField.setText(splitPaymentDetails);
        }
    }

    private void executeCheckout() {
        String err = billingService.validateCartForCheckout();
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Checkout Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String paymentMethod = (String) paymentMethodCombo.getSelectedItem();
        if ("Split Payment (Multi-Mode)".equals(paymentMethod)) {
            if (splitPaymentDetails == null || splitPaymentDetails.trim().isEmpty()) {
                openSplitPaymentDialog();
                if (splitPaymentDetails == null || splitPaymentDetails.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please configure the split payment breakdown before proceeding.", "Payment Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        String name = custNameField.getText().trim();
        String phone = custPhoneField.getText().trim();
        String email = custEmailField.getText().trim();
        String address = custAddressField.getText().trim();

        if (name.isEmpty()) name = "Walk-in Customer";

        String classification = (rbWholesale != null && rbWholesale.isSelected()) ? "WHOLESALE" : "RETAIL";
        Customer customer = new Customer(
                phone.isEmpty() ? "GUEST" : "CUST-" + phone,
                name,
                phone,
                email,
                address,
                "",
                classification
        );

        String paymentRef = paymentRefField.getText().trim();

        try {
            Invoice invoice = billingService.checkout(customer, paymentMethod, paymentRef, "", splitPaymentDetails);

            // Refresh UI
            setClassification(BillingService.CustomerClassification.RETAIL);
            splitPaymentDetails = "";
            btnConfigSplit.setVisible(false);
            paymentMethodCombo.setSelectedIndex(0);
            updateCartTable();
            refreshProductList();
            custPhoneField.setText("");
            custNameField.setText("");
            custEmailField.setText("");
            custAddressField.setText("");
            paymentRefField.setText("");
            overallDiscountField.setText("0");

            // Show Invoice Preview Dialog
            InvoicePreviewDialog previewDialog = new InvoicePreviewDialog(parentWindow, invoice);
            previewDialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Checkout failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
