import javax.swing.*;
import javax.swing.border.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.awt.GradientPaint;

public class SecuritySuite extends JFrame {
    private CardLayout mainCardLayout;
    private JPanel mainCardPanel;
    private LoginPanel loginPanel;
    private JPanel mainAppPanel;
    private boolean darkTheme = false;
    private Preferences prefs;

    // Colors
    private Color PRIMARY_COLOR = new Color(41, 128, 185);
    private Color SECONDARY_COLOR = new Color(52, 152, 219);
    private Color ACCENT_COLOR = new Color(46, 204, 113);
    private Color DANGER_COLOR = new Color(231, 76, 60);
    private Color BACKGROUND_COLOR = new Color(240, 242, 245);
    private Color CARD_COLOR = Color.WHITE;
    private Color TEXT_COLOR = Color.BLACK;

    // Dark theme colors
    private final Color DARK_BACKGROUND = new Color(30, 30, 30);
    private final Color DARK_CARD = new Color(50, 50, 50);
    private final Color DARK_TEXT = Color.WHITE;

    public SecuritySuite() {
        prefs = Preferences.userRoot().node("SecuritySuite");
        loadPreferences();
        initializeUI();
        setupComponents();
        setAppIcon();
    }
    private void setAppIcon() {
        try {
            List<Image> icons = new ArrayList<>();

            // Load from resources (place icon in src folder)
            ImageIcon customIcon = new ImageIcon(getClass().getResource("/my-icon.png"));
            Image baseImage = customIcon.getImage();

            // Create multiple sizes from your custom image
            icons.add(baseImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            icons.add(baseImage.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
            icons.add(baseImage.getScaledInstance(48, 48, Image.SCALE_SMOOTH));
            icons.add(baseImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH));

            setIconImages(icons);

        } catch (Exception e) {
            System.out.println("Error loading custom icon: " + e.getMessage());
            e.printStackTrace(); // This will show you the exact error
            // Fallback to programmatic icon
            createProgrammaticIcons();
        }
    }

    private void createProgrammaticIcons() {
        try {
            List<Image> icons = new ArrayList<>();
            icons.add(createIconImage(16, 16).getImage());
            icons.add(createIconImage(32, 32).getImage());
            icons.add(createIconImage(48, 48).getImage());
            icons.add(createIconImage(64, 64).getImage());
            setIconImages(icons);
        } catch (Exception e) {
            System.out.println("Failed to create programmatic icons");
        }
    }

    private ImageIcon createIconImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw shield background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(41, 128, 185),
                width, height, new Color(52, 152, 219));
        g2d.setPaint(gradient);
        g2d.fillRoundRect(2, 2, width-4, height-4, 8, 8);

        // Draw shield border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(2, 2, width-4, height-4, 8, 8);

        // Draw lock icon inside
        g2d.setColor(Color.WHITE);
        int lockSize = width / 3;
        int lockX = (width - lockSize) / 2;
        int lockY = (height - lockSize) / 2;

        // Lock body
        g2d.fillRect(lockX, lockY + lockSize/3, lockSize, lockSize * 2/3);
        // Lock arch
        g2d.fillArc(lockX - lockSize/6, lockY, lockSize * 4/3, lockSize/2, 0, 180);

