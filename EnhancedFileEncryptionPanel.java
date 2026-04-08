import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.util.Random;


class EnhancedFileEncryptionPanel extends JPanel {
    private JTextField filePathField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton browseButton, encryptButton, decryptButton, generateKeyButton;
    private JTextArea statusArea;
    private JProgressBar progressBar;
    private JCheckBox compressCheckbox, shredCheckbox, backupCheckbox;
    private JLabel dragDropLabel, encryptionStrengthLabel;
    private JComboBox<String> algorithmCombo;
    private Image backgroundImage;
    private java.util.Map<String, String> fileHistory;
    private boolean scanning = true;
    private JLabel fileSizeLabel, encryptionTimeLabel;
    private long totalBytesProcessed = 0;

    public EnhancedFileEncryptionPanel(Color primary, Color secondary, Color accent,
                                       Color danger, Color bgColor, Color cardColor) {
        loadBackgroundImage();
        setLayout(new BorderLayout());
        setOpaque(false);
        fileHistory = new java.util.HashMap<>();
        initializeComponents(primary, secondary, accent, danger, bgColor, cardColor);
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
                                      Color danger, Color bgColor, Color cardColor) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JScrollPane mainScrollPane = new JScrollPane(contentPanel);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.setOpaque(false);
        mainScrollPane.getViewport().setOpaque(false);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScrollPane, BorderLayout.CENTER);

        JLabel title = new JLabel("Advanced File & Folder Encryption");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(primary);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Drag & Drop panel
        JPanel dragDropPanel = createCardPanel(cardColor);
        dragDropPanel.setLayout(new BorderLayout());
        dragDropPanel.setPreferredSize(new Dimension(800, 120));
        dragDropPanel.setMaximumSize(new Dimension(800, 120));

        dragDropLabel = new JLabel("Drag & Drop Files or Folders Here", SwingConstants.CENTER);
        dragDropLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dragDropLabel.setForeground(primary);

        dragDropPanel.add(dragDropLabel, BorderLayout.CENTER);
        setupDragAndDrop();

        // Configuration panel
        JPanel configPanel = createCardPanel(cardColor);
        configPanel.setLayout(new GridBagLayout());
        configPanel.setPreferredSize(new Dimension(800, 280));
        configPanel.setMaximumSize(new Dimension(800, 280));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // File selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel fileLabel = new JLabel("File/Folder:");
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        configPanel.add(fileLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        filePathField = new JTextField();
        styleTextField(filePathField);
        filePathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateFileInfo(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateFileInfo(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateFileInfo(); }
        });
        configPanel.add(filePathField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        browseButton = new JButton("Browse");
        styleButton(browseButton, secondary);
        browseButton.addActionListener(e -> browseFileOrFolder());
        configPanel.add(browseButton, gbc);

        // File info
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
        fileSizeLabel = new JLabel("No file selected");
        fileSizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileSizeLabel.setForeground(Color.GRAY);
        configPanel.add(fileSizeLabel, gbc);

        // Algorithm selection
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel algoLabel = new JLabel("Algorithm:");
        algoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        configPanel.add(algoLabel, gbc);

        String[] algorithms = {"AES-256 (Recommended)", "AES-128", "ChaCha20", "Blowfish"};
        algorithmCombo = new JComboBox<>(algorithms);
        algorithmCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        configPanel.add(algorithmCombo, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        configPanel.add(passLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0;
        passwordField = new JPasswordField();
        styleTextField(passwordField);
        configPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel confirmLabel = new JLabel("Confirm:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        configPanel.add(confirmLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 1.0;
        confirmPasswordField = new JPasswordField();
        styleTextField(confirmPasswordField);
        configPanel.add(confirmPasswordField, gbc);

        gbc.gridx = 2; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        generateKeyButton = new JButton("Generate Strong Key");
        styleButton(generateKeyButton, accent);
        generateKeyButton.addActionListener(e -> generateStrongPassword());
        configPanel.add(generateKeyButton, gbc);

        // Options panel
        JPanel optionsPanel = createCardPanel(cardColor);
        optionsPanel.setLayout(new GridLayout(1, 3, 10, 0));

        compressCheckbox = new JCheckBox("Enable Compression");
        shredCheckbox = new JCheckBox("Secure Delete Original");
        backupCheckbox = new JCheckBox("Create Backup");

        styleCheckbox(compressCheckbox);
        styleCheckbox(shredCheckbox);
        styleCheckbox(backupCheckbox);

        compressCheckbox.setSelected(true);
        backupCheckbox.setSelected(true);

        optionsPanel.add(compressCheckbox);
        optionsPanel.add(shredCheckbox);
        optionsPanel.add(backupCheckbox);

        // Encryption strength indicator
        JPanel strengthPanel = createCardPanel(cardColor);
        strengthPanel.setLayout(new BorderLayout());

        encryptionStrengthLabel = new JLabel("Encryption Strength: Not Calculated", SwingConstants.CENTER);
        encryptionStrengthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        strengthPanel.add(encryptionStrengthLabel, BorderLayout.CENTER);

        // Performance info
        JPanel performancePanel = createCardPanel(cardColor);
        performancePanel.setLayout(new GridLayout(1, 2, 10, 0));

        encryptionTimeLabel = new JLabel("Estimated Time: N/A", SwingConstants.CENTER);
        encryptionTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        encryptionTimeLabel.setForeground(Color.GRAY);

        performancePanel.add(encryptionStrengthLabel);
        performancePanel.add(encryptionTimeLabel);

        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        controlPanel.setOpaque(false);
        controlPanel.setPreferredSize(new Dimension(800, 60));
        controlPanel.setMaximumSize(new Dimension(800, 60));

        encryptButton = new JButton("Encrypt Files");
        decryptButton = new JButton("Decrypt Files");

        styleButton(encryptButton, accent);
        styleButton(decryptButton, primary);

        encryptButton.addActionListener(e -> encryptFileOrFolder());
        decryptButton.addActionListener(e -> decryptFileOrFolder());

        controlPanel.add(encryptButton);
        controlPanel.add(decryptButton);

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(800, 25));
        progressBar.setMaximumSize(new Dimension(800, 25));

        // Status panel
        JPanel statusPanel = createCardPanel(cardColor);
        statusPanel.setLayout(new BorderLayout());
        statusPanel.setPreferredSize(new Dimension(800, 300));
        statusPanel.setMaximumSize(new Dimension(800, 300));

        JLabel statusLabel = new JLabel("Operation Log");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        statusArea.setBackground(new Color(250, 250, 250));
        statusArea.setText("Advanced File Encryption Tool Ready\n- Drag & drop files to encrypt\n- Use strong passwords for maximum security\n- Backup important files before encryption");

        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setPreferredSize(new Dimension(760, 220));

        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(statusScroll, BorderLayout.CENTER);

        // Add all components
        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(dragDropPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(configPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(optionsPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(performancePanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(controlPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(progressBar);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(statusPanel);

        // Add password strength listener
        passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateEncryptionStrength();
                updateEncryptionTimeEstimate();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateEncryptionStrength();
                updateEncryptionTimeEstimate();
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateEncryptionStrength();
                updateEncryptionTimeEstimate();
            }
        });

        algorithmCombo.addActionListener(e -> updateEncryptionTimeEstimate());
        compressCheckbox.addActionListener(e -> updateEncryptionTimeEstimate());
    }

    private void updateFileInfo() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            fileSizeLabel.setText("No file selected");
            encryptionTimeLabel.setText("Estimated Time: N/A");
            return;
        }

        File file = new File(filePath);
        if (file.exists()) {
            long size = calculateTotalSize(file);
            String sizeText = formatFileSize(size);
            fileSizeLabel.setText("Size: " + sizeText + " | " + (file.isDirectory() ? "Folder" : "File"));
            updateEncryptionTimeEstimate();
        } else {
            fileSizeLabel.setText("File not found");
            encryptionTimeLabel.setText("Estimated Time: N/A");
        }
    }

    private long calculateTotalSize(File file) {
        if (file.isFile()) {
            return file.length();
        } else if (file.isDirectory()) {
            long totalSize = 0;
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    totalSize += calculateTotalSize(f);
                }
            }
            return totalSize;
        }
        return 0;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private void updateEncryptionStrength() {
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            encryptionStrengthLabel.setText("Encryption Strength: Not Calculated");
            encryptionStrengthLabel.setForeground(Color.GRAY);
            return;
        }

        int strength = calculatePasswordStrength(password);
        String strengthText;
        Color color;

        if (strength >= 80) {
            strengthText = "Excellent (Military Grade)";
            color = new Color(46, 204, 113);
        } else if (strength >= 60) {
            strengthText = "Strong (Enterprise Grade)";
            color = new Color(52, 152, 219);
        } else if (strength >= 40) {
            strengthText = "Good (Standard Protection)";
            color = new Color(241, 196, 15);
        } else {
            strengthText = "Weak (Inadequate)";
            color = new Color(231, 76, 60);
        }

        encryptionStrengthLabel.setText("Encryption Strength: " + strengthText);
        encryptionStrengthLabel.setForeground(color);
    }

    private void updateEncryptionTimeEstimate() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            encryptionTimeLabel.setText("Estimated Time: N/A");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            encryptionTimeLabel.setText("Estimated Time: N/A");
            return;
        }

        long size = calculateTotalSize(file);
        String algorithm = (String) algorithmCombo.getSelectedItem();
        boolean compress = compressCheckbox.isSelected();

        // Realistic time estimates based on file size and algorithm
        double baseSpeed = compress ? 50 : 100; // MB/s
        if (algorithm.contains("AES-256")) baseSpeed *= 0.8;
        if (algorithm.contains("ChaCha20")) baseSpeed *= 1.2;

        double seconds = (size / (baseSpeed * 1024 * 1024));

        if (seconds < 1) {
            encryptionTimeLabel.setText("Estimated Time: < 1 second");
        } else if (seconds < 60) {
            encryptionTimeLabel.setText(String.format("Estimated Time: %.1f seconds", seconds));
        } else if (seconds < 3600) {
            encryptionTimeLabel.setText(String.format("Estimated Time: %.1f minutes", seconds / 60));
        } else {
            encryptionTimeLabel.setText(String.format("Estimated Time: %.1f hours", seconds / 3600));
        }
    }

    private int calculatePasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 15;
        if (password.length() >= 16) score += 10;
        if (Pattern.compile("[a-z]").matcher(password).find()) score += 10;
        if (Pattern.compile("[A-Z]").matcher(password).find()) score += 10;
        if (Pattern.compile("[0-9]").matcher(password).find()) score += 10;
        if (Pattern.compile("[^a-zA-Z0-9]").matcher(password).find()) score += 20;
        if (Pattern.compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9])").matcher(password).find()) score += 10;
        return Math.min(score, 100);
    }

    private void setupDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles = (java.util.List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        filePathField.setText(file.getAbsolutePath());
                        dragDropLabel.setText("File Selected: " + file.getName());
                        dragDropLabel.setForeground(new Color(46, 204, 113));
                        log("File selected via drag & drop: " + file.getName());
                        updateFileHistory(file);
                        updateFileInfo();
                    }
                } catch (Exception ex) {
                    log("Error: " + ex.getMessage());
                } finally {
                    resetDragDropLabel();
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                dragDropLabel.setText("Drop File Here!");
                dragDropLabel.setForeground(new Color(52, 152, 219));
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                resetDragDropLabel();
            }
        });
    }

    private void updateFileHistory(File file) {
        fileHistory.put(file.getAbsolutePath(), file.getName());
    }

    private void resetDragDropLabel() {
        Timer timer = new Timer(2000, e -> {
            dragDropLabel.setText("Drag & Drop Files or Folders Here");
            dragDropLabel.setForeground(new Color(41, 128, 185));
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void browseFileOrFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogTitle("Select File or Folder");
        chooser.setApproveButtonText("Select");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            filePathField.setText(selected.getAbsolutePath());
            log("Selected: " + selected.getName());
            updateFileHistory(selected);
            updateFileInfo();
        }
    }

    private void generateStrongPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%&*+-=?";

        Random random = new Random();
        StringBuilder password = new StringBuilder();

        // Ensure at least one of each type
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill remaining to 16 characters
        String allChars = upper + lower + digits + special;
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        String generated = new String(chars);
        passwordField.setText(generated);
        confirmPasswordField.setText(generated);
        updateEncryptionStrength();
        updateEncryptionTimeEstimate();
        log("Generated strong 16-character password");
    }

    private void encryptFileOrFolder() {
        if (!validateInput(true)) return;

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to encrypt the selected files?\n\n" +
                        "Warning: Keep your password safe! Without it, your files will be permanently inaccessible.",
                "Confirm Encryption",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            processFileOrFolder(true);
        }
    }

    private void decryptFileOrFolder() {
        if (!validateInput(false)) return;
        processFileOrFolder(false);
    }

    private boolean validateInput(boolean encrypt) {
        String filePath = filePathField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file or folder", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File or folder does not exist", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a password", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (encrypt && !password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password must be at least 8 characters long", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (encrypt && file.isDirectory() && calculateTotalSize(file) == 0) {
            JOptionPane.showMessageDialog(this, "Selected folder is empty", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void processFileOrFolder(boolean encrypt) {
        String filePath = filePathField.getText().trim();
        File file = new File(filePath);

        encryptButton.setEnabled(false);
        decryptButton.setEnabled(false);
        browseButton.setEnabled(false);
        generateKeyButton.setEnabled(false);

        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
            private long startTime;
            private long totalBytesToProcess = 0;

            @Override
            protected Boolean doInBackground() throws Exception {
                startTime = System.currentTimeMillis();
                totalBytesProcessed = 0; // Reset for each operation
                try {
                    if (file.isDirectory()) {
                        totalBytesToProcess = calculateTotalSize(file);
                        publish("Processing folder: " + file.getName());
                        publish("Total size: " + formatFileSize(totalBytesToProcess));
                        return processFolder(file, encrypt);
                    } else {
                        totalBytesToProcess = file.length();
                        return processSingleFile(file, encrypt);
                    }
                } catch (Exception e) {
                    publish("Error: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    log(msg);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    long endTime = System.currentTimeMillis();
                    long duration = (endTime - startTime) / 1000;

                    if (success) {
                        String operation = encrypt ? "Encryption" : "Decryption";
                        publish(operation + " completed successfully in " + duration + " seconds");
                        publish("Average speed: " + formatFileSize(totalBytesProcessed / Math.max(1, duration)) + "/s");

                        if (encrypt && shredCheckbox.isSelected()) {
                            publish("Original files securely deleted");
                        }
                    }
                } catch (Exception e) {
                    log("Operation failed: " + e.getMessage());
                } finally {
                    progressBar.setVisible(false);
                    encryptButton.setEnabled(true);
                    decryptButton.setEnabled(true);
                    browseButton.setEnabled(true);
                    generateKeyButton.setEnabled(true);
                    updateFileInfo();
                }
            }
        };

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        worker.execute();
    }

    private boolean processFolder(File folder, boolean encrypt) throws Exception {
        File[] files = folder.listFiles();
        if (files == null) return true;

        int totalFiles = countFiles(folder);
        int[] processedFiles = {0};

        log((encrypt ? "Encrypting" : "Decrypting") + " folder: " + folder.getName());
        log("Total files: " + totalFiles);

        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(totalFiles);
        });

        return processFilesRecursive(folder, encrypt, processedFiles, totalFiles);
    }

    private boolean processFilesRecursive(File folder, boolean encrypt, int[] processedFiles, int totalFiles) throws Exception {
        File[] files = folder.listFiles();
        if (files == null) return true;

        for (File file : files) {
            if (!scanning) break;

            if (file.isDirectory()) {
                if (!processFilesRecursive(file, encrypt, processedFiles, totalFiles)) {
                    return false;
                }
            } else {
                if (!processSingleFile(file, encrypt)) {
                    return false;
                }
                processedFiles[0]++;

                int progress = processedFiles[0];
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(progress);
                    progressBar.setString(progress + "/" + totalFiles + " files");
                });
            }
        }
        return true;
    }

    private int countFiles(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return 0;

        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += countFiles(file);
            } else {
                count++;
            }
        }
        return count;
    }

    private boolean processSingleFile(File inputFile, boolean encrypt) throws Exception {
        String password = new String(passwordField.getPassword());
        String algorithm = (String) algorithmCombo.getSelectedItem();

        if (encrypt) {
            // Create backup if requested
            if (backupCheckbox.isSelected()) {
                File backupFile = new File(inputFile.getAbsolutePath() + ".backup");
                copyFile(inputFile, backupFile);
                log("Backup created: " + backupFile.getName());
            }

            // Compress if requested
            File fileToEncrypt = inputFile;
            if (compressCheckbox.isSelected()) {
                File compressedFile = new File(inputFile.getAbsolutePath() + ".compressed");
                compressFile(inputFile, compressedFile);
                fileToEncrypt = compressedFile;
                log("File compressed: " + inputFile.getName());
            }

            String outputPath = fileToEncrypt.getAbsolutePath() + ".encrypted";
            File outputFile = new File(outputPath);

            encryptFile(fileToEncrypt, outputFile, password, algorithm);
            log("Encrypted: " + inputFile.getName() + " -> " + outputFile.getName());

            // Clean up temporary files
            if (compressCheckbox.isSelected()) {
                fileToEncrypt.delete();
            }

            // Secure delete original if requested
            if (shredCheckbox.isSelected()) {
                secureDelete(inputFile);
                log("Original file securely deleted: " + inputFile.getName());
            } else {
                inputFile.delete();
            }

        } else {
            // Decryption
            String originalPath = inputFile.getAbsolutePath();
            if (!originalPath.endsWith(".encrypted")) {
                throw new Exception("File is not encrypted (missing .encrypted extension)");
            }

            String outputPath = originalPath.substring(0, originalPath.length() - 10);
            File outputFile = new File(outputPath);

            // Check if file already exists
            if (outputFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                        "File already exists: " + outputFile.getName() + "\nOverwrite?",
                        "File Exists",
                        JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return true;
                }
            }

            decryptFile(inputFile, outputFile, password, algorithm);
            log("Decrypted: " + inputFile.getName() + " -> " + outputFile.getName());

            // Clean up encrypted file
            inputFile.delete();
        }

        return true;
    }

    private void encryptFile(File inputFile, File outputFile, String password, String algorithm) throws Exception {
        String algo = algorithm.contains("AES-256") ? "AES" :
                algorithm.contains("AES-128") ? "AES" :
                        algorithm.contains("ChaCha20") ? "ChaCha20" : "Blowfish";

        SecretKeySpec key = generateKey(password, algo);
        Cipher cipher;

        if ("ChaCha20".equals(algo)) {
            cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding");
        } else {
            cipher = Cipher.getInstance(algo + "/CBC/PKCS5Padding");
        }

        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        if ("ChaCha20".equals(algo)) {
            // ChaCha20 uses a 12-byte nonce
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            javax.crypto.spec.ChaCha20ParameterSpec paramSpec = new javax.crypto.spec.ChaCha20ParameterSpec(nonce, 1);
            cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        }

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // Write algorithm identifier and IV/Nonce
            fos.write(algo.getBytes().length);
            fos.write(algo.getBytes());
            if ("ChaCha20".equals(algo)) {
                fos.write(cipher.getIV()); // Write 12-byte nonce
            } else {
                fos.write(iv);
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = inputFile.length();
            long processedBytes = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    fos.write(output);
                }
                processedBytes += bytesRead;
                totalBytesProcessed += bytesRead;

                // Update progress for large files
                if (totalBytes > 0) {
                    final int progress = (int) ((processedBytes * 100) / totalBytes);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString(progress + "% - " + (algo.equals("ChaCha20") ? "ChaCha20" : algo) + " Encrypting");
                    });
                }
            }

            byte[] outputBytes = cipher.doFinal();
            if (outputBytes != null) {
                fos.write(outputBytes);
            }
        }
    }

    private void decryptFile(File inputFile, File outputFile, String password, String algorithm) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // Read algorithm identifier
            int algoLength = fis.read();
            byte[] algoBytes = new byte[algoLength];
            if (fis.read(algoBytes) != algoLength) {
                throw new Exception("Invalid encrypted file format");
            }
            String algo = new String(algoBytes);

            SecretKeySpec key = generateKey(password, algo);
            Cipher cipher;

            if ("ChaCha20".equals(algo)) {
                cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding");
                // Read 12-byte nonce for ChaCha20
                byte[] nonce = new byte[12];
                if (fis.read(nonce) != 12) {
                    throw new Exception("Invalid encrypted file format");
                }
                javax.crypto.spec.ChaCha20ParameterSpec paramSpec = new javax.crypto.spec.ChaCha20ParameterSpec(nonce, 1);
                cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            } else {
                cipher = Cipher.getInstance(algo + "/CBC/PKCS5Padding");
                // Read IV for AES/Blowfish
                byte[] iv = new byte[16];
                if (fis.read(iv) != 16) {
                    throw new Exception("Invalid encrypted file format");
                }
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = inputFile.length();
            long processedBytes = algoLength + (algo.equals("ChaCha20") ? 13 : 17);

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    fos.write(output);
                }
                processedBytes += bytesRead;
                totalBytesProcessed += bytesRead;

                // Update progress for large files
                if (totalBytes > 0) {
                    final int progress = (int) ((processedBytes * 100) / totalBytes);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString(progress + "% - " + (algo.equals("ChaCha20") ? "ChaCha20" : algo) + " Decrypting");
                    });
                }
            }

            byte[] outputBytes = cipher.doFinal();
            if (outputBytes != null) {
                fos.write(outputBytes);
            }
        }
    }

    private SecretKeySpec generateKey(String password, String algorithm) throws Exception {
        // Use PBKDF2 for key derivation
        byte[] salt = "SecuritySuiteSalt".getBytes("UTF-8");
        int iterations = 100000;
        int keyLength = algorithm.contains("256") ? 256 : algorithm.contains("128") ? 128 : 256;

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKey secretKey = factory.generateSecret(spec);

        return new SecretKeySpec(secretKey.getEncoded(), algorithm.equals("ChaCha20") ? "ChaCha20" : "AES");
    }

    private void compressFile(File inputFile, File outputFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, bytesRead);
            }
        }
    }

    private void copyFile(File source, File destination) throws Exception {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private void secureDelete(File file) {
        // 3-pass secure delete (DoD 5220.22-M standard)
        if (file.exists() && file.isFile()) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
                long length = raf.length();
                Random random = new Random();

                // Pass 1: Overwrite with zeros
                raf.seek(0);
                for (long i = 0; i < length; i++) {
                    raf.write(0);
                }

                // Pass 2: Overwrite with ones
                raf.seek(0);
                for (long i = 0; i < length; i++) {
                    raf.write(0xFF);
                }

                // Pass 3: Overwrite with random data
                raf.seek(0);
                byte[] randomData = new byte[8192];
                long written = 0;
                while (written < length) {
                    random.nextBytes(randomData);
                    int toWrite = (int) Math.min(randomData.length, length - written);
                    raf.write(randomData, 0, toWrite);
                    written += toWrite;
                }

                raf.close();
                file.delete();

            } catch (Exception e) {
                log("Secure delete failed: " + e.getMessage());
                // Fallback to normal delete
                file.delete();
            }
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    private JPanel createCardPanel(Color color) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 240));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        return panel;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(color);
            }
        });
    }

    private void styleCheckbox(JCheckBox checkbox) {
        checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkbox.setBackground(Color.WHITE);
        checkbox.setFocusPainted(false);
    }
}