package com.electro.ui;

import com.electro.model.CartItem;
import com.electro.model.Invoice;
import com.electro.model.Product;
import com.electro.service.DataStore;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Analytics and KPI dashboard for store managers.
 * Features live Month Profit tracking, profit margins, monthly P&L statements,
 * category breakdowns, and top-selling product metrics.
 */
public class AnalyticsPanel extends JPanel {
    private final DataStore dataStore;

    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter SHORT_MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    // KPI Labels
    private JLabel lblMonthProfitTitle;
    private JLabel lblMonthProfit;
    private JLabel lblMonthProfitHint;

    private JLabel lblMonthRevenueTitle;
    private JLabel lblMonthRevenue;
    private JLabel lblMonthRevenueHint;

    private JLabel lblTotalRevenue;
    private JLabel lblTotalRevenueHint;

    private JLabel lblTotalProfit;
    private JLabel lblTotalProfitHint;

    private JLabel lblLowStockCount;
    private JLabel lblLowStockHint;

    // Filter
    private JComboBox<String> cbPeriod;
    private boolean isUpdatingPeriodCombo = false;

    // Tables
    private JTable categoryTable;
    private DefaultTableModel categoryTableModel;

    private JTable topProductsTable;
    private DefaultTableModel topProductsTableModel;

    private JTable monthlyProfitTable;
    private DefaultTableModel monthlyProfitTableModel;

    public AnalyticsPanel() {
        this.dataStore = DataStore.getInstance();

        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.COLOR_BG);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.setBackground(UITheme.COLOR_BG);
        topContainer.add(createTopBar(), BorderLayout.NORTH);
        topContainer.add(createKpiHeader(), BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_REGULAR_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("  Category & Product Breakdown  ", createBreakdownTab());
        tabbedPane.addTab("  Monthly Profit & Loss Statement  ", createMonthlyProfitTab());

        add(tabbedPane, BorderLayout.CENTER);
        refreshAnalytics();
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setBackground(UITheme.COLOR_PANEL_BG);
        bar.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel title = new JLabel("Sales Performance & Business Analytics");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY_DARK);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setBackground(UITheme.COLOR_PANEL_BG);

        JLabel lblFilter = new JLabel("Filter Period:");
        lblFilter.setFont(UITheme.FONT_REGULAR_BOLD);
        lblFilter.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        cbPeriod = new JComboBox<>();
        cbPeriod.setFont(UITheme.FONT_REGULAR);
        cbPeriod.setPreferredSize(new Dimension(220, 32));
        cbPeriod.addActionListener(e -> {
            if (!isUpdatingPeriodCombo) {
                applyPeriodFilter();
            }
        });

        JButton btnResetSales = UITheme.createButton("Reset Sales Data", UITheme.COLOR_DANGER, Color.WHITE);
        btnResetSales.setPreferredSize(new Dimension(140, 32));
        btnResetSales.setFont(UITheme.FONT_SMALL);
        btnResetSales.setToolTipText("Clear all sales history, revenue records, and invoices");
        btnResetSales.addActionListener(e -> handleResetSalesData());

        controls.add(lblFilter);
        controls.add(cbPeriod);
        controls.add(btnResetSales);

