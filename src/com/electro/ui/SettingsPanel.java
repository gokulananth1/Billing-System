package com.electro.ui;

import com.electro.model.ShopSettings;
import com.electro.service.AuthService;
import com.electro.service.DataStore;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Settings panel for store configuration, tax rates, contact details, invoice formatting,
 * and admin profile management.
 */
public class SettingsPanel extends JPanel {
    private final DataStore dataStore;
    private final AuthService authService;

    private JTextField tfStoreName;
    private JTextField tfTagline;
    private JTextField tfAddress;
    private JTextField tfPhone;
    private JTextField tfEmail;
    private JTextField tfGstin;
    private JTextField tfCurrency;
    private JTextField tfInvoiceFooter;
    private JTextArea taTerms;
    private JTextField tfAdminDisplayName;

    private final Runnable onSettingsUpdated;

    public SettingsPanel(Runnable onSettingsUpdated) {
        this.dataStore = DataStore.getInstance();
        this.authService = AuthService.getInstance();
        this.onSettingsUpdated = onSettingsUpdated;


        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.COLOR_BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(createContent(), BorderLayout.CENTER);
        loadValues();
    }

    private JPanel createContent() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(UITheme.COLOR_PANEL_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel("Store Configuration & Invoice Header Settings");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY_DARK);
        card.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(9, 2, 10, 10));
        grid.setBackground(UITheme.COLOR_PANEL_BG);

        tfStoreName = UITheme.createTextField(20);
        tfTagline = UITheme.createTextField(20);
        tfAddress = UITheme.createTextField(20);
        tfPhone = UITheme.createTextField(20);
        tfEmail = UITheme.createTextField(20);
        tfGstin = UITheme.createTextField(20);
        tfCurrency = UITheme.createTextField(10);
        tfInvoiceFooter = UITheme.createTextField(20);
        tfAdminDisplayName = UITheme.createTextField(20);

        grid.add(new JLabel("Store / Business Name:")); grid.add(tfStoreName);
        grid.add(new JLabel("Tagline / Slogan:")); grid.add(tfTagline);
        grid.add(new JLabel("Shop Address:")); grid.add(tfAddress);
        grid.add(new JLabel("Support Phone:")); grid.add(tfPhone);
        grid.add(new JLabel("Support Email:")); grid.add(tfEmail);
        grid.add(new JLabel("GSTIN / Tax ID:")); grid.add(tfGstin);
        grid.add(new JLabel("Currency Symbol (e.g. \u20B9, $, \u20AC):")); grid.add(tfCurrency);
        grid.add(new JLabel("Invoice Footer Message:")); grid.add(tfInvoiceFooter);

        // Admin Profile separator row
        JLabel lblAdminSection = new JLabel("Admin Profile \u2014 Display Name (shown in header):");
        lblAdminSection.setFont(UITheme.FONT_REGULAR_BOLD);
        lblAdminSection.setForeground(new Color(79, 70, 229));
        grid.add(lblAdminSection); grid.add(tfAdminDisplayName);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(UITheme.COLOR_PANEL_BG);
        center.add(grid, BorderLayout.NORTH);

        JPanel termsPanel = new JPanel(new BorderLayout(6, 6));
        termsPanel.setBackground(UITheme.COLOR_PANEL_BG);
        termsPanel.add(new JLabel("Invoice Terms & Conditions / Warranty Policy:"), BorderLayout.NORTH);

        taTerms = new JTextArea(5, 40);
        taTerms.setFont(UITheme.FONT_REGULAR);
        taTerms.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        termsPanel.add(new JScrollPane(taTerms), BorderLayout.CENTER);

        center.add(termsPanel, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setBackground(UITheme.COLOR_PANEL_BG);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        leftActions.setBackground(UITheme.COLOR_PANEL_BG);
        JButton btnResetSales = UITheme.createButton("Reset Sales Data", UITheme.COLOR_DANGER, Color.WHITE);
        btnResetSales.setPreferredSize(new Dimension(150, 38));
        btnResetSales.setToolTipText("Clear all invoices, sales history, and warranty transactions (Admin only)");
        btnResetSales.addActionListener(e -> handleResetSalesData());

        JButton btnChangeAdminPass = UITheme.createButton("Change Admin Password", new Color(79, 70, 229), Color.WHITE);
        btnChangeAdminPass.setPreferredSize(new Dimension(195, 38));
        btnChangeAdminPass.setToolTipText("Change the password for the Administrator account");
        btnChangeAdminPass.addActionListener(e -> handleChangeAdminPassword());

        JButton btnManageStaff = UITheme.createButton("Manage Staff Accounts", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnManageStaff.setPreferredSize(new Dimension(190, 38));
        btnManageStaff.setToolTipText("Add cashiers, manage user roles and passwords (Admin only)");
        btnManageStaff.addActionListener(e -> handleManageStaff());

        leftActions.add(btnResetSales);
        leftActions.add(btnChangeAdminPass);
        leftActions.add(btnManageStaff);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        rightActions.setBackground(UITheme.COLOR_PANEL_BG);
        JButton btnSave = UITheme.createButton("Save Changes", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnSave.setPreferredSize(new Dimension(140, 38));
        btnSave.addActionListener(e -> saveValues());
        rightActions.add(btnSave);

        footer.add(leftActions, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private void handleChangeAdminPassword() {
        com.electro.model.User user = com.electro.service.AuthService.getInstance().getCurrentUser();
        if (user != null && !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can change admin credentials.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        com.electro.model.User adminUser = user != null ? user : com.electro.service.AuthService.getInstance().getUserByUsername("admin");
        if (adminUser == null) {
            adminUser = com.electro.service.AuthService.getInstance().getUserByUsername("admin");
        }

        ChangePasswordDialog dlg = new ChangePasswordDialog(SwingUtilities.getWindowAncestor(this), adminUser);
        dlg.setVisible(true);
    }

    private void handleManageStaff() {
        com.electro.model.User user = com.electro.service.AuthService.getInstance().getCurrentUser();
        if (user != null && !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can manage staff accounts.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UserManagerDialog dlg = new UserManagerDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
    }

    private void handleResetSalesData() {
        com.electro.model.User user = com.electro.service.AuthService.getInstance().getCurrentUser();
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
        }
    }

    private void loadValues() {
        ShopSettings s = dataStore.getSettings();
        tfStoreName.setText(s.getStoreName());
        tfTagline.setText(s.getTagline());
        tfAddress.setText(s.getAddress());
        tfPhone.setText(s.getPhone());
        tfEmail.setText(s.getEmail());
        tfGstin.setText(s.getGstin());
        tfCurrency.setText(s.getCurrencySymbol());
        tfInvoiceFooter.setText(s.getInvoiceFooter());
        taTerms.setText(s.getTermsAndConditions());

        // Load current admin's display name
        com.electro.model.User currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.isAdmin()) {
            tfAdminDisplayName.setText(currentUser.getFullName());
        } else {
            com.electro.model.User admin = authService.getUserByUsername("admin");
            tfAdminDisplayName.setText(admin != null ? admin.getFullName() : "");
        }
    }

    private void saveValues() {
        ShopSettings s = dataStore.getSettings();
        s.setStoreName(tfStoreName.getText().trim());
        s.setTagline(tfTagline.getText().trim());
        s.setAddress(tfAddress.getText().trim());
        s.setPhone(tfPhone.getText().trim());
        s.setEmail(tfEmail.getText().trim());
        s.setGstin(tfGstin.getText().trim());
        s.setCurrencySymbol(tfCurrency.getText().trim().isEmpty() ? "\u20B9" : tfCurrency.getText().trim());
        s.setInvoiceFooter(tfInvoiceFooter.getText().trim());
        s.setTermsAndConditions(taTerms.getText().trim());
        dataStore.saveSettings(s);

        // Persist admin display name change
        String newDisplayName = tfAdminDisplayName.getText().trim();
        if (!newDisplayName.isEmpty()) {
            com.electro.model.User currentUser = authService.getCurrentUser();
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
                "Shop settings and admin name saved successfully!", "Success",
                JOptionPane.INFORMATION_MESSAGE);

        if (onSettingsUpdated != null) {
            onSettingsUpdated.run();
        }
    }
}
