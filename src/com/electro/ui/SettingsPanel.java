package com.electro.ui;

import com.electro.model.Product;
import com.electro.model.ShopSettings;
import com.electro.model.User;
import com.electro.service.AuthService;
import com.electro.service.DataStore;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enterprise Admin Control Panel providing complete administrative control over the application:
 * Store profile, POS & Billing rules, Inventory controls, Staff access, and System backup/maintenance.
 */
public class SettingsPanel extends JPanel {
    private final DataStore dataStore;
    private final AuthService authService;
    private final Runnable onSettingsUpdated;

    // Tab 1: Store Profile
    private JTextField tfStoreName;
    private JTextField tfTagline;
    private JTextField tfAddress;
    private JTextField tfPhone;
    private JTextField tfEmail;
    private JTextField tfGstin;
    private JTextField tfCurrency;
    private JTextField tfAdminDisplayName;
    private JTextField tfInvoiceFooter;
    private JTextArea taTerms;

    // Tab 2: Billing & POS Rules
    private JTextField tfInvoicePrefix;
    private JSpinner spDefaultTax;
    private JCheckBox chkEnableGst;
    private JSpinner spWholesaleDiscount;
    private JCheckBox chkStrictSerial;
    private JSpinner spDefaultWarranty;

    // Tab 3: Inventory Controls
    private JSpinner spLowStockThreshold;

    // Tab 4: Security
    private JLabel lblStaffSummary;

    // Tab 5: Maintenance & Stats
    private JLabel lblStatsProducts;
    private JLabel lblStatsInvoices;
    private JLabel lblStatsWarranties;
    private JLabel lblStatsBackups;

