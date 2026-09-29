package com.electro.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Modal dialog for configuring split payments across Cash, Card, UPI, and EMI.
 */
public class SplitPaymentDialog extends JDialog {
    private final double targetAmount;
    private final String currencySymbol;

    private JTextField tfCash;
    private JTextField tfCard;
    private JTextField tfUpi;
    private JTextField tfEmi;

    private JLabel lblTarget;
    private JLabel lblAllocated;
    private JLabel lblRemaining;
    private JButton btnApply;

    private boolean confirmed = false;
    private String splitSummary = "";

    public SplitPaymentDialog(Window owner, double targetAmount, String currencySymbol) {
        super(owner, "Configure Split Payment", ModalityType.APPLICATION_MODAL);
        this.targetAmount = Math.max(0.0, targetAmount);
        this.currencySymbol = (currencySymbol != null && !currencySymbol.isEmpty()) ? currencySymbol : "\u20B9";

        initUI();
    }

    private void initUI() {
        setSize(460, 480);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setLayout(new BorderLayout(0, 0));
        UITheme.applyAppIcon(this);

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 4, 4));
        header.setBackground(UITheme.COLOR_PRIMARY_DARK);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Split Payment Allocation");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(Color.WHITE);

        lblTarget = new JLabel("Bill Grand Total: " + UITheme.formatCurrency(targetAmount, currencySymbol));
        lblTarget.setFont(UITheme.FONT_REGULAR_BOLD);
        lblTarget.setForeground(new Color(191, 219, 254));

        header.add(title);
        header.add(lblTarget);
        add(header, BorderLayout.NORTH);

        // Inputs Panel
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UITheme.COLOR_PANEL_BG);
        center.setBorder(new EmptyBorder(16, 20, 12, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        tfCash = UITheme.createTextField(12);
        tfCard = UITheme.createTextField(12);
        tfUpi = UITheme.createTextField(12);
        tfEmi = UITheme.createTextField(12);

        tfCash.setText("0.00");
        tfCard.setText("0.00");
        tfUpi.setText("0.00");
        tfEmi.setText("0.00");

        KeyAdapter recalculateListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalculate();
            }
        };

        tfCash.addKeyListener(recalculateListener);
        tfCard.addKeyListener(recalculateListener);
        tfUpi.addKeyListener(recalculateListener);
        tfEmi.addKeyListener(recalculateListener);

        // Row 0: Cash
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        JLabel lblCash = new JLabel("Cash Amount (" + currencySymbol + "):");
        lblCash.setFont(UITheme.FONT_REGULAR_BOLD);
        center.add(lblCash, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        center.add(tfCash, gbc);

        // Row 1: Card
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.35;
        JLabel lblCard = new JLabel("Card Amount (" + currencySymbol + "):");
        lblCard.setFont(UITheme.FONT_REGULAR_BOLD);
        center.add(lblCard, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        center.add(tfCard, gbc);

        // Row 2: UPI
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.35;
        JLabel lblUpi = new JLabel("UPI / QR (" + currencySymbol + "):");
        lblUpi.setFont(UITheme.FONT_REGULAR_BOLD);
        center.add(lblUpi, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        center.add(tfUpi, gbc);

        // Row 3: EMI / Finance
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.35;
        JLabel lblEmi = new JLabel("EMI / Finance (" + currencySymbol + "):");
        lblEmi.setFont(UITheme.FONT_REGULAR_BOLD);
        center.add(lblEmi, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        center.add(tfEmi, gbc);

        // Row 4: Quick Fill Buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel quickBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        quickBar.setBackground(UITheme.COLOR_PANEL_BG);

        JButton btnFillCash = UITheme.createButton("Fill Remaining with Cash", new Color(13, 148, 136), Color.WHITE);
        btnFillCash.setFont(UITheme.FONT_SMALL);
        btnFillCash.addActionListener(e -> fillRemaining(tfCash));

        JButton btnFillUpi = UITheme.createButton("Fill Remaining with UPI", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnFillUpi.setFont(UITheme.FONT_SMALL);
        btnFillUpi.addActionListener(e -> fillRemaining(tfUpi));

        quickBar.add(btnFillCash);
        quickBar.add(btnFillUpi);
        center.add(quickBar, gbc);

        // Row 5: Summary Card
        gbc.gridy = 5;
        JPanel summaryCard = new JPanel(new GridLayout(2, 1, 4, 4));
        summaryCard.setBackground(new Color(241, 245, 249));
        summaryCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        lblAllocated = new JLabel("Total Allocated: " + currencySymbol + "0.00");
        lblAllocated.setFont(UITheme.FONT_REGULAR_BOLD);

        lblRemaining = new JLabel("Remaining Due: " + UITheme.formatCurrency(targetAmount, currencySymbol));
        lblRemaining.setFont(UITheme.FONT_REGULAR_BOLD);
        lblRemaining.setForeground(UITheme.COLOR_DANGER);

        summaryCard.add(lblAllocated);
        summaryCard.add(lblRemaining);
        center.add(summaryCard, gbc);

        add(center, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UITheme.COLOR_PANEL_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnCancel = UITheme.createButton("Cancel", new Color(148, 163, 184), Color.WHITE);
        btnCancel.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        btnApply = UITheme.createButton("Apply Split Payment", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnApply.setEnabled(false);
        btnApply.addActionListener(e -> applySplit());

        footer.add(btnCancel);
        footer.add(btnApply);
        add(footer, BorderLayout.SOUTH);

        // Auto-fill initial cash with target amount for convenience
        tfCash.setText(String.format("%.2f", targetAmount));
        recalculate();
    }

    private double parseVal(JTextField tf) {
        try {
            double v = Double.parseDouble(tf.getText().trim());
            return Math.max(0.0, v);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void fillRemaining(JTextField targetTf) {
        double currentTotal = parseVal(tfCash) + parseVal(tfCard) + parseVal(tfUpi) + parseVal(tfEmi);
        double thisVal = parseVal(targetTf);
        double otherTotals = currentTotal - thisVal;
        double diff = targetAmount - otherTotals;
        if (diff < 0) diff = 0;
        targetTf.setText(String.format("%.2f", diff));
        recalculate();
    }

    private void recalculate() {
        double cash = parseVal(tfCash);
        double card = parseVal(tfCard);
        double upi = parseVal(tfUpi);
        double emi = parseVal(tfEmi);

        double allocated = cash + card + upi + emi;
        double remaining = targetAmount - allocated;

        lblAllocated.setText("Total Allocated: " + UITheme.formatCurrency(allocated, currencySymbol));

        if (Math.abs(remaining) < 0.01) {
            lblRemaining.setText("Remaining Due: " + currencySymbol + "0.00 (Balanced \u2714)");
            lblRemaining.setForeground(UITheme.COLOR_SUCCESS);
            btnApply.setEnabled(true);
        } else if (remaining > 0) {
            lblRemaining.setText("Remaining Due: " + UITheme.formatCurrency(remaining, currencySymbol) + " (Under-allocated)");
            lblRemaining.setForeground(UITheme.COLOR_DANGER);
            btnApply.setEnabled(false);
        } else {
            lblRemaining.setText("Over-allocated: " + UITheme.formatCurrency(-remaining, currencySymbol) + " (Exceeds Total)");
            lblRemaining.setForeground(UITheme.COLOR_DANGER);
            btnApply.setEnabled(false);
        }
    }

    private void applySplit() {
        double cash = parseVal(tfCash);
        double card = parseVal(tfCard);
        double upi = parseVal(tfUpi);
        double emi = parseVal(tfEmi);

        StringBuilder sb = new StringBuilder();
        if (cash > 0) sb.append("Cash: ").append(UITheme.formatCurrency(cash, currencySymbol)).append(" | ");
        if (card > 0) sb.append("Card: ").append(UITheme.formatCurrency(card, currencySymbol)).append(" | ");
        if (upi > 0) sb.append("UPI: ").append(UITheme.formatCurrency(upi, currencySymbol)).append(" | ");
        if (emi > 0) sb.append("EMI: ").append(UITheme.formatCurrency(emi, currencySymbol)).append(" | ");

        String res = sb.toString();
        if (res.endsWith(" | ")) {
            res = res.substring(0, res.length() - 3);
        }
        this.splitSummary = res;
        this.confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getSplitSummary() {
        return splitSummary;
    }
}
