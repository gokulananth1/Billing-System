package com.electro.ui;

import com.electro.model.*;
import com.electro.service.AuthService;
import com.electro.service.BillingService;
import com.electro.service.DataStore;
import com.electro.service.InventoryService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Clean POS Workstation Interface for Electronic Billing.
 */
public class BillingPanel extends JPanel {
    public static final String[] BILLING_CATALOG_COLUMNS = {
            "SKU", "Item Description", "Category", "Price", "Stock", "Warranty"
    };

    private final BillingService billingService;
    private final InventoryService inventoryService;
    private final DataStore dataStore;
    private final Window parentWindow;

    // Barcode Rapid Scan Input & Product Search Dropdown
    private JTextField barcodeScanField;
    private JWindow productDropdownWindow;
    private JList<Product> productSearchList;
    private DefaultListModel<Product> productSearchListModel;
    private JScrollPane productSearchScroll;

    // Customer & Pricing Controls
    private JRadioButton rbRetail;
    private JRadioButton rbWholesale;
    private JLabel lblClassificationBadge;

    private JTextField custNameField;
    private JTextField custPhoneField;
    private JTextField custEmailField;
    private JTextField custAddressField;

    // Cart Table
    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private boolean isUpdatingTable = false;

    // Quick Touch Items Container
    private JPanel quickItemsPanel;

    // Right Side: Checkout & Figures
    private JLabel lblHeroTotal;
    private JLabel lblReceiptSubtotal;
    private JLabel lblReceiptDiscount;
    private JLabel lblReceiptTaxable;
    private JLabel lblReceiptGst;

    // Payment Selection & Settlement
    private JComboBox<String> paymentMethodCombo;
    private JTextField tenderedAmountField;
    private JLabel lblChangeDue;
    private JTextField overallDiscountField;
    private JButton btnConfigSplit;
    private String splitPaymentDetails = "";

    // Legacy / Compatibility references
    private JComboBox<String> categoryCombo;
    private boolean isUpdatingCategories = false;

    public BillingPanel(Window parentWindow, BillingService billingService, InventoryService inventoryService) {
        this.parentWindow = parentWindow;
        this.billingService = billingService;
        this.inventoryService = inventoryService;
        this.dataStore = DataStore.getInstance();

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.COLOR_BG);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        // Category combo for background synchronization compatibility
        categoryCombo = new JComboBox<>();
        categoryCombo.addItem("All Categories");
        for (String cat : inventoryService.getAllCategories()) {
            categoryCombo.addItem(cat);
        }