        bar.add(title, BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    private void handleResetSalesData() {
        com.electro.model.User user = com.electro.service.AuthService.getInstance().getCurrentUser();
        if (user != null && !user.isAdmin() && !user.isStoreOwner()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators and Store Owners can reset sales data.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently clear all sales data and invoice history?\n"
                + "This will delete all past transactions and reset sales revenue to zero.\n\n"
                + "Do you wish to proceed?",
                "Confirm Reset Sales Data",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dataStore.resetSalesData();
            refreshAnalytics();
            JOptionPane.showMessageDialog(this, "All sales data and transaction history have been reset!", "Sales Data Reset", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel createKpiHeader() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 10));
        panel.setBackground(UITheme.COLOR_BG);

        // 1. Month Profit Card
        lblMonthProfitTitle = new JLabel("THIS MONTH PROFIT");
        lblMonthProfit = new JLabel("0.00");
        lblMonthProfitHint = new JLabel("Gross profit this month");
        panel.add(createKpiCard(lblMonthProfitTitle, lblMonthProfit, UITheme.COLOR_SUCCESS, lblMonthProfitHint));

        // 2. Month Revenue Card
        lblMonthRevenueTitle = new JLabel("THIS MONTH REVENUE");
        lblMonthRevenue = new JLabel("0.00");
        lblMonthRevenueHint = new JLabel("Current month sales");
        panel.add(createKpiCard(lblMonthRevenueTitle, lblMonthRevenue, new Color(79, 70, 229), lblMonthRevenueHint));

        // 3. All-time Revenue Card
        JLabel lblRevTitle = new JLabel("ALL-TIME REVENUE");
        lblTotalRevenue = new JLabel("0.00");
        lblTotalRevenueHint = new JLabel("All-time completed sales");
        panel.add(createKpiCard(lblRevTitle, lblTotalRevenue, UITheme.COLOR_PRIMARY, lblTotalRevenueHint));

        // 4. All-time Profit Card
        JLabel lblProfitTitle = new JLabel("ALL-TIME PROFIT");
        lblTotalProfit = new JLabel("0.00");
        lblTotalProfitHint = new JLabel("Cumulative net margin");
        panel.add(createKpiCard(lblProfitTitle, lblTotalProfit, new Color(13, 148, 136), lblTotalProfitHint));

        // 5. Low Stock Alert Card
        JLabel lblStockTitle = new JLabel("LOW STOCK ITEMS");
        lblLowStockCount = new JLabel("0");
        lblLowStockHint = new JLabel("Products with <= 3 stock");
        panel.add(createKpiCard(lblStockTitle, lblLowStockCount, UITheme.COLOR_DANGER, lblLowStockHint));

        return panel;
    }

    private JPanel createKpiCard(JLabel titleLabel, JLabel valueLabel, Color accentColor, JLabel hintLabel) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(UITheme.COLOR_PANEL_BG);
        card.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                new CompoundBorder(
                        new LineBorder(UITheme.COLOR_BORDER, 1, true),
                        new EmptyBorder(12, 14, 12, 14)
                )
        ));

        titleLabel.setFont(UITheme.FONT_SMALL);
        titleLabel.setForeground(UITheme.COLOR_TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 21));
        valueLabel.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        hintLabel.setFont(UITheme.FONT_SMALL);
        hintLabel.setForeground(UITheme.COLOR_TEXT_MUTED);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(hintLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createBreakdownTab() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        centerPanel.setBackground(UITheme.COLOR_BG);
        centerPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        centerPanel.add(createCategoryBreakdownPanel());
        centerPanel.add(createTopSellingProductsPanel());
        return centerPanel;
    }

    private JPanel createMonthlyProfitTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        // Header info banner
        JPanel headerPanel = new JPanel(new BorderLayout(6, 4));
        headerPanel.setBackground(new Color(240, 253, 244));
        headerPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(187, 247, 208), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel title = new JLabel("Historical Monthly Profit & Sales Statement");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(new Color(22, 101, 52));

        JLabel subtitle = new JLabel("Gross Profit = Taxable Sales Revenue - Cost of Goods Sold (COGS). Tax (GST) is collected separately on behalf of tax authorities.");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(new Color(21, 128, 61));

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);

        String[] cols = {"Month / Period", "Invoices", "Units Sold", "Taxable Sales", "Cost of Goods (COGS)", "Gross Profit", "Profit Margin", "Total Billed (Inc. GST)"};
        monthlyProfitTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        monthlyProfitTable = new JTable(monthlyProfitTableModel);
        UITheme.styleTable(monthlyProfitTable);

        // Align numeric columns right
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < cols.length; i++) {
            monthlyProfitTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        // Custom renderer for Gross Profit column to highlight green/red
        monthlyProfitTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(SwingConstants.RIGHT);
                c.setFont(UITheme.FONT_REGULAR_BOLD);
                String s = String.valueOf(value);
                if (s.contains("-")) {
                    c.setForeground(UITheme.COLOR_DANGER);
                } else {
                    c.setForeground(new Color(5, 150, 105));
                }
                return c;
            }
        });

        // Set column widths
        monthlyProfitTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        monthlyProfitTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        monthlyProfitTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        monthlyProfitTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        monthlyProfitTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        monthlyProfitTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        monthlyProfitTable.getColumnModel().getColumn(6).setPreferredWidth(95);
        monthlyProfitTable.getColumnModel().getColumn(7).setPreferredWidth(130);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(monthlyProfitTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCategoryBreakdownPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel("Sales by Category");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY_DARK);

        String[] cols = {"Category", "Units Sold", "Total Revenue"};
        categoryTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        categoryTable = new JTable(categoryTableModel);
        UITheme.styleTable(categoryTable);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(categoryTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTopSellingProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel("Top Selling Electronics");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY_DARK);

        String[] cols = {"Product Name", "Brand", "Units Sold", "Total Sales"};
        topProductsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        topProductsTable = new JTable(topProductsTableModel);
        UITheme.styleTable(topProductsTable);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(topProductsTable), BorderLayout.CENTER);
        return panel;
    }

    public static class MonthStats {
        public double revenue = 0.0;    // Grand total
        public double taxable = 0.0;    // Taxable sales
        public double cost = 0.0;       // COGS
        public double profit = 0.0;     // Taxable - COGS
        public int invoices = 0;
        public int units = 0;

        public double getMarginPercent() {
            return taxable > 0 ? (profit / taxable) * 100.0 : 0.0;
        }
    }

    private YearMonth getInvoiceYearMonth(Invoice inv) {
        if (inv == null || inv.getDateTime() == null || inv.getDateTime().trim().length() < 7) {
            return YearMonth.now();
        }
        try {
            String ymStr = inv.getDateTime().trim().substring(0, 7);
            return YearMonth.parse(ymStr);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    public void refreshAnalytics() {
        List<Invoice> invoices = dataStore.getAllInvoices();
        List<Product> products = dataStore.getAllProducts();
        String sym = dataStore.getSettings().getCurrencySymbol();

        // Group invoices by YearMonth
        Map<YearMonth, MonthStats> monthlyData = new TreeMap<>(Collections.reverseOrder());

        double totalRevenue = 0.0;
        double totalTaxable = 0.0;
        double totalCost = 0.0;
        double totalProfit = 0.0;

        for (Invoice inv : invoices) {
            if (inv.isRefunded()) continue;
            YearMonth ym = getInvoiceYearMonth(inv);
            MonthStats stats = monthlyData.computeIfAbsent(ym, k -> new MonthStats());

            double invCost = 0.0;
            int invUnits = 0;
            for (CartItem item : inv.getItems()) {
                invUnits += item.getQuantity();
                double unitCost = 0.0;
                if (item.getProduct() != null) {
                    unitCost = item.getProduct().getCostPrice();
                }
                if (unitCost <= 0.0 && item.getProduct() != null && item.getProduct().getId() != null) {
                    Product pStore = dataStore.getProductById(item.getProduct().getId());
                    if (pStore != null) {
                        unitCost = pStore.getCostPrice();
                    }
                }
                invCost += unitCost * item.getQuantity();
            }

            double invTaxable = inv.getTaxableAmount();
            double invProfit = invTaxable - invCost;

            stats.revenue += inv.getGrandTotal();
            stats.taxable += invTaxable;
            stats.cost += invCost;
            stats.profit += invProfit;
            stats.invoices++;
            stats.units += invUnits;

            totalRevenue += inv.getGrandTotal();
            totalTaxable += invTaxable;
            totalCost += invCost;
            totalProfit += invProfit;
        }

        // Low stock count
        long lowStock = products.stream().filter(Product::isLowStock).count();
        lblLowStockCount.setText(String.valueOf(lowStock));
        lblLowStockCount.setForeground(lowStock > 0 ? UITheme.COLOR_DANGER : UITheme.COLOR_SUCCESS);
        lblLowStockHint.setText(lowStock > 0 ? lowStock + " items need restock" : "All inventory healthy");

        // All-time revenue & profit
        lblTotalRevenue.setText(UITheme.formatCurrency(totalRevenue, sym));
        lblTotalRevenueHint.setText(invoices.size() + " total bills all-time");

        lblTotalProfit.setText(UITheme.formatCurrency(totalProfit, sym));
        lblTotalProfit.setForeground(totalProfit >= 0 ? new Color(13, 148, 136) : UITheme.COLOR_DANGER);
        double allTimeMargin = totalTaxable > 0 ? (totalProfit / totalTaxable) * 100.0 : 0.0;
        lblTotalProfitHint.setText(String.format("All-time margin: %.1f%%", allTimeMargin));

        // Populate Monthly Profit & Loss Statement Table
        monthlyProfitTableModel.setRowCount(0);
        for (Map.Entry<YearMonth, MonthStats> entry : monthlyData.entrySet()) {
            YearMonth ym = entry.getKey();
            MonthStats s = entry.getValue();
            monthlyProfitTableModel.addRow(new Object[]{
                    ym.format(MONTH_LABEL_FORMAT),
                    s.invoices,
                    s.units,
                    UITheme.formatCurrency(s.taxable, sym),
                    UITheme.formatCurrency(s.cost, sym),
                    UITheme.formatCurrency(s.profit, sym),
                    String.format("%.2f%%", s.getMarginPercent()),
                    UITheme.formatCurrency(s.revenue, sym)
            });
        }

        // Update Period Filter combo box
        YearMonth currentYm = YearMonth.now();
        String currentSelection = (String) cbPeriod.getSelectedItem();

        isUpdatingPeriodCombo = true;
        cbPeriod.removeAllItems();
        cbPeriod.addItem("Current Month (" + currentYm.format(SHORT_MONTH_FORMAT) + ")");
        cbPeriod.addItem("All Time (Cumulative)");

        for (YearMonth ym : monthlyData.keySet()) {
            if (!ym.equals(currentYm)) {
                cbPeriod.addItem(ym.format(MONTH_LABEL_FORMAT));
            }
        }

        if (currentSelection != null) {
            for (int i = 0; i < cbPeriod.getItemCount(); i++) {
                if (cbPeriod.getItemAt(i).equalsIgnoreCase(currentSelection)) {
                    cbPeriod.setSelectedIndex(i);
                    break;
                }
            }
        }
        isUpdatingPeriodCombo = false;

        // Apply period filter to update Month cards, Category and Top Products tables
        applyPeriodFilter();
    }

    private void applyPeriodFilter() {
        List<Invoice> allInvoices = dataStore.getAllInvoices();
        String sym = dataStore.getSettings().getCurrencySymbol();
        YearMonth currentYm = YearMonth.now();

        int selectedIndex = cbPeriod.getSelectedIndex();
        YearMonth filterYm = null;

        if (selectedIndex <= 0) {
            filterYm = currentYm;
        } else if (selectedIndex == 1) {
            filterYm = null; // All Time
        } else {
            String selectedText = (String) cbPeriod.getSelectedItem();
            if (selectedText != null) {
                try {
                    filterYm = YearMonth.parse(selectedText, MONTH_LABEL_FORMAT);
                } catch (Exception e) {
                    filterYm = currentYm;
                }
            }
        }

        // Calculate Month Profit & Revenue for the active month (either selected month or current month)
        YearMonth targetMonth = (filterYm != null) ? filterYm : currentYm;
        double monthProfit = 0.0;
        double monthRevenue = 0.0;
        double monthTaxable = 0.0;
        double monthCost = 0.0;
        int monthInvoices = 0;
        int monthUnits = 0;

        for (Invoice inv : allInvoices) {
            if (inv.isRefunded()) continue;
            if (getInvoiceYearMonth(inv).equals(targetMonth)) {
                monthRevenue += inv.getGrandTotal();
                monthTaxable += inv.getTaxableAmount();
                monthInvoices++;

                for (CartItem item : inv.getItems()) {
                    monthUnits += item.getQuantity();
                    double cp = item.getProduct() != null ? item.getProduct().getCostPrice() : 0.0;
                    if (cp <= 0.0 && item.getProduct() != null && item.getProduct().getId() != null) {
                        Product pStore = dataStore.getProductById(item.getProduct().getId());
                        if (pStore != null) cp = pStore.getCostPrice();
                    }
                    monthCost += cp * item.getQuantity();
                }
            }
        }

        monthProfit = monthTaxable - monthCost;
        double monthMargin = monthTaxable > 0 ? (monthProfit / monthTaxable) * 100.0 : 0.0;

        String monthName = targetMonth.format(SHORT_MONTH_FORMAT).toUpperCase();
        lblMonthProfitTitle.setText("MONTH PROFIT (" + monthName + ")");
        lblMonthProfit.setText(UITheme.formatCurrency(monthProfit, sym));
        lblMonthProfit.setForeground(monthProfit >= 0 ? new Color(5, 150, 105) : UITheme.COLOR_DANGER);
        lblMonthProfitHint.setText(String.format("Margin: %.1f%% | Cost: %s", monthMargin, UITheme.formatCurrency(monthCost, sym)));

        lblMonthRevenueTitle.setText("MONTH REVENUE (" + monthName + ")");
        lblMonthRevenue.setText(UITheme.formatCurrency(monthRevenue, sym));
        lblMonthRevenueHint.setText(monthInvoices + " bills | " + monthUnits + " units sold");

        // Filter invoices for Category & Top Products breakdowns
        List<Invoice> filteredInvoices = new ArrayList<>();
        for (Invoice inv : allInvoices) {
            if (inv.isRefunded()) continue;
            if (filterYm == null || getInvoiceYearMonth(inv).equals(filterYm)) {
                filteredInvoices.add(inv);
            }
        }

        Map<String, Integer> catUnits = new HashMap<>();
        Map<String, Double> catRevenue = new HashMap<>();
        Map<String, Integer> prodUnits = new HashMap<>();
        Map<String, Double> prodRevenue = new HashMap<>();
        Map<String, String> prodBrand = new HashMap<>();

        for (Invoice inv : filteredInvoices) {
            for (CartItem item : inv.getItems()) {
                int q = item.getQuantity();
                double line = item.getLineTotal();

                String cat = item.getProduct() != null ? item.getProduct().getCategory() : "Other";
                catUnits.put(cat, catUnits.getOrDefault(cat, 0) + q);
                catRevenue.put(cat, catRevenue.getOrDefault(cat, 0.0) + line);

                String pName = item.getProduct() != null ? item.getProduct().getName() : "Unknown";
                prodUnits.put(pName, prodUnits.getOrDefault(pName, 0) + q);
                prodRevenue.put(pName, prodRevenue.getOrDefault(pName, 0.0) + line);
                if (item.getProduct() != null) {
                    prodBrand.put(pName, item.getProduct().getBrand());
                }
            }
        }

        // Category Table
        categoryTableModel.setRowCount(0);
        for (String cat : catUnits.keySet()) {
            categoryTableModel.addRow(new Object[]{
                    cat,
                    catUnits.get(cat),
                    UITheme.formatCurrency(catRevenue.get(cat), sym)
            });
        }

        // Top Products Table
        topProductsTableModel.setRowCount(0);
        List<Map.Entry<String, Integer>> sortedProds = new ArrayList<>(prodUnits.entrySet());
        sortedProds.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sortedProds) {
            String pName = entry.getKey();
            topProductsTableModel.addRow(new Object[]{
                    pName,
                    prodBrand.getOrDefault(pName, ""),
                    entry.getValue(),
                    UITheme.formatCurrency(prodRevenue.getOrDefault(pName, 0.0), sym)
            });
        }
    }
}

