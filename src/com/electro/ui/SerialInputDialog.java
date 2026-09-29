package com.electro.ui;

import com.electro.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog prompting the cashier to enter or scan Serial / IMEI numbers
 * for electronics items before checkout.
 */
public class SerialInputDialog extends JDialog {
    private final List<JTextField> serialFields = new ArrayList<>();
    private List<String> confirmedSerials = null;
    private final Product product;
    private final int quantity;

    public SerialInputDialog(Window owner, Product product, int quantity, List<String> existingSerials) {
        super(owner, "Enter Serial / IMEI Numbers", ModalityType.APPLICATION_MODAL);
        this.product = product;
        this.quantity = quantity;

        initUI(existingSerials);
    }

    private void initUI(List<String> existingSerials) {
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(UITheme.COLOR_BG);
        setSize(480, 200 + (quantity * 45));
        setLocationRelativeTo(getOwner());
        UITheme.applyAppIcon(this);

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        headerPanel.setBackground(UITheme.COLOR_PRIMARY);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("Serial / IMEI Registration (" + quantity + " Required)");
        titleLabel.setFont(UITheme.FONT_SUBTITLE);
        titleLabel.setForeground(Color.WHITE);

        JLabel itemLabel = new JLabel(product.getBrand() + " " + product.getName() + " [Model: " + product.getModelNumber() + "]");
        itemLabel.setFont(UITheme.FONT_REGULAR);
        itemLabel.setForeground(new Color(224, 231, 255));

        headerPanel.add(titleLabel);
        headerPanel.add(itemLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Inputs
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(UITheme.COLOR_BG);
        fieldsPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        for (int i = 0; i < quantity; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 5));
            row.setBackground(UITheme.COLOR_BG);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            JLabel lbl = new JLabel("Unit #" + (i + 1) + " S/N or IMEI:");
            lbl.setFont(UITheme.FONT_REGULAR_BOLD);
            lbl.setPreferredSize(new Dimension(150, 30));

            JTextField tf = UITheme.createTextField(20);
            if (existingSerials != null && i < existingSerials.size()) {
                tf.setText(existingSerials.get(i));
            }
            serialFields.add(tf);

            row.add(lbl, BorderLayout.WEST);
            row.add(tf, BorderLayout.CENTER);
            fieldsPanel.add(row);
            fieldsPanel.add(Box.createVerticalStrut(8));
        }

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnAuto = UITheme.createButton("Auto-Generate", new Color(147, 51, 234), Color.WHITE);
        btnAuto.setToolTipText("Generate sample valid serial numbers for fast testing");
        btnAuto.addActionListener(e -> autoGenerateSerials());

        JButton btnCancel = UITheme.createButton("Cancel", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> {
            confirmedSerials = null;
            dispose();
        });

        JButton btnSave = UITheme.createButton("Confirm Serials", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnSave.addActionListener(e -> validateAndSave());

        footerPanel.add(btnAuto);
        footerPanel.add(btnCancel);
        footerPanel.add(btnSave);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void autoGenerateSerials() {
        String prefix = (product.getBrand().substring(0, Math.min(3, product.getBrand().length())).toUpperCase());
        long baseNum = System.currentTimeMillis() % 1000000;
        for (int i = 0; i < serialFields.size(); i++) {
            serialFields.get(i).setText("SN-" + prefix + "-" + (baseNum + i));
        }
    }

    private void validateAndSave() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < serialFields.size(); i++) {
            String val = serialFields.get(i).getText().trim();
            if (val.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter Serial/IMEI for Unit #" + (i + 1), "Missing Serial Number", JOptionPane.WARNING_MESSAGE);
                serialFields.get(i).requestFocus();
                return;
            }
            if (list.contains(val.toUpperCase())) {
                JOptionPane.showMessageDialog(this, "Duplicate Serial Number entered: " + val, "Duplicate Serial", JOptionPane.WARNING_MESSAGE);
                return;
            }
            list.add(val.toUpperCase());
        }

        this.confirmedSerials = list;
        dispose();
    }

    public List<String> getConfirmedSerials() {
        return confirmedSerials;
    }
}
