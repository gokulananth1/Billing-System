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
import java.awt.geom.Path2D;

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

        UITheme.applyAppIcon(this);

        // Header Panel
        JPanel header = new JPanel(new GridLayout(2, 1, 4, 4));
        header.setBackground(UITheme.COLOR_PRIMARY_DARK);
        header.setBorder(new EmptyBorder(22, 24, 22, 24));

        ImageIcon logoIcon = UITheme.getAppLogoIcon(32, 32);
        JLabel title;
        if (logoIcon != null) {
            title = new JLabel(DataStore.getInstance().getSettings().getStoreName(), logoIcon, SwingConstants.CENTER);
            title.setIconTextGap(10);
        } else {
            title = new JLabel(DataStore.getInstance().getSettings().getStoreName(), SwingConstants.CENTER);
        }
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Administrator & POS Terminal Authentication", SwingConstants.CENTER);
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

        JPanel passContainer = new JPanel(new BorderLayout(0, 0));
        passContainer.setBackground(Color.WHITE);
        passContainer.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(0, 2, 0, 4)
        ));
        passContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        passContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        tfPassword = new JPasswordField(15);
        tfPassword.setFont(UITheme.FONT_REGULAR);
        tfPassword.setBorder(new EmptyBorder(6, 8, 6, 8));
        tfPassword.setBackground(Color.WHITE);

        char defaultEcho = tfPassword.getEchoChar();

        JButton btnToggleEye = new JButton(new EyeIcon(false));
        btnToggleEye.setToolTipText("Show password");
        btnToggleEye.setFocusPainted(false);
        btnToggleEye.setBorderPainted(false);
        btnToggleEye.setContentAreaFilled(false);
        btnToggleEye.setOpaque(false);
        btnToggleEye.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleEye.setPreferredSize(new Dimension(32, 32));

        btnToggleEye.addActionListener(e -> {
            if (tfPassword.getEchoChar() != (char) 0) {
                tfPassword.setEchoChar((char) 0);
                btnToggleEye.setIcon(new EyeIcon(true));
                btnToggleEye.setToolTipText("Hide password");
            } else {
                tfPassword.setEchoChar(defaultEcho);
                btnToggleEye.setIcon(new EyeIcon(false));
                btnToggleEye.setToolTipText("Show password");
            }
            tfPassword.requestFocusInWindow();
        });

        passContainer.add(tfPassword, BorderLayout.CENTER);
        passContainer.add(btnToggleEye, BorderLayout.EAST);

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

        JButton btnFillOwner = new JButton("Store Owner");
        styleChipButton(btnFillOwner, new Color(243, 232, 255), new Color(126, 34, 206));
        btnFillOwner.addActionListener(e -> {
            tfUsername.setText("owner");
            tfPassword.setText("owner123");
            lblError.setText(" ");
        });

        demoPanel.add(lblQuick);
        demoPanel.add(btnFillOwner);

        formCard.add(lblUser);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(tfUsername);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(lblPass);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(passContainer);
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
        SwingUtilities.invokeLater(() -> {
            tfUsername.selectAll();
            tfUsername.requestFocusInWindow();
        });
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

    public static class EyeIcon implements Icon {
        private final boolean show;
        private final int size = 18;

        public EyeIcon(boolean show) {
            this.show = show;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color color = show ? UITheme.COLOR_PRIMARY : new Color(148, 163, 184);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = x + size / 2;
            int cy = y + size / 2;

            Path2D p = new Path2D.Double();
            p.moveTo(x + 2, cy);
            p.quadTo(cx, cy - 6, x + size - 2, cy);
            p.quadTo(cx, cy + 6, x + 2, cy);
            g2.draw(p);

            int r = 3;
            if (show) {
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            } else {
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 3, y + size - 3, x + size - 3, y + 3);
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