    public SettingsPanel(Runnable onSettingsUpdated) {
        this.dataStore = DataStore.getInstance();
        this.authService = AuthService.getInstance();
        this.onSettingsUpdated = onSettingsUpdated;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.COLOR_BG);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        initUI();
        loadValues();
    }

    private void initUI() {
        // Main Tabbed Pane for categorized Admin Controls
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_REGULAR_BOLD);
        tabs.setBackground(UITheme.COLOR_PANEL_BG);

        tabs.addTab("  Store Profile & Branding  ", createStoreProfileTab());
        tabs.addTab("  Billing & POS Rules  ", createBillingRulesTab());
        tabs.addTab("  Inventory & Catalog  ", createInventoryTab());
        tabs.addTab("  Staff & Access Control  ", createSecurityTab());
        tabs.addTab("  Backup & System Maintenance  ", createMaintenanceTab());

        add(tabs, BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------
    // TAB 1: STORE PROFILE & BRANDING
    // -------------------------------------------------------------
    private JPanel createStoreProfileTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel formGrid = new JPanel(new GridLayout(9, 2, 12, 10));
        formGrid.setBackground(UITheme.COLOR_PANEL_BG);

        tfStoreName = UITheme.createTextField(20);
        tfTagline = UITheme.createTextField(20);
        tfAddress = UITheme.createTextField(20);
        tfPhone = UITheme.createTextField(20);
        tfEmail = UITheme.createTextField(20);
        tfGstin = UITheme.createTextField(20);
        tfCurrency = UITheme.createTextField(10);
        tfInvoiceFooter = UITheme.createTextField(20);
        tfAdminDisplayName = UITheme.createTextField(20);

        formGrid.add(new JLabel("Business / Store Name:")); formGrid.add(tfStoreName);
        formGrid.add(new JLabel("Tagline / Slogan:")); formGrid.add(tfTagline);
        formGrid.add(new JLabel("Store Physical Address:")); formGrid.add(tfAddress);
        formGrid.add(new JLabel("Customer Support Phone:")); formGrid.add(tfPhone);
        formGrid.add(new JLabel("Customer Support Email:")); formGrid.add(tfEmail);
        formGrid.add(new JLabel("GSTIN / Tax ID Number:")); formGrid.add(tfGstin);
        formGrid.add(new JLabel("Currency Symbol (e.g. \u20B9, $, \u20AC, \u00A3):")); formGrid.add(tfCurrency);
        formGrid.add(new JLabel("Invoice Footer Message:")); formGrid.add(tfInvoiceFooter);

        JLabel lblAdminSection = new JLabel("Admin Profile - Display Name:");
        lblAdminSection.setFont(UITheme.FONT_REGULAR_BOLD);
        lblAdminSection.setForeground(new Color(79, 70, 229));
        formGrid.add(lblAdminSection); formGrid.add(tfAdminDisplayName);

        JPanel termsPanel = new JPanel(new BorderLayout(6, 6));
        termsPanel.setBackground(UITheme.COLOR_PANEL_BG);
        termsPanel.add(new JLabel("Printed Invoice Terms & Conditions / Warranty Policy:"), BorderLayout.NORTH);

        taTerms = new JTextArea(4, 40);
        taTerms.setFont(UITheme.FONT_REGULAR);
        taTerms.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        termsPanel.add(new JScrollPane(taTerms), BorderLayout.CENTER);

        panel.add(formGrid, BorderLayout.NORTH);
        panel.add(termsPanel, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------
    // TAB 2: BILLING & POS RULES
    // -------------------------------------------------------------
    private JPanel createBillingRulesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel grid = new JPanel(new GridLayout(6, 2, 14, 14));
        grid.setBackground(UITheme.COLOR_PANEL_BG);

        tfInvoicePrefix = UITheme.createTextField(10);
        spDefaultTax = new JSpinner(new SpinnerNumberModel(18.0, 0.0, 100.0, 1.0));
        spDefaultTax.setFont(UITheme.FONT_REGULAR);

        chkEnableGst = new JCheckBox("Calculate & Charge GST on Invoices (Uncheck for composition / non-tax bills)");
        chkEnableGst.setFont(UITheme.FONT_REGULAR);
        chkEnableGst.setBackground(UITheme.COLOR_PANEL_BG);

        spWholesaleDiscount = new JSpinner(new SpinnerNumberModel(8.0, 0.0, 50.0, 0.5));
        spWholesaleDiscount.setFont(UITheme.FONT_REGULAR);

        chkStrictSerial = new JCheckBox("Enforce Mandatory Serial/IMEI Entry (Checkout blocked if serial missing on tracked goods)");
        chkStrictSerial.setFont(UITheme.FONT_REGULAR);
        chkStrictSerial.setBackground(UITheme.COLOR_PANEL_BG);

        spDefaultWarranty = new JSpinner(new SpinnerNumberModel(12, 1, 120, 1));
        spDefaultWarranty.setFont(UITheme.FONT_REGULAR);

        grid.add(new JLabel("Invoice ID Prefix (e.g. INV, BILL, POS):")); grid.add(tfInvoicePrefix);
        grid.add(new JLabel("Default GST Rate (%):")); grid.add(spDefaultTax);
        grid.add(new JLabel("GST Billing Mode:")); grid.add(chkEnableGst);
        grid.add(new JLabel("Wholesale B2B Rate Default Discount (%):")); grid.add(spWholesaleDiscount);
        grid.add(new JLabel("Serial/IMEI Verification Policy:")); grid.add(chkStrictSerial);
        grid.add(new JLabel("Default Warranty Period (Months):")); grid.add(spDefaultWarranty);

        panel.add(grid, BorderLayout.NORTH);

        // Explanatory Note Card
        JPanel noteCard = new JPanel(new BorderLayout(6, 6));
        noteCard.setBackground(new Color(248, 250, 252));
        noteCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));
        JLabel lblNote = new JLabel("<html><b>Billing Policy Guide:</b><br>"
                + "- Changes to Invoice Prefix apply immediately to future sales transactions.<br>"
                + "- Disabling GST sets invoice tax to 0% for tax-exempt operations without altering catalog tax rates.<br>"
                + "- Strict Serial Tracking prevents cashier checkout unless a unique IMEI/SN is captured for every tracked device."
                + "</html>");
        lblNote.setFont(UITheme.FONT_SMALL);
        lblNote.setForeground(UITheme.COLOR_TEXT_MUTED);
        noteCard.add(lblNote, BorderLayout.CENTER);

        panel.add(noteCard, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------
    // TAB 3: INVENTORY & CATALOG CONTROLS
    // -------------------------------------------------------------
    private JPanel createInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel topGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        topGrid.setBackground(UITheme.COLOR_PANEL_BG);

        spLowStockThreshold = new JSpinner(new SpinnerNumberModel(4, 1, 100, 1));
        spLowStockThreshold.setFont(UITheme.FONT_REGULAR);

        topGrid.add(new JLabel("Low Stock Alert Threshold (Units):"));
        topGrid.add(spLowStockThreshold);

        panel.add(topGrid, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridLayout(2, 1, 14, 14));
        centerWrapper.setOpaque(false);

        // Column Customization Card
        JPanel colCard = new JPanel(new BorderLayout(8, 8));
        colCard.setBackground(new Color(245, 243, 255));
        colCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(196, 181, 253), 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel colTitle = new JLabel("Catalog Table Column Display Names (Admin Choice)");
        colTitle.setFont(UITheme.FONT_REGULAR_BOLD);
        colTitle.setForeground(new Color(91, 33, 182));
        colCard.add(colTitle, BorderLayout.NORTH);

        JLabel colDesc = new JLabel("<html>Customize what name should display for catalog columns (ID, SKU, Brand, Product Name, Category, Retail, Wholesale, etc.).<br>The Admin decides the exact label terminology shown on tables and reports across the store.</html>");
        colDesc.setFont(UITheme.FONT_SMALL);
        colDesc.setForeground(UITheme.COLOR_TEXT_MUTED);
        colCard.add(colDesc, BorderLayout.CENTER);

        JPanel colBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        colBtnRow.setOpaque(false);
        JButton btnEditCols = UITheme.createButton("Edit Column Display Names...", new Color(109, 40, 217), Color.WHITE);
        btnEditCols.setToolTipText("Admin: Decide display names for ID, SKU, Brand, Product Name, and other columns");
        btnEditCols.addActionListener(e -> {
            ColumnConfigDialog dlg = new ColumnConfigDialog(SwingUtilities.getWindowAncestor(this), () -> {
                if (onSettingsUpdated != null) onSettingsUpdated.run();
            });
            dlg.setVisible(true);
        });
        colBtnRow.add(btnEditCols);
        colCard.add(colBtnRow, BorderLayout.SOUTH);

        // Catalog Reset & Maintenance Card
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(new Color(254, 242, 242));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(252, 165, 165), 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel cardTitle = new JLabel("Catalog & Stock Management Actions (Admin Only)");
        cardTitle.setFont(UITheme.FONT_REGULAR_BOLD);
        cardTitle.setForeground(UITheme.COLOR_DANGER);
        card.add(cardTitle, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        btnRow.setBackground(new Color(254, 242, 242));

        JButton btnRestoreCatalog = UITheme.createButton("Restore Sample Electronics Catalog", new Color(13, 148, 136), Color.WHITE);
        btnRestoreCatalog.setToolTipText("Restores the standard 15+ demo electronics products catalog");
        btnRestoreCatalog.addActionListener(e -> handleRestoreCatalog());

        JButton btnRemoveStock = UITheme.createButton("Remove All Stock Items", UITheme.COLOR_DANGER, Color.WHITE);
        btnRemoveStock.setToolTipText("Permanently clear all products from the inventory catalog");
        btnRemoveStock.addActionListener(e -> handleRemoveAllStockItems());

        btnRow.add(btnRestoreCatalog);
        btnRow.add(btnRemoveStock);
        card.add(btnRow, BorderLayout.CENTER);

        centerWrapper.add(colCard);
        centerWrapper.add(card);
        panel.add(centerWrapper, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------
    // TAB 4: SECURITY & STAFF ACCESS
    // -------------------------------------------------------------
    private JPanel createSecurityTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel content = new JPanel(new GridLayout(2, 1, 16, 16));
        content.setBackground(UITheme.COLOR_PANEL_BG);

        // Security Actions Box
        JPanel actionBox = new JPanel(new BorderLayout(10, 10));
        actionBox.setBackground(new Color(245, 243, 255));
        actionBox.setBorder(new CompoundBorder(
                new LineBorder(new Color(196, 181, 253), 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel title = new JLabel("Administrative Credentials & Staff Access Management");
        title.setFont(UITheme.FONT_REGULAR_BOLD);
        title.setForeground(new Color(109, 40, 217));
        actionBox.add(title, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        btnRow.setBackground(new Color(245, 243, 255));

        JButton btnChangeAdminPass = UITheme.createButton("Change Admin Password", new Color(79, 70, 229), Color.WHITE);
        btnChangeAdminPass.addActionListener(e -> handleChangeAdminPassword());

        JButton btnManageStaff = UITheme.createButton("Manage Staff Accounts", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnManageStaff.addActionListener(e -> handleManageStaff());

        btnRow.add(btnChangeAdminPass);
        btnRow.add(btnManageStaff);
        actionBox.add(btnRow, BorderLayout.CENTER);

        // Staff Status Card
        JPanel statusBox = new JPanel(new BorderLayout(8, 8));
        statusBox.setBackground(new Color(248, 250, 252));
        statusBox.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblHeader = new JLabel("System Role Overview:");
        lblHeader.setFont(UITheme.FONT_REGULAR_BOLD);
        lblHeader.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        lblStaffSummary = new JLabel("Loading staff accounts...");
        lblStaffSummary.setFont(UITheme.FONT_REGULAR);
        lblStaffSummary.setForeground(UITheme.COLOR_TEXT_MUTED);

        statusBox.add(lblHeader, BorderLayout.NORTH);
        statusBox.add(lblStaffSummary, BorderLayout.CENTER);

        content.add(actionBox);
        content.add(statusBox);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    // -------------------------------------------------------------
    // TAB 5: BACKUP & SYSTEM MAINTENANCE
    // -------------------------------------------------------------
    private JPanel createMaintenanceTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel layout = new JPanel(new GridLayout(2, 1, 14, 14));
        layout.setBackground(UITheme.COLOR_PANEL_BG);

        // System Statistics Card
        JPanel statsCard = new JPanel(new GridLayout(4, 1, 6, 6));
        statsCard.setBackground(new Color(240, 253, 250));
        statsCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(153, 246, 228), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        lblStatsProducts = new JLabel("Products in Inventory: --");
        lblStatsInvoices = new JLabel("Total Invoices Recorded: --");
        lblStatsWarranties = new JLabel("Active Warranties: --");
        lblStatsBackups = new JLabel("Available Backups in backups/: --");

        statsCard.add(lblStatsProducts);
        statsCard.add(lblStatsInvoices);
        statsCard.add(lblStatsWarranties);
        statsCard.add(lblStatsBackups);

        // Operations Box
        JPanel opsBox = new JPanel(new BorderLayout(8, 8));
        opsBox.setBackground(UITheme.COLOR_PANEL_BG);
        opsBox.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblOps = new JLabel("Data Backup, Disaster Recovery & Reset Operations:");
        lblOps.setFont(UITheme.FONT_REGULAR_BOLD);
        opsBox.add(lblOps, BorderLayout.NORTH);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        btnBar.setBackground(UITheme.COLOR_PANEL_BG);

        JButton btnBackup = UITheme.createButton("Create System Backup", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnBackup.addActionListener(e -> handleBackup());

        JButton btnRestore = UITheme.createButton("Restore from Backup", new Color(13, 148, 136), Color.WHITE);
        btnRestore.addActionListener(e -> handleRestoreBackup());

        JButton btnResetSales = UITheme.createButton("Reset Sales Data", UITheme.COLOR_DANGER, Color.WHITE);
        btnResetSales.addActionListener(e -> handleResetSalesData());

        JButton btnFactoryReset = UITheme.createButton("Factory Reset (Full Wipe)", new Color(153, 27, 27), Color.WHITE);
        btnFactoryReset.addActionListener(e -> handleFactoryReset());

        btnBar.add(btnBackup);
        btnBar.add(btnRestore);
        btnBar.add(btnResetSales);
        btnBar.add(btnFactoryReset);
        opsBox.add(btnBar, BorderLayout.CENTER);

        layout.add(statsCard);
        layout.add(opsBox);

        panel.add(layout, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------
    // FOOTER ACTIONS
    // -------------------------------------------------------------
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setBackground(UITheme.COLOR_PANEL_BG);
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblNote = new JLabel("Settings changes take effect immediately across all POS screens upon saving.");
        lblNote.setFont(UITheme.FONT_SMALL);
        lblNote.setForeground(UITheme.COLOR_TEXT_MUTED);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightActions.setBackground(UITheme.COLOR_PANEL_BG);

        JButton btnSave = UITheme.createButton("Save All Changes", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnSave.setPreferredSize(new Dimension(170, 38));
        btnSave.addActionListener(e -> saveValues());
        rightActions.add(btnSave);

        footer.add(lblNote, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);
        return footer;
    }

    // -------------------------------------------------------------
    // VALUES & CONTROLS BINDING
    // -------------------------------------------------------------
    private void loadValues() {
        ShopSettings s = dataStore.getSettings();

        // Tab 1
        tfStoreName.setText(s.getStoreName());
        tfTagline.setText(s.getTagline());
        tfAddress.setText(s.getAddress());
        tfPhone.setText(s.getPhone());
        tfEmail.setText(s.getEmail());
        tfGstin.setText(s.getGstin());
        tfCurrency.setText(s.getCurrencySymbol());
        tfInvoiceFooter.setText(s.getInvoiceFooter());
        taTerms.setText(s.getTermsAndConditions());

        User currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.isAdmin()) {
            tfAdminDisplayName.setText(currentUser.getFullName());
        } else {
            User admin = authService.getUserByUsername("admin");
            tfAdminDisplayName.setText(admin != null ? admin.getFullName() : "");
        }

        // Tab 2
        tfInvoicePrefix.setText(s.getInvoicePrefix());
        spDefaultTax.setValue(s.getDefaultTaxRate());
        chkEnableGst.setSelected(s.isEnableGstBilling());
        spWholesaleDiscount.setValue(s.getWholesaleDiscountPercent());
        chkStrictSerial.setSelected(s.isStrictSerialTracking());
        spDefaultWarranty.setValue(s.getDefaultWarrantyMonths());

        // Tab 3
        spLowStockThreshold.setValue(s.getLowStockThreshold());

        // Refresh stats
        refreshMaintenanceStats();
    }

    private void saveValues() {
        ShopSettings s = dataStore.getSettings();

        // Store profile
        s.setStoreName(tfStoreName.getText().trim());
        s.setTagline(tfTagline.getText().trim());
        s.setAddress(tfAddress.getText().trim());
        s.setPhone(tfPhone.getText().trim());
        s.setEmail(tfEmail.getText().trim());
        s.setGstin(tfGstin.getText().trim());
        s.setCurrencySymbol(tfCurrency.getText().trim().isEmpty() ? "\u20B9" : tfCurrency.getText().trim());
        s.setInvoiceFooter(tfInvoiceFooter.getText().trim());
        s.setTermsAndConditions(taTerms.getText().trim());

        // Billing & POS Rules
        String prefix = tfInvoicePrefix.getText().trim().toUpperCase();
        s.setInvoicePrefix(prefix.isEmpty() ? "INV" : prefix);
        s.setDefaultTaxRate(((Number) spDefaultTax.getValue()).doubleValue());
        s.setEnableGstBilling(chkEnableGst.isSelected());
        s.setWholesaleDiscountPercent(((Number) spWholesaleDiscount.getValue()).doubleValue());
        s.setStrictSerialTracking(chkStrictSerial.isSelected());
        s.setDefaultWarrantyMonths((Integer) spDefaultWarranty.getValue());

        // Inventory
        s.setLowStockThreshold((Integer) spLowStockThreshold.getValue());

        dataStore.saveSettings(s);

        // Persist admin display name change
        String newDisplayName = tfAdminDisplayName.getText().trim();
        if (!newDisplayName.isEmpty()) {
            User currentUser = authService.getCurrentUser();
            String targetUsername = (currentUser != null && currentUser.isAdmin())
                    ? currentUser.getUsername() : "admin";
            try {
                authService.updateFullName(targetUsername, newDisplayName);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Store settings saved, but could not update admin name: " + ex.getMessage(),
                        "Warning", JOptionPane.WARNING_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this,
                "Admin configurations and store settings updated successfully!",
                "Settings Saved",
                JOptionPane.INFORMATION_MESSAGE);

        if (onSettingsUpdated != null) {
            onSettingsUpdated.run();
        }
        refreshMaintenanceStats();
    }

    private void refreshMaintenanceStats() {
        int prodCount = dataStore.getAllProducts().size();
        int invCount = dataStore.getAllInvoices().size();
        int warCount = dataStore.getAllWarranties().size();
        List<User> users = authService.getAllUsers();

        lblStatsProducts.setText("Products in Inventory: " + prodCount);
        lblStatsInvoices.setText("Total Invoices Recorded: " + invCount);
        lblStatsWarranties.setText("Active Warranties Tracked: " + warCount);

        File backupDir = new File("backups");
        int backupCount = (backupDir.exists() && backupDir.isDirectory())
                ? (backupDir.listFiles(File::isDirectory) != null ? backupDir.listFiles(File::isDirectory).length : 0)
                : 0;
        lblStatsBackups.setText("Available System Backups: " + backupCount + " snapshot(s)");

        long admins = users.stream().filter(User::isAdmin).count();
        long owners = users.stream().filter(User::isStoreOwner).count();
        long cashiers = users.stream().filter(User::isCashier).count();
        lblStaffSummary.setText(String.format("Total Accounts: %d  |  %d Administrator(s)  |  %d Store Owner(s)  |  %d Cashier(s)",
                users.size(), admins, owners, cashiers));
    }

    // -------------------------------------------------------------
    // ACTION HANDLERS
    // -------------------------------------------------------------
    private void handleChangeAdminPassword() {
        User user = authService.getCurrentUser();
        if (user != null && !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can change admin credentials.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User adminUser = user != null ? user : authService.getUserByUsername("admin");
        if (adminUser == null) {
            adminUser = authService.getUserByUsername("admin");
        }

        ChangePasswordDialog dlg = new ChangePasswordDialog(SwingUtilities.getWindowAncestor(this), adminUser);
        dlg.setVisible(true);
    }

    private void handleManageStaff() {
        User user = authService.getCurrentUser();
        if (user != null && !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can manage staff accounts.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UserManagerDialog dlg = new UserManagerDialog(SwingUtilities.getWindowAncestor(this), () -> {
            loadValues();
            if (onSettingsUpdated != null) {
                onSettingsUpdated.run();
            }
        });
        dlg.setVisible(true);
        loadValues();
    }

    private void handleRestoreCatalog() {
        User user = authService.getCurrentUser();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can restore the catalog.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Restore the standard default catalog of electronics (smartphones, laptops, audio, accessories)?\n"
                + "This will populate 15+ demo products with standard stock and warranties.",
                "Confirm Restore Default Catalog",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dataStore.restoreDefaultProducts();
            JOptionPane.showMessageDialog(this, "Default product catalog restored successfully!", "Catalog Restored", JOptionPane.INFORMATION_MESSAGE);
            if (onSettingsUpdated != null) {
                onSettingsUpdated.run();
            }
            refreshMaintenanceStats();
        }
    }

    private void handleRemoveAllStockItems() {
        User user = authService.getCurrentUser();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can remove all stock items.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int count = dataStore.getAllProducts().size();
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "The inventory catalog is already empty. There are no stock items to remove.", "Inventory Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently remove all " + count + " stock item(s) from the application?\n"
                + "This will delete all products from the inventory catalog and stock list.\n\n"
                + "This action cannot be undone. Do you wish to proceed?",
                "Confirm Remove All Stock Items",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dataStore.removeAllProducts();
            JOptionPane.showMessageDialog(this, "All stock items have been successfully removed from the application!", "Stock Removed", JOptionPane.INFORMATION_MESSAGE);
            if (onSettingsUpdated != null) {
                onSettingsUpdated.run();
            }
            refreshMaintenanceStats();
        }
    }

    private void handleResetSalesData() {
        User user = authService.getCurrentUser();
        if (user != null && !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can reset sales data.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently clear all sales data and invoice history?\n"
                + "This will erase all past transactions and reset sales revenue to zero.\n\n"
                + "Do you wish to proceed?",
                "Confirm Reset Sales Data",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dataStore.resetSalesData();
            JOptionPane.showMessageDialog(this, "All sales data and transaction history have been reset!", "Sales Data Reset", JOptionPane.INFORMATION_MESSAGE);
            if (onSettingsUpdated != null) {
                onSettingsUpdated.run();
            }
            refreshMaintenanceStats();
        }
    }

    private void handleBackup() {
        try {
            String note = JOptionPane.showInputDialog(this, "Enter an optional note or label for this backup:", "Create System Backup", JOptionPane.QUESTION_MESSAGE);
            if (note == null) return;
            String backupPath = dataStore.createBackup(note);
            JOptionPane.showMessageDialog(this, "System backup snapshot created successfully!\n\nLocation:\n" + backupPath, "Backup Created", JOptionPane.INFORMATION_MESSAGE);
            refreshMaintenanceStats();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to create backup: " + ex.getMessage(), "Backup Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRestoreBackup() {
        File backupDir = new File("backups");
        if (!backupDir.exists() || !backupDir.isDirectory() || backupDir.listFiles(File::isDirectory) == null || backupDir.listFiles(File::isDirectory).length == 0) {
            JOptionPane.showMessageDialog(this, "No previous backups found in 'backups/' directory.", "No Backups Available", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File[] dirs = backupDir.listFiles(File::isDirectory);
        String[] options = new String[dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            options[i] = dirs[i].getName();
        }
        Arrays.sort(options, Collections.reverseOrder());

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Select a backup snapshot to restore:\n\nWARNING: This will overwrite current data with the selected snapshot!",
                "Restore System from Backup",
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selected != null) {
            try {
                Path restorePath = backupDir.toPath().resolve(selected);
                dataStore.restoreBackup(restorePath);
                loadValues();
                if (onSettingsUpdated != null) {
                    onSettingsUpdated.run();
                }
                JOptionPane.showMessageDialog(this, "System successfully restored from: " + selected, "Restore Complete", JOptionPane.INFORMATION_MESSAGE);
                refreshMaintenanceStats();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to restore backup: " + ex.getMessage(), "Restore Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleFactoryReset() {
        User user = authService.getCurrentUser();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can trigger factory reset.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "CRITICAL WARNING: Factory Reset will reset the application to fresh install defaults:\n\n"
                + "- All invoices, customer purchases, and warranties will be deleted.\n"
                + "- Inventory will be reset to default sample catalog.\n"
                + "- Store configuration and billing policies will be restored to defaults.\n\n"
                + "Are you sure you want to completely reset the application?",
                "Confirm Factory Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dataStore.factoryReset();
            loadValues();
            if (onSettingsUpdated != null) {
                onSettingsUpdated.run();
            }
            JOptionPane.showMessageDialog(this, "Application has been successfully reset to factory defaults!", "Factory Reset Done", JOptionPane.INFORMATION_MESSAGE);
            refreshMaintenanceStats();
        }
    }
}
