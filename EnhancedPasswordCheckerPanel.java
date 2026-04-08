import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class EnhancedPasswordCheckerPanel extends JPanel {
    private JPasswordField passwordField;
    private JCheckBox showPassword;
    private JLabel strengthLabel;
    private JProgressBar strengthBar;
    private JTextArea feedbackArea;
    private JButton generateBtn, checkBreachBtn, saveBtn;
    private JComboBox<String> historyCombo;
    private Map<String, Integer> passwordHistory;
    private Image backgroundImage;

    public EnhancedPasswordCheckerPanel(Color primary, Color secondary, Color accent,
                                        Color danger, Color bgColor, Color cardColor, Color textColor) {
        loadBackgroundImage();
        setLayout(new BorderLayout());
        passwordHistory = new HashMap<>();
        initializeComponents(primary, secondary, accent, danger, bgColor, cardColor, textColor);
    }

    private void loadBackgroundImage() {
        try {
            java.net.URL imageURL = getClass().getResource("/background.gif");
            if (imageURL != null) {
                backgroundImage = new ImageIcon(imageURL).getImage();
            } else {
                backgroundImage = null;
            }
        } catch (Exception e) {
            backgroundImage = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private void initializeComponents(Color primary, Color secondary, Color accent,
                                      Color danger, Color bgColor, Color cardColor, Color textColor) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // Title
        JLabel title = new JLabel("Advanced Password Analyzer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(primary);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Main input panel
        JPanel inputPanel = createCardPanel(cardColor, textColor);
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel passwordLabel = new JLabel("Enter Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordLabel.setForeground(textColor);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        inputPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        styleTextField(passwordField, textColor);
        passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { checkPasswordStrength(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { checkPasswordStrength(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { checkPasswordStrength(); }
        });
        gbc.gridy = 1;
        inputPanel.add(passwordField, gbc);

        showPassword = new JCheckBox("Show Password");
        showPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPassword.setBackground(cardColor);
        showPassword.setForeground(textColor);
        showPassword.addActionListener(e -> {
            passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : '•');
        });
        gbc.gridy = 2;
        inputPanel.add(showPassword, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);

        generateBtn = new JButton("Generate Strong Password");
        checkBreachBtn = new JButton("Check Data Breaches");
        saveBtn = new JButton("Save to History");

        styleButton(generateBtn, secondary, textColor);
        styleButton(checkBreachBtn, accent, textColor);
        styleButton(saveBtn, primary, textColor);

        generateBtn.addActionListener(e -> generateStrongPassword());
        checkBreachBtn.addActionListener(e -> checkPasswordBreach());
        saveBtn.addActionListener(e -> savePasswordToHistory());

        buttonPanel.add(generateBtn);
        buttonPanel.add(checkBreachBtn);
        buttonPanel.add(saveBtn);

        gbc.gridy = 3; gbc.gridwidth = 2;
        inputPanel.add(buttonPanel, gbc);

        // History panel
        JPanel historyPanel = createCardPanel(cardColor, textColor);
        historyPanel.setLayout(new BorderLayout());

        JLabel historyLabel = new JLabel("Password History:");
        historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyLabel.setForeground(textColor);

        historyCombo = new JComboBox<>();
        historyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyCombo.addActionListener(e -> {
            if (historyCombo.getSelectedIndex() > 0) {
                String selected = (String) historyCombo.getSelectedItem();
                passwordField.setText(selected.replaceAll("\\*{8}", ""));
                checkPasswordStrength();
            }
        });

        JButton clearHistoryBtn = new JButton("Clear History");
        styleButton(clearHistoryBtn, danger, textColor);
        clearHistoryBtn.addActionListener(e -> clearHistory());

        JPanel historyTopPanel = new JPanel(new BorderLayout());
        historyTopPanel.setOpaque(false);
        historyTopPanel.add(historyLabel, BorderLayout.WEST);
        historyTopPanel.add(clearHistoryBtn, BorderLayout.EAST);

        historyPanel.add(historyTopPanel, BorderLayout.NORTH);
        historyPanel.add(historyCombo, BorderLayout.CENTER);

        // Strength panel
        JPanel strengthPanel = createCardPanel(cardColor, textColor);
        strengthPanel.setLayout(new BoxLayout(strengthPanel, BoxLayout.Y_AXIS));

        strengthBar = new JProgressBar(0, 100);
        strengthBar.setPreferredSize(new Dimension(300, 25));
        strengthBar.setStringPainted(true);
        strengthBar.setString("Enter password to analyze");

        strengthLabel = new JLabel("Password strength will appear here");
        strengthLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        strengthLabel.setForeground(Color.GRAY);
        strengthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        strengthPanel.add(strengthLabel);
        strengthPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        strengthPanel.add(strengthBar);

        // Advanced analysis panel
        JPanel analysisPanel = createCardPanel(cardColor, textColor);
        analysisPanel.setLayout(new BorderLayout());

        JLabel analysisLabel = new JLabel("Detailed Analysis:");
        analysisLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        analysisLabel.setForeground(textColor);
        analysisLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        feedbackArea = new JTextArea(10, 30);
        feedbackArea.setEditable(false);
        feedbackArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        feedbackArea.setBackground(cardColor);
        feedbackArea.setForeground(textColor);
        feedbackArea.setText("Enter a password to see detailed analysis...");
        JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
        feedbackScroll.getVerticalScrollBar().setUnitIncrement(16);

        analysisPanel.add(analysisLabel, BorderLayout.NORTH);
        analysisPanel.add(feedbackScroll, BorderLayout.CENTER);

        // Add components to content panel
        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(inputPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(historyPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(strengthPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(analysisPanel);

        updateHistoryCombo();
    }

    private void checkPasswordStrength() {
        char[] password = passwordField.getPassword();
        if (password.length == 0) {
            strengthLabel.setText("Enter password to analyze");
            strengthLabel.setForeground(Color.GRAY);
            strengthBar.setValue(0);
            strengthBar.setString("Enter password to analyze");
            feedbackArea.setText("Enter a password to see detailed analysis...");
            return;
        }

        String pass = new String(password);
        int score = calculatePasswordScore(pass);
        String strength = getStrengthLevel(score);
        Color color = getStrengthColor(strength);
        String crackTime = estimateCrackTime(pass);

        strengthBar.setValue(score);
        strengthBar.setForeground(color);
        strengthBar.setString(strength + " (" + score + "%)");
        strengthLabel.setText("Crack Time: " + crackTime);
        strengthLabel.setForeground(color);

        // Generate detailed feedback
        StringBuilder feedback = new StringBuilder();
        feedback.append("PASSWORD ANALYSIS REPORT\n");
        feedback.append("========================\n\n");

        feedback.append("BASIC METRICS:\n");
        feedback.append("• Length: ").append(pass.length()).append(" characters\n");
        feedback.append("• Strength Score: ").append(score).append("/100\n");
        feedback.append("• Security Level: ").append(strength).append("\n");
        feedback.append("• Estimated Crack Time: ").append(crackTime).append("\n\n");

        feedback.append("COMPOSITION ANALYSIS:\n");
        feedback.append(generateDetailedFeedback(pass)).append("\n");

        feedback.append("SECURITY RECOMMENDATIONS:\n");
        if (score >= 80) {
            feedback.append("• Excellent password security\n");
            feedback.append("• No immediate changes needed\n");
        } else if (score >= 60) {
            feedback.append("• Good password foundation\n");
            feedback.append("• Consider adding special characters\n");
            feedback.append("• Increase length to 12+ characters\n");
        } else if (score >= 40) {
            feedback.append("• Moderate security level\n");
            feedback.append("• Add uppercase letters and numbers\n");
            feedback.append("• Use special characters\n");
            feedback.append("• Increase length to 12+ characters\n");
        } else {
            feedback.append("• Weak password detected\n");
            feedback.append("• Immediate change recommended\n");
            feedback.append("• Use our password generator\n");
        }

        feedbackArea.setText(feedback.toString());
    }

    private int calculatePasswordScore(String password) {
        int score = 0;

        // Length scoring (max 30 points)
        if (password.length() >= 8) score += 10;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;

        // Character variety (max 50 points)
        if (Pattern.compile("[a-z]").matcher(password).find()) score += 10;
        if (Pattern.compile("[A-Z]").matcher(password).find()) score += 10;
        if (Pattern.compile("[0-9]").matcher(password).find()) score += 10;
        if (Pattern.compile("[^a-zA-Z0-9]").matcher(password).find()) score += 15;

        // Bonus for all character types (max 15 points)
        if (Pattern.compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9])").matcher(password).find())
            score += 15;

        // Entropy bonus (max 15 points)
        double entropy = calculateEntropy(password);
        if (entropy > 60) score += 15;
        else if (entropy > 40) score += 10;
        else if (entropy > 20) score += 5;

        // Penalties for weak patterns
        if (password.length() < 8) score -= 20;
        if (Pattern.compile("(.)\\1{2,}").matcher(password).find()) score -= 15;
        if (Pattern.compile("(123|abc|password|admin|qwerty|123456|letmein|welcome|111111|000000)").matcher(password.toLowerCase()).find())
            score -= 25;
        if (hasSequentialChars(password, 3)) score -= 10;
        if (isCommonPattern(password)) score -= 15;

        return Math.min(Math.max(score, 0), 100);
    }

    private double calculateEntropy(String password) {
        boolean hasLower = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasUpper = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasDigit = Pattern.compile("[0-9]").matcher(password).find();
        boolean hasSpecial = Pattern.compile("[^a-zA-Z0-9]").matcher(password).find();

        int poolSize = 0;
        if (hasLower) poolSize += 26;
        if (hasUpper) poolSize += 26;
        if (hasDigit) poolSize += 10;
        if (hasSpecial) poolSize += 32;

        return password.length() * (Math.log(poolSize) / Math.log(2));
    }

    private String estimateCrackTime(String password) {
        double entropy = calculateEntropy(password);

        // More realistic crack time estimation
        if (entropy < 28) return "Instantly";
        if (entropy < 35) return "Seconds";
        if (entropy < 45) return "Minutes";
        if (entropy < 55) return "Hours";
        if (entropy < 65) return "Days";
        if (entropy < 75) return "Weeks";
        if (entropy < 85) return "Months";
        if (entropy < 95) return "Years";
        return "Decades";
    }

    private String generateDetailedFeedback(String password) {
        StringBuilder feedback = new StringBuilder();

        // Length analysis
        if (password.length() < 8) {
            feedback.append("• CRITICAL: Too short (minimum 8 characters required)\n");
        } else if (password.length() < 12) {
            feedback.append("• Good: Minimum length achieved\n");
            feedback.append("• Suggestion: Use 12+ characters for better security\n");
        } else {
            feedback.append("• Excellent: Strong length\n");
        }

        // Character type analysis
        boolean hasLower = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasUpper = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasDigit = Pattern.compile("[0-9]").matcher(password).find();
        boolean hasSpecial = Pattern.compile("[^a-zA-Z0-9]").matcher(password).find();

        feedback.append(hasLower ? "• Contains lowercase letters\n" : "• Missing lowercase letters\n");
        feedback.append(hasUpper ? "• Contains uppercase letters\n" : "• Missing uppercase letters\n");
        feedback.append(hasDigit ? "• Contains numbers\n" : "• Missing numbers\n");
        feedback.append(hasSpecial ? "• Contains special characters\n" : "• Missing special characters\n");

        // Pattern analysis
        if (Pattern.compile("(.)\\1{2,}").matcher(password).find()) {
            feedback.append("• Warning: Repeated character patterns detected\n");
        }
        if (hasSequentialChars(password, 3)) {
            feedback.append("• Warning: Sequential patterns (abc, 123, etc.)\n");
        }
        if (isCommonPattern(password)) {
            feedback.append("• CRITICAL: Common dictionary word detected\n");
        }

        // Entropy information
        double entropy = calculateEntropy(password);
        feedback.append(String.format("• Entropy: %.1f bits\n", entropy));

        if (entropy < 40) feedback.append("• Security: Low - easily crackable\n");
        else if (entropy < 60) feedback.append("• Security: Moderate\n");
        else if (entropy < 80) feedback.append("• Security: Good\n");
        else feedback.append("• Security: Excellent\n");

        return feedback.toString();
    }

    private void generateStrongPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%&*+-=?";

        Random random = new Random();
        StringBuilder password = new StringBuilder();

        // Generate 12-16 character password
        int length = 12 + random.nextInt(5);

        // Ensure at least one of each type
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill remaining characters
        String allChars = upper + lower + digits + special;
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        passwordField.setText(new String(chars));
        checkPasswordStrength();
    }

    private void checkPasswordBreach() {
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a password first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                checkBreachBtn.setEnabled(false);
                checkBreachBtn.setText("Checking...");

                Thread.sleep(1500);

                SwingUtilities.invokeLater(() -> {
                    // More realistic breach detection
                    boolean isBreached = isPasswordBreached(password);
                    if (isBreached) {
                        JOptionPane.showMessageDialog(this,
                                "SECURITY ALERT: This password has been compromised in data breaches!\n\n" +
                                        "This password appears in known breach databases.\n" +
                                        "Immediate password change is strongly recommended.",
                                "Password Breach Detected",
                                JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "No known breaches found for this password.\n\n" +
                                        "Note: This check simulates breach database lookup.\n" +
                                        "For real-world protection, use unique passwords for each service.",
                                "Breach Check Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    checkBreachBtn.setEnabled(true);
                    checkBreachBtn.setText("Check Data Breaches");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private boolean isPasswordBreached(String password) {
        // Check against common breached passwords
        String[] breachedPasswords = {
                "password", "123456", "12345678", "1234", "qwerty", "12345",
                "dragon", "baseball", "football", "letmein", "monkey", "abc123",
                "mustang", "michael", "shadow", "master", "jennifer", "111111",
                "2000", "jordan", "superman", "harley", "1234567", "freedom",
                "admin", "welcome", "passw0rd", "password1", "123123", "hello"
        };

        String lowerPassword = password.toLowerCase();
        for (String breached : breachedPasswords) {
            if (lowerPassword.equals(breached)) {
                return true;
            }
        }

        // Check for very weak patterns
        if (password.length() < 6) return true;
        if (Pattern.compile("^[0-9]+$").matcher(password).find()) return true;

        return false;
    }

    private void savePasswordToHistory() {
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No password to save", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String masked = password.substring(0, Math.min(2, password.length())) +
                "****" +
                (password.length() > 6 ? password.substring(password.length() - 2) : "");

        passwordHistory.put(masked, calculatePasswordScore(password));
        updateHistoryCombo();
        JOptionPane.showMessageDialog(this, "Password saved to history", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearHistory() {
        if (passwordHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "History is already empty", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Clear all password history?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            passwordHistory.clear();
            updateHistoryCombo();
            JOptionPane.showMessageDialog(this, "History cleared", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateHistoryCombo() {
        historyCombo.removeAllItems();
        historyCombo.addItem("-- Select from history --");
        for (String pwd : passwordHistory.keySet()) {
            historyCombo.addItem(pwd + " (Score: " + passwordHistory.get(pwd) + ")");
        }
    }

    // Helper methods
    private boolean hasSequentialChars(String password, int sequenceLength) {
        password = password.toLowerCase();
        for (int i = 0; i <= password.length() - sequenceLength; i++) {
            String segment = password.substring(i, i + sequenceLength);
            if (isSequential(segment)) return true;
        }
        return false;
    }

    private boolean isSequential(String segment) {
        boolean ascending = true;
        boolean descending = true;

        for (int i = 1; i < segment.length(); i++) {
            if (segment.charAt(i) - segment.charAt(i-1) != 1) {
                ascending = false;
            }
            if (segment.charAt(i-1) - segment.charAt(i) != 1) {
                descending = false;
            }
        }

        return ascending || descending;
    }

    private boolean isCommonPattern(String password) {
        String[] commonPatterns = {
                "password", "admin", "welcome", "letmein", "monkey", "dragon",
                "master", "qwerty", "baseball", "football", "mustang", "shadow"
        };
        String lower = password.toLowerCase();
        for (String pattern : commonPatterns) {
            if (lower.contains(pattern)) return true;
        }
        return false;
    }

    private String getStrengthLevel(int score) {
        if (score >= 80) return "Very Strong";
        if (score >= 60) return "Strong";
        if (score >= 40) return "Moderate";
        if (score >= 20) return "Weak";
        return "Very Weak";
    }

    private Color getStrengthColor(String strength) {
        switch (strength) {
            case "Very Strong": return new Color(46, 204, 113);
            case "Strong": return new Color(52, 152, 219);
            case "Moderate": return new Color(241, 196, 15);
            case "Weak": return new Color(230, 126, 34);
            case "Very Weak": return new Color(231, 76, 60);
            default: return Color.GRAY;
        }
    }

    private JPanel createCardPanel(Color bgColor, Color textColor) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 240));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        return panel;
    }

    private void styleTextField(JTextField field, Color textColor) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(textColor);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
    }

    private void styleButton(JButton button, Color bgColor, Color textColor) {
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
    }
}