package com.electro.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * Modern design system and UI constants for the Electronics Billing System.
 */
public class UITheme {

    // Palette
    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);     // Modern Blue
    public static final Color COLOR_PRIMARY_DARK = new Color(30, 58, 138); // Deep Navy
    public static final Color COLOR_SECONDARY = new Color(71, 85, 105);   // Slate 600
    public static final Color COLOR_SUCCESS = new Color(16, 185, 129);    // Emerald
    public static final Color COLOR_DANGER = new Color(239, 68, 68);      // Red
    public static final Color COLOR_WARNING = new Color(245, 158, 11);    // Amber
    public static final Color COLOR_BG = new Color(248, 250, 252);        // Slate 50
    public static final Color COLOR_PANEL_BG = Color.WHITE;
    public static final Color COLOR_BORDER = new Color(226, 232, 240);    // Slate 200
    public static final Color COLOR_TEXT_PRIMARY = new Color(15, 23, 42); // Slate 900
    public static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // Slate 500
    public static final Color COLOR_ACCENT = new Color(238, 242, 255);    // Indigo light tint

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_REGULAR_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BIG_NUMBER = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    public static JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_REGULAR_BOLD);
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static JTextField createTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_REGULAR);
        tf.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        return tf;
    }

    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(COLOR_PANEL_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return card;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(32);
        table.setGridColor(COLOR_BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(224, 231, 255));
        table.setSelectionForeground(COLOR_TEXT_PRIMARY);

        table.getTableHeader().setFont(FONT_REGULAR_BOLD);
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(COLOR_TEXT_PRIMARY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
    }

    public static String formatCurrency(double amount, String symbol) {
        return symbol + String.format("%,.2f", amount);
    }

    private static Image cachedAppIcon = null;

    public static Image getAppIcon() {
        if (cachedAppIcon != null) return cachedAppIcon;
        try {
            File f = new File("assets/app_logo.jpg");
            if (f.exists()) {
                cachedAppIcon = ImageIO.read(f);
                return cachedAppIcon;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void applyAppIcon(Window window) {
        if (window == null) return;
        Image icon = getAppIcon();
        if (icon != null) {
            window.setIconImage(icon);
        }
    }

    public static ImageIcon getAppLogoIcon(int width, int height) {
        Image img = getAppIcon();
        if (img != null) {
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return null;
    }
}