        g2d.dispose();
        return new ImageIcon(image);
    }

    private void loadPreferences() {
        darkTheme = prefs.getBoolean("darkTheme", false);
        updateTheme();
    }

    private void savePreferences() {
        prefs.putBoolean("darkTheme", darkTheme);
    }

    private void updateTheme() {
        if (darkTheme) {
            BACKGROUND_COLOR = DARK_BACKGROUND;
            CARD_COLOR = DARK_CARD;
            TEXT_COLOR = DARK_TEXT;
        } else {
            BACKGROUND_COLOR = new Color(240, 242, 245);
            CARD_COLOR = Color.WHITE;
            TEXT_COLOR = Color.BLACK;
        }
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        updateTheme();
        savePreferences();
        refreshUI();
    }

    private void refreshUI() {
        // Recreate the main application panel with new theme
        if (mainAppPanel != null) {
            mainCardPanel.remove(mainAppPanel);
            mainAppPanel = createMainApplicationPanel();
            mainCardPanel.add(mainAppPanel, "main");
            mainCardPanel.revalidate();
            mainCardPanel.repaint();
        }
    }

    private void initializeUI() {
        setTitle("OutCore Security Suite");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        mainCardLayout = new CardLayout();
        mainCardPanel = new JPanel(mainCardLayout);
        mainCardPanel.setBackground(BACKGROUND_COLOR);
        setContentPane(mainCardPanel);
    }

    private void setupComponents() {
        loginPanel = new LoginPanel(this);
        mainAppPanel = createMainApplicationPanel();

        mainCardPanel.add(loginPanel, "login");
        mainCardPanel.add(mainAppPanel, "main");

        showLoginPanel();
    }

    private JPanel createMainApplicationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);

        panel.add(createHeader(), BorderLayout.NORTH);
        panel.add(createMainContent(), BorderLayout.CENTER);
        panel.add(createStatusBar(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        header.setPreferredSize(new Dimension(getWidth(), 70));

        JLabel title = new JLabel("OutCore - Security Suite");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navPanel.setOpaque(false);

        String[] navItems = {"Dashboard", "Password Checker", "Network Scanner", "File Encryption"};
        for (String item : navItems) {
            JButton navButton = createNavButton(item);
            navButton.addActionListener(e -> switchPanel(item));
            navPanel.add(navButton);
        }

        // Theme toggle button
        JButton themeBtn = createNavButton(darkTheme ? "Light Mode" : "Dark Mode");
        themeBtn.addActionListener(e -> {
            toggleTheme();
            themeBtn.setText(darkTheme ? "Light Mode" : "Dark Mode");
        });

        JButton logoutBtn = createNavButton("Logout");
        logoutBtn.addActionListener(e -> logout());

        navPanel.add(themeBtn);
        navPanel.add(logoutBtn);

        header.add(title, BorderLayout.WEST);
        header.add(navPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createMainContent() {
        // Use CardLayout for the main content area
        JPanel contentPanel = new JPanel(new CardLayout());
        contentPanel.setBackground(BACKGROUND_COLOR);

        // Create and add all the panels
        contentPanel.add(createDashboardPanel(), "Dashboard");
        contentPanel.add(createPasswordCheckerPanel(), "Password Checker");
        contentPanel.add(createNetworkScannerPanel(), "Network Scanner");
        contentPanel.add(createFileEncryptionPanel(), "File Encryption");

        return contentPanel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Security Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_COLOR);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel mainPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Create dashboard cards
        mainPanel.add(createDashboardCard("Security Suit", "89.6%", PRIMARY_COLOR));
        mainPanel.add(createDashboardCard("Password Checker", "99%", ACCENT_COLOR));
        mainPanel.add(createDashboardCard("Network Scanning", "80%", SECONDARY_COLOR));
        mainPanel.add(createDashboardCard("Files Encryption", "90%", new Color(155, 89, 182)));

        // Recent activity panel
        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBackground(CARD_COLOR);
        activityPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel activityTitle = new JLabel("Recent Activity");
        activityTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        activityTitle.setForeground(TEXT_COLOR);

        JTextArea activityArea = new JTextArea();
        activityArea.setEditable(false);
        activityArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        activityArea.setBackground(CARD_COLOR);
        activityArea.setForeground(TEXT_COLOR);
        activityArea.setText("• System started successfully\n• Last login: Today\n• No security issues detected\n• All modules operational");
        activityArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        activityPanel.add(activityTitle, BorderLayout.NORTH);
        activityPanel.add(new JScrollPane(activityArea), BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(mainPanel, BorderLayout.CENTER);
        panel.add(activityPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDashboardCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(color, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createPasswordCheckerPanel() {
        // EnhancedPasswordCheckerPanel needs 7 parameters
        return new EnhancedPasswordCheckerPanel(PRIMARY_COLOR, SECONDARY_COLOR, ACCENT_COLOR,
                DANGER_COLOR, BACKGROUND_COLOR, CARD_COLOR, TEXT_COLOR);
    }

    private JPanel createNetworkScannerPanel() {
        // EnhancedNetworkScannerPanel needs 6 parameters
        return new EnhancedNetworkScannerPanel(PRIMARY_COLOR, SECONDARY_COLOR, ACCENT_COLOR,
                DANGER_COLOR, BACKGROUND_COLOR, CARD_COLOR);
    }

    private JPanel createFileEncryptionPanel() {
        // EnhancedFileEncryptionPanel needs 6 parameters
        return new EnhancedFileEncryptionPanel(PRIMARY_COLOR, SECONDARY_COLOR, ACCENT_COLOR,
                DANGER_COLOR, BACKGROUND_COLOR, CARD_COLOR);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(PRIMARY_COLOR.darker());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusBar.setPreferredSize(new Dimension(getWidth(), 25));

        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel versionLabel = new JLabel("Security Suite v2.0");
        versionLabel.setForeground(Color.WHITE);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(versionLabel, BorderLayout.EAST);

        return statusBar;
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(SECONDARY_COLOR);
            }
        });

        return button;
    }

    private void switchPanel(String panelName) {
        // Get the main content panel (which uses CardLayout)
        Component centerComponent = ((BorderLayout) mainAppPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent instanceof JPanel) {
            CardLayout cl = (CardLayout) ((JPanel) centerComponent).getLayout();
            cl.show((JPanel) centerComponent, panelName);
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            showLoginPanel();
        }
    }

    public void showMainApplication() {
        mainCardLayout.show(mainCardPanel, "main");
        switchPanel("Dashboard");
    }

    public void showLoginPanel() {
        mainCardLayout.show(mainCardPanel, "login");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            SecuritySuite suite = new SecuritySuite();
            suite.setVisible(true);
        });
    }
}