        initBillingUi();
        updateCartTable();
    }

    private void initBillingUi() {
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftWorkPanel(), createRightCheckoutPanel());
        mainSplit.setResizeWeight(0.70);
        mainSplit.setDividerSize(6);
        mainSplit.setBorder(null);
        mainSplit.setOpaque(false);
        add(mainSplit, BorderLayout.CENTER);
    }

    // --- LEFT WORKSPACE (70%): Customer, Barcode Scanner, Cart Table ---
    private JPanel createLeftWorkPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setOpaque(false);

        // Top Area: Customer Details Card & Barcode Scanner Bar
        JPanel topBox = new JPanel(new BorderLayout(8, 8));
        topBox.setOpaque(false);
        topBox.add(createCustomerBanner(), BorderLayout.NORTH);
        topBox.add(createBarcodeScanBanner(), BorderLayout.SOUTH);

        leftPanel.add(topBox, BorderLayout.NORTH);

        // Center Area: Cart Table with editable 1st column & editable Qty
        leftPanel.add(createCartTablePanel(), BorderLayout.CENTER);

        return leftPanel;
    }

    private JPanel createCustomerBanner() {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(UITheme.COLOR_PANEL_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        // Row 1: Header + Pricing Tier Selector & Classification Badge
        JPanel tierRow = new JPanel(new BorderLayout(8, 4));
        tierRow.setOpaque(false);

        JPanel tierLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tierLeft.setOpaque(false);

        JLabel lblCustTitle = new JLabel("Customer Information & Pricing Tier");
        lblCustTitle.setFont(UITheme.FONT_SUBTITLE);
        lblCustTitle.setForeground(UITheme.COLOR_PRIMARY_DARK);

        JLabel lblTier = new JLabel("Tier:");
        lblTier.setFont(UITheme.FONT_REGULAR_BOLD);
        lblTier.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        rbRetail = new JRadioButton("Retail (MRP)", true);
        rbRetail.setFont(UITheme.FONT_REGULAR_BOLD);
        rbRetail.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        rbRetail.setOpaque(false);
        rbRetail.setCursor(new Cursor(Cursor.HAND_CURSOR));

        rbWholesale = new JRadioButton("Wholesale (B2B Bulk Rate)", false);
        rbWholesale.setFont(UITheme.FONT_REGULAR_BOLD);
        rbWholesale.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        rbWholesale.setOpaque(false);
        rbWholesale.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbRetail);
        bg.add(rbWholesale);

        rbRetail.addActionListener(e -> setClassification(BillingService.CustomerClassification.RETAIL));
        rbWholesale.addActionListener(e -> setClassification(BillingService.CustomerClassification.WHOLESALE));

        tierLeft.add(lblCustTitle);
        tierLeft.add(Box.createHorizontalStrut(10));
        tierLeft.add(lblTier);
        tierLeft.add(rbRetail);
        tierLeft.add(rbWholesale);

        lblClassificationBadge = new JLabel(" [ RETAIL ACTIVE ] ");
        lblClassificationBadge.setFont(UITheme.FONT_SMALL_BOLD);
        lblClassificationBadge.setOpaque(true);
        lblClassificationBadge.setBackground(new Color(240, 253, 244));
        lblClassificationBadge.setForeground(UITheme.COLOR_SUCCESS);
        lblClassificationBadge.setBorder(new CompoundBorder(
                new LineBorder(new Color(187, 247, 208), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));

        tierRow.add(tierLeft, BorderLayout.CENTER);
        tierRow.add(lblClassificationBadge, BorderLayout.EAST);
        card.add(tierRow, BorderLayout.NORTH);

        // Row 2: Customer Fields Grid
        JPanel grid = new JPanel(new GridLayout(1, 4, 10, 4));
        grid.setOpaque(false);

        custPhoneField = UITheme.createTextField(10);
        custPhoneField.setToolTipText("Enter customer 10-digit mobile number for instant lookup.");
        custPhoneField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                autoFillCustomer(custPhoneField.getText().trim());
            }
        });

        custNameField = UITheme.createTextField(12);
        custEmailField = UITheme.createTextField(12);
        custAddressField = UITheme.createTextField(14);

        grid.add(createFieldGroup("Mobile No. (Auto Lookup):", custPhoneField));
        grid.add(createFieldGroup("Customer / Company:", custNameField));
        grid.add(createFieldGroup("Email Address:", custEmailField));
        grid.add(createFieldGroup("Billing Address:", custAddressField));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBarcodeScanBanner() {
        JPanel scanBar = new JPanel(new BorderLayout(8, 4));
        scanBar.setBackground(UITheme.COLOR_PANEL_BG);
        scanBar.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblScan = new JLabel("Barcode / Item Scanner: ");
        lblScan.setFont(UITheme.FONT_REGULAR_BOLD);
        lblScan.setForeground(UITheme.COLOR_PRIMARY_DARK);

        barcodeScanField = UITheme.createTextField(25);
        barcodeScanField.setFont(UITheme.FONT_REGULAR_BOLD);
        barcodeScanField.putClientProperty("JTextField.placeholderText", "Click here or scan barcode / enter product name...");

        initProductSearchPopup();

        barcodeScanField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showProductSearchPopup();
            }
        });

        barcodeScanField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                showProductSearchPopup();
            }
            @Override
            public void focusLost(FocusEvent e) {
                scheduleHideDropdown();
            }
        });

        barcodeScanField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_DOWN) {
                    if (productDropdownWindow != null && productDropdownWindow.isVisible() && productSearchListModel.getSize() > 0) {
                        int idx = productSearchList.getSelectedIndex();
                        if (idx < productSearchListModel.getSize() - 1) {
                            productSearchList.setSelectedIndex(idx + 1);
                            productSearchList.ensureIndexIsVisible(idx + 1);
                        }
                    } else {
                        showProductSearchPopup();
                    }
                } else if (code == KeyEvent.VK_UP) {
                    if (productDropdownWindow != null && productDropdownWindow.isVisible() && productSearchListModel.getSize() > 0) {
                        int idx = productSearchList.getSelectedIndex();
                        if (idx > 0) {
                            productSearchList.setSelectedIndex(idx - 1);
                            productSearchList.ensureIndexIsVisible(idx - 1);
                        }
                    }
                } else if (code == KeyEvent.VK_ESCAPE) {
                    hideProductDropdown();
                } else if (code != KeyEvent.VK_ENTER) {
                    showProductSearchPopup();
                }
            }
        });

        barcodeScanField.addActionListener(e -> {
            String code = barcodeScanField.getText().trim();
            if (productDropdownWindow != null && productDropdownWindow.isVisible() && productSearchList.getSelectedValue() != null && !code.isEmpty()) {
                Product exact = findProductByBarcode(code);
                if (exact != null) {
                    hideProductDropdown();
                    barcodeScanField.setText("");
                    handleBarcodeScan(exact.getSku());
                } else {
                    Product sel = productSearchList.getSelectedValue();
                    selectProductFromPopup(sel);
                }
            } else if (!code.isEmpty()) {
                hideProductDropdown();
                handleBarcodeScan(code);
                barcodeScanField.setText("");
            } else if (productDropdownWindow != null && productDropdownWindow.isVisible() && productSearchList.getSelectedValue() != null) {
                Product sel = productSearchList.getSelectedValue();
                selectProductFromPopup(sel);
            }
        });

        JButton btnBrowse = UITheme.createButton("Products \u25BC", UITheme.COLOR_SECONDARY, Color.WHITE);
        btnBrowse.setFont(UITheme.FONT_SMALL_BOLD);
        btnBrowse.setToolTipText("Toggle product items catalog dropdown");
        btnBrowse.addActionListener(e -> {
            barcodeScanField.requestFocusInWindow();
            toggleProductDropdown();
        });

        JButton btnScanAdd = UITheme.createButton("+ Add Item", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnScanAdd.addActionListener(e -> {
            String code = barcodeScanField.getText().trim();
            if (productDropdownWindow != null && productDropdownWindow.isVisible() && productSearchList.getSelectedValue() != null && code.isEmpty()) {
                Product sel = productSearchList.getSelectedValue();
                selectProductFromPopup(sel);
            } else if (!code.isEmpty()) {
                hideProductDropdown();
                handleBarcodeScan(code);
                barcodeScanField.setText("");
            } else {
                showProductSearchPopup();
            }
        });

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(btnBrowse);
        rightBtns.add(btnScanAdd);

        JPanel inputWrap = new JPanel(new BorderLayout(8, 0));
        inputWrap.setOpaque(false);
        inputWrap.add(barcodeScanField, BorderLayout.CENTER);
        inputWrap.add(rightBtns, BorderLayout.EAST);

        JLabel lblHint = new JLabel("Fast POS Entry: Click scanner or Products \u25BC to browse, or scan barcode with gun.");
        lblHint.setFont(UITheme.FONT_SMALL);
        lblHint.setForeground(UITheme.COLOR_TEXT_MUTED);

        scanBar.add(lblScan, BorderLayout.WEST);
        scanBar.add(inputWrap, BorderLayout.CENTER);
        scanBar.add(lblHint, BorderLayout.SOUTH);

        return scanBar;
    }

    private JPanel createCartTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));

        String[] cartCols = {
                dataStore.getSettings().getColumnDisplayName("SKU"),
                dataStore.getSettings().getColumnDisplayName("Product Name"),
                "Brand / Model",
                "S/N or IMEI",
                "Qty",
                "Rate",
                "Disc%",
                "Total"
        };

        cartTableModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return (col == 0 || col == 4);
            }
        };

        cartTable = new JTable(cartTableModel);
        UITheme.styleTable(cartTable);
        cartTable.setRowHeight(34);

        cartTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(115);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(55);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        cartTable.getColumnModel().getColumn(6).setPreferredWidth(50);
        cartTable.getColumnModel().getColumn(7).setPreferredWidth(95);

        // Clean cell renderer for editable cells matching application theme
        DefaultTableCellRenderer editableRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    setBackground(Color.WHITE);
                    setForeground(UITheme.COLOR_TEXT_PRIMARY);
                    setFont(col == 4 ? UITheme.FONT_REGULAR_BOLD : UITheme.FONT_REGULAR);
                }
                setHorizontalAlignment(col == 4 ? CENTER : LEFT);
                return c;
            }
        };
        cartTable.getColumnModel().getColumn(0).setCellRenderer(editableRenderer);
        cartTable.getColumnModel().getColumn(4).setCellRenderer(editableRenderer);

        cartTableModel.addTableModelListener(e -> {
            if (isUpdatingTable) return;
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row < 0) return;

                if (col == 0) {
                    Object val = cartTableModel.getValueAt(row, 0);
                    if (val != null) {
                        String inputBarcode = val.toString().trim();
                        if (!inputBarcode.isEmpty()) {
                            SwingUtilities.invokeLater(() -> handleTableCellBarcodeEntered(row, inputBarcode));
                        }
                    }
                } else if (col == 4) {
                    Object val = cartTableModel.getValueAt(row, 4);
                    if (val != null) {
                        try {
                            int newQty = Integer.parseInt(val.toString().trim());
                            SwingUtilities.invokeLater(() -> handleTableCellQtyChanged(row, newQty));
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(this, "Please enter a valid numeric quantity.", "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
                            updateCartTable();
                        }
                    }
                }
            }
        });

        // Cart Actions Toolbar matching application UITheme
        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        cartActions.setBackground(UITheme.COLOR_PANEL_BG);

        JButton btnAddRow = UITheme.createButton("+ New Scan Row", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnAddRow.setToolTipText("Add an empty row to type or scan a barcode");
        btnAddRow.addActionListener(e -> addNewBlankCartRow());

        JButton btnPlus = UITheme.createButton("+ Increase Qty", UITheme.COLOR_SECONDARY, Color.WHITE);
        btnPlus.addActionListener(e -> modifyCartQty(1));

        JButton btnMinus = UITheme.createButton("- Decrease Qty", UITheme.COLOR_SECONDARY, Color.WHITE);
        btnMinus.addActionListener(e -> modifyCartQty(-1));

        JButton btnSerials = UITheme.createButton("Assign S/N (IMEI)", UITheme.COLOR_PRIMARY_DARK, Color.WHITE);
        btnSerials.addActionListener(e -> editSelectedSerials());

        JButton btnRemove = UITheme.createButton("Remove Item", UITheme.COLOR_DANGER, Color.WHITE);
        btnRemove.addActionListener(e -> removeSelectedCartItem());

        JButton btnClear = UITheme.createButton("Clear Bill", UITheme.COLOR_SECONDARY, Color.WHITE);
        btnClear.addActionListener(e -> {
            billingService.clearCart();
            updateCartTable();
        });

        cartActions.add(btnAddRow);
        cartActions.add(btnPlus);
        cartActions.add(btnMinus);
        cartActions.add(btnSerials);
        cartActions.add(btnRemove);
        cartActions.add(btnClear);

        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        panel.add(cartActions, BorderLayout.SOUTH);

        return panel;
    }

    // --- PRODUCT SEARCH DROPDOWN POPUP ON BARCODE/ITEM SCANNER ---
    private void initProductSearchPopup() {
        productSearchListModel = new DefaultListModel<>();
        productSearchList = new JList<>(productSearchListModel);
        productSearchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productSearchList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productSearchList.setFocusable(false);
        productSearchList.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // High-performance cell renderer (reusing single renderer stamp)
        productSearchList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product) {
                    Product p = (Product) value;
                    String sym = dataStore.getSettings().getCurrencySymbol();
                    double price = (billingService.getCustomerClassification() == BillingService.CustomerClassification.WHOLESALE)
                            ? p.getWholesalePrice() : p.getRetailPrice();

                    setText(String.format("  [%s]  %s - %s (%s)  |  %s %,.2f  |  Stock: %d",
                            p.getSku(), p.getBrand(), p.getName(), p.getModelNumber(), sym, price, p.getStockQuantity()));
                    setBorder(new EmptyBorder(5, 8, 5, 8));

                    if (isSelected) {
                        setBackground(new Color(224, 231, 255));
                        setForeground(UITheme.COLOR_TEXT_PRIMARY);
                    } else {
                        setBackground(index % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                        setForeground(p.getStockQuantity() <= 3 ? UITheme.COLOR_DANGER : UITheme.COLOR_TEXT_PRIMARY);
                    }
                }
                return this;
            }
        });

        productSearchList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Product selected = productSearchList.getSelectedValue();
                if (selected != null) {
                    selectProductFromPopup(selected);
                }
            }
        });

        productSearchScroll = new JScrollPane(productSearchList);
        productSearchScroll.setBorder(new LineBorder(UITheme.COLOR_BORDER, 1));

        Window ancestor = (parentWindow != null) ? parentWindow : SwingUtilities.getWindowAncestor(this);
        productDropdownWindow = (ancestor != null) ? new JWindow(ancestor) : new JWindow();
        productDropdownWindow.setFocusableWindowState(false);
        productDropdownWindow.setFocusable(false);
        productDropdownWindow.getContentPane().add(productSearchScroll);

        if (ancestor != null) {
            ancestor.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentMoved(java.awt.event.ComponentEvent e) {
                    hideProductDropdown();
                }
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    hideProductDropdown();
                }
            });
        }
    }

    private void showProductSearchPopup() {
        if (barcodeScanField == null || !barcodeScanField.isShowing()) return;
        String text = barcodeScanField.getText().trim();
        updateProductSearchList(text);
        if (productSearchListModel.isEmpty()) {
            hideProductDropdown();
            return;
        }

        if (productDropdownWindow == null) {
            initProductSearchPopup();
        }

        try {
            Point loc = barcodeScanField.getLocationOnScreen();
            int width = Math.max(barcodeScanField.getWidth(), 550);
            int rowHeight = 28;
            int listHeight = Math.min(250, Math.max(50, productSearchListModel.size() * rowHeight + 4));
            productDropdownWindow.setBounds(loc.x, loc.y + barcodeScanField.getHeight() + 2, width, listHeight);
            if (!productDropdownWindow.isVisible()) {
                productDropdownWindow.setVisible(true);
            }
        } catch (Exception ignored) {}
    }

    private void hideProductDropdown() {
        if (productDropdownWindow != null && productDropdownWindow.isVisible()) {
            productDropdownWindow.setVisible(false);
        }
    }

    private void toggleProductDropdown() {
        if (productDropdownWindow != null && productDropdownWindow.isVisible()) {
            hideProductDropdown();
        } else {
            showProductSearchPopup();
        }
    }

    private void scheduleHideDropdown() {
        Timer t = new Timer(200, evt -> {
            if (productDropdownWindow != null && productDropdownWindow.isVisible()) {
                Point p = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
                if (p != null && productDropdownWindow.getBounds().contains(p)) {
                    return; // Pointer is over dropdown
                }
                hideProductDropdown();
            }
        });
        t.setRepeats(false);
        t.start();
    }

    public void updateProductSearchList(String filter) {
        if (productSearchListModel == null) return;
        productSearchListModel.clear();
        List<Product> all = inventoryService.getAllProducts();
        String f = (filter == null) ? "" : filter.toLowerCase().trim();

        for (Product p : all) {
            if (f.isEmpty()) {
                productSearchListModel.addElement(p);
            } else {
                String combined = (p.getSku() + " " + p.getName() + " " + p.getBrand() + " " + p.getModelNumber()).toLowerCase();
                if (combined.contains(f)) {
                    productSearchListModel.addElement(p);
                }
            }
        }
        if (productSearchListModel.getSize() > 0 && productSearchList != null) {
            productSearchList.setSelectedIndex(0);
        }
    }

    private void selectProductFromPopup(Product p) {
        if (p != null) {
            hideProductDropdown();
            barcodeScanField.setText("");
            handleBarcodeScan(p.getSku());
            barcodeScanField.requestFocusInWindow();
        }
    }

    public void populateQuickItems() {
        // Quick-pick items removed in favor of clickable barcode product search popup
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // --- RIGHT PANEL (30%): Checkout & Settlement Panel ---
    private JPanel createRightCheckoutPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(UITheme.COLOR_PANEL_BG);
        rightPanel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Top: Title and Total Amount Payable Banner
        JPanel topBox = new JPanel(new BorderLayout(8, 8));
        topBox.setOpaque(false);

        JLabel lblTitle = new JLabel("Checkout & Settlement");
        lblTitle.setFont(UITheme.FONT_SUBTITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY_DARK);
        topBox.add(lblTitle, BorderLayout.NORTH);

        JPanel heroPanel = new JPanel(new BorderLayout(4, 4));
        heroPanel.setBackground(new Color(239, 246, 255)); // Soft blue-50 card tint
        heroPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(191, 219, 254), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblHeroPrompt = new JLabel("TOTAL AMOUNT PAYABLE");
        lblHeroPrompt.setFont(UITheme.FONT_SMALL_BOLD);
        lblHeroPrompt.setForeground(UITheme.COLOR_PRIMARY_DARK);

        lblHeroTotal = new JLabel("\u20B90.00");
        lblHeroTotal.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblHeroTotal.setForeground(UITheme.COLOR_PRIMARY_DARK);

        heroPanel.add(lblHeroPrompt, BorderLayout.NORTH);
        heroPanel.add(lblHeroTotal, BorderLayout.CENTER);
        topBox.add(heroPanel, BorderLayout.CENTER);

        rightPanel.add(topBox, BorderLayout.NORTH);

        // Center: Financial Breakdown Card & Payment Controls
        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);

        // 1. Figures Grid Card
        JPanel figuresCard = new JPanel(new GridLayout(4, 2, 8, 8));
        figuresCard.setBackground(new Color(248, 250, 252));
        figuresCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        lblReceiptSubtotal = new JLabel("Subtotal: \u20B90.00");
        lblReceiptSubtotal.setFont(UITheme.FONT_REGULAR_BOLD);
        lblReceiptSubtotal.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        lblReceiptDiscount = new JLabel("Discount: -\u20B90.00");
        lblReceiptDiscount.setFont(UITheme.FONT_REGULAR_BOLD);
        lblReceiptDiscount.setForeground(UITheme.COLOR_DANGER);

        lblReceiptTaxable = new JLabel("Taxable: \u20B90.00");
        lblReceiptTaxable.setFont(UITheme.FONT_REGULAR);
        lblReceiptTaxable.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        lblReceiptGst = new JLabel("Total GST: \u20B90.00");
        lblReceiptGst.setFont(UITheme.FONT_REGULAR);
        lblReceiptGst.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        figuresCard.add(lblReceiptSubtotal);
        figuresCard.add(lblReceiptTaxable);
        figuresCard.add(lblReceiptDiscount);
        figuresCard.add(lblReceiptGst);
        figuresCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        centerBox.add(figuresCard);
        centerBox.add(Box.createVerticalStrut(14));

        // 2. Payment Controls Card
        JPanel payCard = new JPanel(new BorderLayout(6, 10));
        payCard.setBackground(UITheme.COLOR_PANEL_BG);
        payCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblPayHeader = new JLabel("Payment Mode & Adjustments");
        lblPayHeader.setFont(UITheme.FONT_REGULAR_BOLD);
        lblPayHeader.setForeground(UITheme.COLOR_PRIMARY_DARK);
        payCard.add(lblPayHeader, BorderLayout.NORTH);

        JPanel payFields = new JPanel(new GridLayout(3, 1, 6, 8));
        payFields.setOpaque(false);

        // Payment Method Dropdown
        JPanel pRow1 = new JPanel(new BorderLayout(6, 0));
        pRow1.setOpaque(false);

        paymentMethodCombo = new JComboBox<>(new String[]{"Cash", "UPI / QR Code", "Credit/Debit Card", "EMI / Finance", "Net Banking", "Split Payment (Multi-Mode)"});
        paymentMethodCombo.setFont(UITheme.FONT_REGULAR);

        btnConfigSplit = UITheme.createButton("Split", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnConfigSplit.setFont(UITheme.FONT_SMALL_BOLD);
        btnConfigSplit.setVisible(false);
        btnConfigSplit.addActionListener(e -> openSplitPaymentDialog());

        paymentMethodCombo.addActionListener(e -> {
            String selected = (String) paymentMethodCombo.getSelectedItem();
            if ("Split Payment (Multi-Mode)".equals(selected)) {
                btnConfigSplit.setVisible(true);
                if (billingService.getCart().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please add items to cart before configuring split payments.", "Empty Cart", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    openSplitPaymentDialog();
                }
            } else {
                btnConfigSplit.setVisible(false);
                splitPaymentDetails = "";
            }
            updateSummaryLabels();
        });

        pRow1.add(paymentMethodCombo, BorderLayout.CENTER);
        pRow1.add(btnConfigSplit, BorderLayout.EAST);

        // Row 2: Cash Received / Amount Paid & Change Return Calculator
        JPanel pRow2 = new JPanel(new BorderLayout(6, 0));
        pRow2.setOpaque(false);
        JLabel lblTendered = new JLabel("Amount Paid:");
        lblTendered.setFont(UITheme.FONT_REGULAR_BOLD);
        lblTendered.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        JPanel pTenderedInput = new JPanel(new BorderLayout(4, 0));
        pTenderedInput.setOpaque(false);

        tenderedAmountField = UITheme.createTextField(6);
        tenderedAmountField.putClientProperty("JTextField.placeholderText", "Enter amount");
        tenderedAmountField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateChangeDue();
            }
        });

        JButton btnExact = UITheme.createSecondaryButton("Exact");
        btnExact.setFont(UITheme.FONT_SMALL_BOLD);
        btnExact.setMargin(new Insets(2, 6, 2, 6));
        btnExact.setToolTipText("Auto-fill exact bill total");
        btnExact.addActionListener(e -> {
            double grandTotal = billingService.calculateGrandTotal();
            tenderedAmountField.setText(String.format(java.util.Locale.US, "%.2f", grandTotal));
            updateChangeDue();
        });

        lblChangeDue = new JLabel("Change: " + dataStore.getSettings().getCurrencySymbol() + "0.00");
        lblChangeDue.setFont(UITheme.FONT_SMALL_BOLD);
        lblChangeDue.setOpaque(true);
        lblChangeDue.setBackground(new Color(241, 245, 249));
        lblChangeDue.setForeground(new Color(71, 85, 105));
        lblChangeDue.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(3, 6, 3, 6)
        ));

        pTenderedInput.add(tenderedAmountField, BorderLayout.CENTER);
        pTenderedInput.add(btnExact, BorderLayout.EAST);

        pRow2.add(lblTendered, BorderLayout.WEST);
        pRow2.add(pTenderedInput, BorderLayout.CENTER);
        pRow2.add(lblChangeDue, BorderLayout.EAST);

        // Row 3: Bill Discount % with Quick Preset Chips
        JPanel pRow3 = new JPanel(new BorderLayout(6, 0));
        pRow3.setOpaque(false);
        JLabel lblDisc = new JLabel("Discount %:");
        lblDisc.setFont(UITheme.FONT_REGULAR_BOLD);
        lblDisc.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        JPanel pDiscControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pDiscControls.setOpaque(false);

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
                updateChangeDue();
            }
        });
        pDiscControls.add(overallDiscountField);

        int[] discountPresets = {0, 5, 10, 15};
        for (int p : discountPresets) {
            JButton btnPreset = UITheme.createSecondaryButton(p + "%");
            btnPreset.setFont(UITheme.FONT_SMALL);
            btnPreset.setMargin(new Insets(1, 4, 1, 4));
            btnPreset.addActionListener(e -> {
                overallDiscountField.setText(String.valueOf(p));
                billingService.setOverallDiscountPercent(p);
                updateSummaryLabels();
                updateChangeDue();
            });
            pDiscControls.add(btnPreset);
        }

        pRow3.add(lblDisc, BorderLayout.WEST);
        pRow3.add(pDiscControls, BorderLayout.CENTER);

        payFields.add(pRow1);
        payFields.add(pRow2);
        payFields.add(pRow3);

        payCard.add(payFields, BorderLayout.CENTER);
        payCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 175));

        centerBox.add(payCard);
        rightPanel.add(centerBox, BorderLayout.CENTER);

        // Bottom: Large Checkout Button
        JButton btnCheckout = UITheme.createButton("COMPLETE SALE & PRINT BILL >", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCheckout.setPreferredSize(new Dimension(0, 50));
        btnCheckout.addActionListener(e -> executeCheckout());

        rightPanel.add(btnCheckout, BorderLayout.SOUTH);
        return rightPanel;
    }

    // --- BARCODE LOOKUP & AUTO-FILL LOGIC ---

    public Product findProductByBarcode(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String query = input.trim();
        String lowerQuery = query.toLowerCase();

        List<Product> allProducts = inventoryService.getAllProducts();

        // 1. Exact Barcode / SKU match
        for (Product p : allProducts) {
            if (p.getSku() != null && p.getSku().equalsIgnoreCase(query)) {
                return p;
            }
        }
        // 2. Exact Product ID match (e.g. P1001)
        for (Product p : allProducts) {
            if (p.getId() != null && p.getId().equalsIgnoreCase(query)) {
                return p;
            }
        }
        // 3. Exact Model Number match (e.g. A2848, SM-S928B)
        for (Product p : allProducts) {
            if (p.getModelNumber() != null && p.getModelNumber().equalsIgnoreCase(query)) {
                return p;
            }
        }
        // 4. Exact Product Name match
        for (Product p : allProducts) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(query)) {
                return p;
            }
        }
        // 5. Exact Brand + Name match
        for (Product p : allProducts) {
            String brandName = (p.getBrand() != null ? p.getBrand() + " " : "") + (p.getName() != null ? p.getName() : "");
            if (brandName.trim().equalsIgnoreCase(query)) {
                return p;
            }
        }
        // 6. StartsWith / Prefix Barcode / SKU match
        for (Product p : allProducts) {
            if (p.getSku() != null && p.getSku().toLowerCase().startsWith(lowerQuery)) {
                return p;
            }
        }
        // 7. Product Name contains search query
        for (Product p : allProducts) {
            if (p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) {
                return p;
            }
        }
        // 8. Brand + Product Name contains search query
        for (Product p : allProducts) {
            String combined = ((p.getBrand() != null ? p.getBrand() : "") + " " + (p.getName() != null ? p.getName() : "")).toLowerCase();
            if (combined.contains(lowerQuery)) {
                return p;
            }
        }
        // 9. Model number contains search query
        for (Product p : allProducts) {
            if (p.getModelNumber() != null && p.getModelNumber().toLowerCase().contains(lowerQuery)) {
                return p;
            }
        }
        return null;
    }

    public boolean handleBarcodeScan(String barcode) {
        Product p = findProductByBarcode(barcode);
        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "No product found for Barcode / SKU / Product Name: '" + barcode + "'.\nPlease verify the entry or check inventory.",
                    "Product Not Found",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (p.getStockQuantity() <= 0) {
            JOptionPane.showMessageDialog(this,
                    p.getName() + " is currently Out of Stock!",
                    "Out of Stock",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        billingService.addToCart(p, 1);

        if (p.isRequiresSerial()) {
            promptSerialEntry(p);
        }

        updateCartTable();
        return true;
    }

    private void handleTableCellBarcodeEntered(int row, String inputBarcode) {
        Product p = findProductByBarcode(inputBarcode);
        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "No product found for Barcode / SKU / Product Name: '" + inputBarcode + "'.",
                    "Item Not Found",
                    JOptionPane.WARNING_MESSAGE);
            updateCartTable();
            return;
        }

        if (p.getStockQuantity() <= 0) {
            JOptionPane.showMessageDialog(this,
                    p.getName() + " is currently Out of Stock!",
                    "Out of Stock",
                    JOptionPane.WARNING_MESSAGE);
            updateCartTable();
            return;
        }

        List<CartItem> cart = billingService.getCart();
        if (row < cart.size()) {
            CartItem existing = cart.get(row);
            int currentQty = existing.getQuantity();
            billingService.removeItem(existing.getProduct().getId());
            billingService.addToCart(p, currentQty > 0 ? currentQty : 1);
        } else {
            billingService.addToCart(p, 1);
        }

        if (p.isRequiresSerial()) {
            promptSerialEntry(p);
        }

        updateCartTable();

        if (cartTable.getRowCount() > 0) {
            int selectRow = Math.min(row, cartTable.getRowCount() - 1);
            cartTable.setRowSelectionInterval(selectRow, selectRow);
        }
    }

    private void handleTableCellQtyChanged(int row, int newQty) {
        List<CartItem> cart = billingService.getCart();
        if (row < 0 || row >= cart.size()) {
            updateCartTable();
            return;
        }

        CartItem item = cart.get(row);
        if (newQty <= 0) {
            billingService.removeItem(item.getProduct().getId());
        } else if (newQty > item.getProduct().getStockQuantity()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot exceed available stock of " + item.getProduct().getStockQuantity() + " units for " + item.getProduct().getName(),
                    "Stock Limit Exceeded",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            billingService.updateQuantity(item.getProduct().getId(), newQty);
            if (item.getProduct().isRequiresSerial() && newQty > item.getSerialNumbers().size()) {
                promptSerialEntry(item.getProduct());
            }
        }
        updateCartTable();
    }

    private void addNewBlankCartRow() {
        isUpdatingTable = true;
        cartTableModel.addRow(new Object[]{
                "", "<- Enter Barcode / SKU to auto-fill", "", "", 1, "-", "-", "-"
        });
        isUpdatingTable = false;
        int newRow = cartTableModel.getRowCount() - 1;
        cartTable.setRowSelectionInterval(newRow, newRow);
        cartTable.editCellAt(newRow, 0);
        if (cartTable.getEditorComponent() != null) {
            cartTable.getEditorComponent().requestFocusInWindow();
        }
    }

    public void updateCartTable() {
        if (isUpdatingTable || cartTableModel == null) return;
        if (cartTable != null && cartTable.isEditing()) {
            TableCellEditor editor = cartTable.getCellEditor();
            if (editor != null) {
                editor.cancelCellEditing();
            }
        }
        isUpdatingTable = true;
        try {
            cartTableModel.setRowCount(0);
            String sym = dataStore.getSettings().getCurrencySymbol();

            for (CartItem ci : billingService.getCart()) {
                Product p = ci.getProduct();
                String serials = ci.getSerialNumbers().isEmpty() ?
                        (p.isRequiresSerial() ? "(! Missing S/N)" : "-") :
                        String.join(", ", ci.getSerialNumbers());

                cartTableModel.addRow(new Object[]{
                        p.getSku(),
                        p.getName(),
                        p.getBrand() + " (" + p.getModelNumber() + ")",
                        serials,
                        ci.getQuantity(),
                        UITheme.formatCurrency(ci.getUnitPrice(), sym),
                        ci.getDiscountPercent() > 0 ? (ci.getDiscountPercent() + "%") : "-",
                        UITheme.formatCurrency(ci.getLineTotal(), sym)
                });
            }
        } finally {
            isUpdatingTable = false;
        }

        updateSummaryLabels();
    }

    private void updateSummaryLabels() {
        String sym = dataStore.getSettings().getCurrencySymbol();
        double subtotal = billingService.calculateSubtotal();
        double discount = billingService.calculateTotalDiscounts();
        double taxable = billingService.calculateTaxableAmount();
        double totalTax = billingService.calculateTotalTax();
        double grandTotal = billingService.calculateGrandTotal();

        if (lblHeroTotal != null) {
            lblHeroTotal.setText(UITheme.formatCurrency(grandTotal, sym));
        }
        if (lblReceiptSubtotal != null) {
            lblReceiptSubtotal.setText("Subtotal: " + UITheme.formatCurrency(subtotal, sym));
        }
        if (lblReceiptDiscount != null) {
            lblReceiptDiscount.setText("Discount: -" + UITheme.formatCurrency(discount, sym));
        }
        if (lblReceiptTaxable != null) {
            lblReceiptTaxable.setText("Taxable: " + UITheme.formatCurrency(taxable, sym));
        }
        if (lblReceiptGst != null) {
            lblReceiptGst.setText("Total GST: " + UITheme.formatCurrency(totalTax, sym));
        }

        updateChangeDue();
    }

    private void updateChangeDue() {
        if (lblChangeDue == null || tenderedAmountField == null) return;
        String text = tenderedAmountField.getText().trim();
        double grandTotal = billingService.calculateGrandTotal();
        String sym = dataStore.getSettings().getCurrencySymbol();

        if (text.isEmpty()) {
            lblChangeDue.setText("Change: " + sym + "0.00");
            lblChangeDue.setBackground(new Color(241, 245, 249));
            lblChangeDue.setForeground(new Color(71, 85, 105));
            return;
        }

        try {
            double tendered = Double.parseDouble(text);
            double diff = tendered - grandTotal;
            if (diff >= 0) {
                lblChangeDue.setText("Change: " + UITheme.formatCurrency(diff, sym));
                lblChangeDue.setBackground(new Color(220, 252, 231)); // light green
                lblChangeDue.setForeground(new Color(22, 101, 52)); // dark green
            } else {
                lblChangeDue.setText("Due: " + UITheme.formatCurrency(-diff, sym));
                lblChangeDue.setBackground(new Color(254, 226, 226)); // light red
                lblChangeDue.setForeground(new Color(220, 38, 38)); // dark red
            }
        } catch (NumberFormatException e) {
            lblChangeDue.setText("Invalid");
            lblChangeDue.setBackground(new Color(254, 226, 226));
            lblChangeDue.setForeground(new Color(220, 38, 38));
        }
    }

    private void modifyCartQty(int delta) {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= billingService.getCart().size()) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart table first.", "Selection Needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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
        if (row < 0 || row >= billingService.getCart().size()) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart table first.", "Selection Needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        CartItem item = billingService.getCart().get(row);
        promptSerialEntry(item.getProduct());
        updateCartTable();
    }

    private void removeSelectedCartItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= billingService.getCart().size()) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart table first.", "Selection Needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        CartItem item = billingService.getCart().get(row);
        billingService.removeItem(item.getProduct().getId());
        updateCartTable();
    }

    private void promptSerialEntry(Product p) {
        if (parentWindow == null) return;
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
        updateSummaryLabels();
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
        }
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
            if (tenderedAmountField != null) {
                tenderedAmountField.setText(String.format(java.util.Locale.US, "%.2f", grandTotal));
                updateChangeDue();
            }
        }
        updateSummaryLabels();
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

        String paymentRef = (splitPaymentDetails != null && !splitPaymentDetails.isEmpty())
                ? splitPaymentDetails
                : (tenderedAmountField != null && !tenderedAmountField.getText().trim().isEmpty()
                    ? "Paid: " + tenderedAmountField.getText().trim()
                    : "");

        try {
            Invoice invoice = billingService.checkout(customer, paymentMethod, paymentRef, "", splitPaymentDetails);

            setClassification(BillingService.CustomerClassification.RETAIL);
            splitPaymentDetails = "";
            btnConfigSplit.setVisible(false);
            paymentMethodCombo.setSelectedIndex(0);
            updateCartTable();
            custPhoneField.setText("");
            custNameField.setText("");
            custEmailField.setText("");
            custAddressField.setText("");
            if (tenderedAmountField != null) {
                tenderedAmountField.setText("");
            }
            updateChangeDue();
            overallDiscountField.setText("0");

            InvoicePreviewDialog previewDialog = new InvoicePreviewDialog(parentWindow, invoice);
            previewDialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Checkout failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- COMPATIBILITY & SYSTEM TESTS METHODS ---

    public void refreshProductList() {
        syncCategoriesIfChanged();
        updateColumnHeaders();
        populateQuickItems();
        if (productSearchListModel != null) {
            updateProductSearchList("");
        }
    }

    public void updateColumnHeaders() {
        if (cartTable == null || cartTable.getColumnModel() == null) return;
        ShopSettings s = dataStore.getSettings();
        if (cartTable.getColumnModel().getColumnCount() > 0) {
            cartTable.getColumnModel().getColumn(0).setHeaderValue(s.getColumnDisplayName("SKU"));
        }
        if (cartTable.getColumnModel().getColumnCount() > 1) {
            cartTable.getColumnModel().getColumn(1).setHeaderValue(s.getColumnDisplayName("Product Name"));
        }
        if (cartTable.getTableHeader() != null) {
            cartTable.getTableHeader().repaint();
        }
    }

    public void updateCategoryFilter() {
        if (categoryCombo == null) return;
        String prevSelection = (String) categoryCombo.getSelectedItem();
        List<String> latest = inventoryService.getAllCategories();

        isUpdatingCategories = true;
        categoryCombo.removeAllItems();
        categoryCombo.addItem("All Categories");
        boolean prevFound = false;
        for (String c : latest) {
            categoryCombo.addItem(c);
            if (c.equals(prevSelection)) {
                prevFound = true;
            }
        }

        if (prevFound && prevSelection != null) {
            categoryCombo.setSelectedItem(prevSelection);
        } else {
            categoryCombo.setSelectedIndex(0);
        }
        isUpdatingCategories = false;
    }

    public void syncCategoriesIfChanged() {
        if (categoryCombo == null) return;
        List<String> latest = inventoryService.getAllCategories();
        boolean matches = (categoryCombo.getItemCount() == latest.size() + 1);
        if (matches) {
            for (int i = 0; i < latest.size(); i++) {
                if (!latest.get(i).equals(categoryCombo.getItemAt(i + 1))) {
                    matches = false;
                    break;
                }
            }
        }
        if (!matches) {
            updateCategoryFilter();
        }
    }

    public JComboBox<String> getCategoryCombo() {
        return categoryCombo;
    }

    public JTable getCartTable() {
        return cartTable;
    }

    public JTextField getBarcodeScanField() {
        return barcodeScanField;
    }

    public DefaultListModel<Product> getProductSearchListModel() {
        return productSearchListModel;
    }
}
