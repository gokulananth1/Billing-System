package com.electro;

import com.electro.service.AuthService;
import com.electro.service.DataStore;
import com.electro.ui.LoginDialog;
import com.electro.ui.MainFrame;

import javax.swing.*;

/**
 * Main application launcher for the Electronics Shop Billing System with authentication.
 */
public class Main {
    public static void main(String[] args) {
        // Set Look and Feel to System native for crisp UI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Initialize Services
        DataStore.getInstance();
        AuthService authService = AuthService.getInstance();

        // Directly launch POS terminal logged in as Store Owner
        SwingUtilities.invokeLater(() -> {
            boolean loggedIn = authService.loginAsDefaultOwner();
            if (!loggedIn) {
                // Fallback to login dialog if store owner account unavailable
                LoginDialog loginDlg = new LoginDialog(null);
                loginDlg.setVisible(true);

                if (!loginDlg.isAuthenticated()) {
                    System.exit(0);
                    return;
                }
            }

            MainFrame frame = new MainFrame();
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        });
    }
}
