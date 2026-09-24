package com.electro.ui;

import com.electro.service.AuthService;
import com.electro.service.DataStore;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Modern login authentication dialog for cashier and administrator sign-in.
 * The store name shown in the header is read live from DataStore so it always
 * matches whatever the admin has configured in Shop Settings.
 */
public class LoginDialog extends JDialog {
    private final AuthService authService;
    private JTextField tfUsername;
    private JPasswordField tfPassword;
    private JLabel lblError;
    private boolean authenticated = false;

    public LoginDialog(Frame parent) {
        super(parent, DataStore.getInstance().getSettings().getStoreName() + " - Terminal Login", true);
        this.authService = AuthService.getInstance();

        initUI();
    }

    private void initUI() {
        setSize(440, 480);
        setResizable(false);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.COLOR_BG);

        // Header Panel
        JPanel header = new JPanel(new GridLayout(2, 1, 4, 4));
        header.setBackground(UITheme.COLOR_PRIMARY_DARK);
        header.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel title = new JLabel(DataStore.getInstance().getSettings().getStoreName(), SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Staff & POS Terminal Authentication", SwingConstants.CENTER);
        subtitle.setFont(UITheme.FONT_REGULAR);
        subtitle.setForeground(new Color(203, 213, 225));

        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // Center Card Panel
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(UITheme.COLOR_BG);
        centerWrapper.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBackground(UITheme.COLOR_PANEL_BG);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Username
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(UITheme.FONT_REGULAR_BOLD);
        lblUser.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        tfUsername = UITheme.createTextField(15);
        tfUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tfUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(UITheme.FONT_REGULAR_BOLD);
        lblPass.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        tfPassword = new JPasswordField(15);
        tfPassword.setFont(UITheme.FONT_REGULAR);
        tfPassword.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        tfPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tfPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Enter key listeners
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        tfUsername.addKeyListener(enterListener);
        tfPassword.addKeyListener(enterListener);

        // Error message
        lblError = new JLabel(" ");
        lblError.setFont(UITheme.FONT_SMALL);
        lblError.setForeground(UITheme.COLOR_DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Quick demo login shortcuts
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        demoPanel.setBackground(UITheme.COLOR_PANEL_BG);
        demoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblQuick = new JLabel("Quick Fill:");
        lblQuick.setFont(UITheme.FONT_SMALL);
        lblQuick.setForeground(UITheme.COLOR_TEXT_MUTED);

        JButton btnFillAdmin = new JButton("Admin");
        styleChipButton(btnFillAdmin, new Color(238, 242, 255), UITheme.COLOR_PRIMARY);
        btnFillAdmin.addActionListener(e -> {
            tfUsername.setText("admin");
            tfPassword.setText("admin123");
            lblError.setText(" ");
        });

        JButton btnFillCashier = new JButton("Cashier");
        styleChipButton(btnFillCashier, new Color(240, 253, 244), UITheme.COLOR_SUCCESS);
        btnFillCashier.addActionListener(e -> {
            tfUsername.setText("cashier");
            tfPassword.setText("cashier123");
            lblError.setText(" ");
        });

        demoPanel.add(lblQuick);
        demoPanel.add(btnFillAdmin);
        demoPanel.add(btnFillCashier);

        formCard.add(lblUser);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(tfUsername);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(lblPass);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(tfPassword);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(lblError);
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(demoPanel);

        centerWrapper.add(formCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footer.setBackground(UITheme.COLOR_PANEL_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnExit = UITheme.createButton("Exit App", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnExit.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        JButton btnLogin = UITheme.createButton("Sign In", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnLogin.setPreferredSize(new Dimension(130, 36));
        btnLogin.addActionListener(e -> performLogin());

        footer.add(btnExit);
        footer.add(btnLogin);
        add(footer, BorderLayout.SOUTH);

        // Default focus
        SwingUtilities.invokeLater(() -> tfUsername.requestFocusInWindow());
    }

    private void styleChipButton(JButton btn, Color bg, Color fg) {
        btn.setFont(UITheme.FONT_SMALL);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(fg, 1, true), new EmptyBorder(2, 8, 2, 8)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void performLogin() {
        String username = tfUsername.getText().trim();
        String password = new String(tfPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Please enter both username and password.");
            return;
        }

        boolean success = authService.login(username, password);
        if (success) {
            authenticated = true;
            dispose();
        } else {
            lblError.setText("Invalid username or password. Please try again.");
            tfPassword.setText("");
            tfPassword.requestFocusInWindow();
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
