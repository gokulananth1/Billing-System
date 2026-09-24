package com.electro.ui;

import com.electro.model.ShopSettings;
import com.electro.service.BillingService;
import com.electro.service.DataStore;
import com.electro.service.InventoryService;
import com.electro.service.WarrantyService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main application window coordinating tabs, header branding, and real-time status.
 */
public class MainFrame extends JFrame {
    private final DataStore dataStore;
    private final BillingService billingService;
    private final InventoryService inventoryService;
    private final WarrantyService warrantyService;

    private JLabel lblStoreTitle;
    private JLabel lblStoreSubtitle;
    private JLabel lblClock;
    private JLabel lblUserBadge;
    private JButton btnLogout;

    private JTabbedPane tabbedPane;
    private BillingPanel billingPanel;
    private InventoryPanel inventoryPanel;
    private InvoiceHistoryPanel invoiceHistoryPanel;
    private WarrantyPanel warrantyPanel;
    private AnalyticsPanel analyticsPanel;
    private SettingsPanel settingsPanel;

    public MainFrame() {
        super("BillPro Electronics - POS & Inventory Billing System");
        this.dataStore = DataStore.getInstance();
        this.billingService = new BillingService();
        this.inventoryService = new InventoryService();
        this.warrantyService = new WarrantyService();

        initUI();
        updateForCurrentUser();
        startClock();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 800);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.COLOR_BG);
        setLayout(new BorderLayout());

        // Header Panel
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_REGULAR_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        billingPanel = new BillingPanel(this, billingService, inventoryService);
        inventoryPanel = new InventoryPanel(this, inventoryService);
        invoiceHistoryPanel = new InvoiceHistoryPanel(this);
        warrantyPanel = new WarrantyPanel(warrantyService);
        analyticsPanel = new AnalyticsPanel();
        settingsPanel = new SettingsPanel(this::onSettingsUpdated);

        tabbedPane.addChangeListener(e -> {
            int selected = tabbedPane.getSelectedIndex();
            if (selected < 0) return;
            String title = tabbedPane.getTitleAt(selected);
            if (title.contains("Billing")) {
                billingPanel.refreshProductList();
                billingPanel.updateCartTable();
            } else if (title.contains("Inventory")) {
                inventoryPanel.refreshTable();
            } else if (title.contains("Sales")) {
                invoiceHistoryPanel.refreshTable();
            } else if (title.contains("Warranty")) {
                warrantyPanel.refreshTable();
            } else if (title.contains("Analytics")) {
                analyticsPanel.refreshAnalytics();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBackground(UITheme.COLOR_PRIMARY_DARK);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        ShopSettings s = dataStore.getSettings();

        JPanel left = new JPanel(new GridLayout(2, 1, 2, 2));
        left.setBackground(UITheme.COLOR_PRIMARY_DARK);

        lblStoreTitle = new JLabel(s.getStoreName().toUpperCase());
        lblStoreTitle.setFont(UITheme.FONT_TITLE);
        lblStoreTitle.setForeground(Color.WHITE);

        lblStoreSubtitle = new JLabel(s.getTagline() + "  |  GSTIN: " + s.getGstin());
        lblStoreSubtitle.setFont(UITheme.FONT_SMALL);
        lblStoreSubtitle.setForeground(new Color(203, 213, 225));

        left.add(lblStoreTitle);
        left.add(lblStoreSubtitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        right.setBackground(UITheme.COLOR_PRIMARY_DARK);

        lblClock = new JLabel();
        lblClock.setFont(UITheme.FONT_REGULAR_BOLD);
        lblClock.setForeground(new Color(241, 245, 249));

        lblUserBadge = new JLabel("\uD83D\uDC64 Staff");
        lblUserBadge.setFont(UITheme.FONT_REGULAR_BOLD);
        lblUserBadge.setForeground(new Color(191, 219, 254));

        btnLogout = UITheme.createButton("Logout", new Color(220, 38, 38), Color.WHITE);
        btnLogout.setPreferredSize(new Dimension(85, 28));
        btnLogout.setFont(UITheme.FONT_SMALL);
        btnLogout.addActionListener(e -> handleLogout());

        right.add(lblClock);
        right.add(lblUserBadge);
        right.add(btnLogout);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    public void updateForCurrentUser() {
        com.electro.model.User user = com.electro.service.AuthService.getInstance().getCurrentUser();
        if (user != null) {
            lblUserBadge.setText(user.getFullName() + " [" + user.getRole() + "]");
        } else {
            lblUserBadge.setText("\uD83D\uDC64 Guest");
        }

        tabbedPane.removeAll();
        tabbedPane.addTab("  Billing POS  ", billingPanel);
        tabbedPane.addTab("  Inventory & Stock  ", inventoryPanel);
        tabbedPane.addTab("  Sales Invoices  ", invoiceHistoryPanel);
        tabbedPane.addTab("  Warranty & Serials  ", warrantyPanel);

        if (user != null && user.isAdmin()) {
            tabbedPane.addTab("  Analytics  ", analyticsPanel);
            tabbedPane.addTab("  Shop Settings  ", settingsPanel);
        }

        tabbedPane.setSelectedIndex(0);
        billingPanel.refreshProductList();
        billingPanel.updateCartTable();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out from the POS terminal?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            com.electro.service.AuthService.getInstance().logout();
            setVisible(false);

            LoginDialog loginDlg = new LoginDialog(null);
            loginDlg.setVisible(true);

            if (loginDlg.isAuthenticated()) {
                updateForCurrentUser();
                setVisible(true);
            } else {
                System.exit(0);
            }
        }
    }

    private void startClock() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss");
        Timer timer = new Timer(1000, e -> {
            lblClock.setText(LocalDateTime.now().format(dtf));
        });
        timer.setInitialDelay(0);
        timer.start();
    }

    private void onSettingsUpdated() {
        ShopSettings s = dataStore.getSettings();
        lblStoreTitle.setText(s.getStoreName().toUpperCase());
        lblStoreSubtitle.setText(s.getTagline() + "  |  GSTIN: " + s.getGstin());
        setTitle(s.getStoreName() + " - POS & Inventory Billing System");

        // Refresh user badge in case admin display name changed
        com.electro.model.User user = com.electro.service.AuthService.getInstance().getCurrentUser();
        if (user != null) {
            lblUserBadge.setText(user.getFullName() + " [" + user.getRole() + "]");
        }

        billingPanel.refreshProductList();
        billingPanel.updateCartTable();
        inventoryPanel.refreshTable();
        invoiceHistoryPanel.refreshTable();
    }
}
