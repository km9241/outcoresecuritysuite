import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.table.DefaultTableCellRenderer;

class EnhancedNetworkScannerPanel extends JPanel {
    private JTextField ipRangeField;
    private JTextField startPortField, endPortField;
    private JButton scanButton, stopButton, exportButton, saveProfileButton;
    private JTextArea logArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;
    private JComboBox<String> commonPortsCombo, scanProfilesCombo;
    private JTextArea portInfoArea;
    private boolean scanning = false;
    private ExecutorService executor;
    private Image backgroundImage;
    private Map<String, ScanProfile> scanProfiles;
    private long scanStartTime;
    private int openPortsFound = 0;

    // Scan profile class
    private static class ScanProfile {
        String name;
        String ipRange;
        int startPort;
        int endPort;

        ScanProfile(String name, String ipRange, int startPort, int endPort) {
            this.name = name;
            this.ipRange = ipRange;
            this.startPort = startPort;
            this.endPort = endPort;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public EnhancedNetworkScannerPanel(Color primary, Color secondary, Color accent,
                                       Color danger, Color bgColor, Color cardColor) {
        loadBackgroundImage();
        setLayout(new BorderLayout());
        scanProfiles = new HashMap<>();
        initializeProfiles();
        initializeComponents(primary, secondary, accent, danger, bgColor, cardColor);
    }

    private void initializeProfiles() {
        scanProfiles.put("Quick Scan", new ScanProfile("Quick Scan", "192.168.1.1-50", 1, 1000));
        scanProfiles.put("Web Services", new ScanProfile("Web Services", "192.168.1.1-100", 80, 443));
        scanProfiles.put("Full Network", new ScanProfile("Full Network", "192.168.1.1-254", 1, 1000));
        scanProfiles.put("Common Services", new ScanProfile("Common Services", "192.168.1.1-100", 1, 10000));
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

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // Title
        JLabel title = new JLabel("Advanced Network Scanner");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(primary);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Scan configuration panel
        JPanel configPanel = createCardPanel(cardColor);
        configPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Scan profiles
        JLabel profileLabel = new JLabel("Scan Profile:");
        profileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        configPanel.add(profileLabel, gbc);

        scanProfilesCombo = new JComboBox<>();
        for (String profile : scanProfiles.keySet()) {
            scanProfilesCombo.addItem(profile);
        }
        scanProfilesCombo.addActionListener(e -> loadProfile());
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        configPanel.add(scanProfilesCombo, gbc);

        saveProfileButton = new JButton("Save Profile");
        styleButton(saveProfileButton, secondary);
        saveProfileButton.addActionListener(e -> saveCurrentProfile());
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        configPanel.add(saveProfileButton, gbc);

        // IP Range
        JLabel ipLabel = new JLabel("IP Range:");
        ipLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        configPanel.add(ipLabel, gbc);

        ipRangeField = new JTextField("192.168.1.1-100");
        styleTextField(ipRangeField);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        configPanel.add(ipRangeField, gbc);

        // Port Range
        JLabel portLabel = new JLabel("Port Range:");
        portLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        configPanel.add(portLabel, gbc);

        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        portPanel.setOpaque(false);

        startPortField = new JTextField("1", 6);
        endPortField = new JTextField("1000", 6);
        styleTextField(startPortField);
        styleTextField(endPortField);

        portPanel.add(startPortField);
        portPanel.add(new JLabel(" to "));
        portPanel.add(endPortField);

        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        configPanel.add(portPanel, gbc);

        // Common ports combo
        JLabel commonPortsLabel = new JLabel("Quick Port Sets:");
        commonPortsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        configPanel.add(commonPortsLabel, gbc);

        String[] commonPorts = {"Custom", "Web (80,443,8080)", "All Common (1-1000)", "Full (1-65535)", "Services (21,22,23,25,53,80,110,143,443,993,995,3389)"};
        commonPortsCombo = new JComboBox<>(commonPorts);
        commonPortsCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        commonPortsCombo.addActionListener(e -> {
            String selected = (String) commonPortsCombo.getSelectedItem();
            switch (selected) {
                case "Web (80,443,8080)":
                    startPortField.setText("80");
                    endPortField.setText("8080");
                    break;
                case "All Common (1-1000)":
                    startPortField.setText("1");
                    endPortField.setText("1000");
                    break;
                case "Full (1-65535)":
                    startPortField.setText("1");
                    endPortField.setText("65535");
                    break;
                case "Services (21,22,23,25,53,80,110,143,443,993,995,3389)":
                    startPortField.setText("21");
                    endPortField.setText("3389");
                    break;
            }
        });
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0;
        configPanel.add(commonPortsCombo, gbc);

        // Control buttons panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        controlPanel.setOpaque(false);
        controlPanel.setMaximumSize(new Dimension(800, 60));

        scanButton = new JButton("Start Scan");
        stopButton = new JButton("Stop Scan");
        exportButton = new JButton("Export Results");

        styleButton(scanButton, accent);
        styleButton(stopButton, danger);
        styleButton(exportButton, secondary);
        stopButton.setEnabled(false);

        scanButton.addActionListener(e -> startScan());
        stopButton.addActionListener(e -> stopScan());
        exportButton.addActionListener(e -> exportResults());

        controlPanel.add(scanButton);
        controlPanel.add(stopButton);
        controlPanel.add(exportButton);

        // Progress bar - KEPT
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(800, 25));
        progressBar.setMaximumSize(new Dimension(800, 25));

        // Results table
        JPanel resultsPanel = createCardPanel(cardColor);
        resultsPanel.setLayout(new BorderLayout());

        JLabel resultsLabel = new JLabel("Scan Results");
        resultsLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        resultsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        String[] columns = {"IP Address", "Port", "Status", "Service", "Description", "Risk Level"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultTable.setRowHeight(25);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add renderer for risk levels
        resultTable.getColumnModel().getColumn(5).setCellRenderer(new RiskLevelRenderer());

        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setPreferredSize(new Dimension(780, 200));

        resultsPanel.add(resultsLabel, BorderLayout.NORTH);
        resultsPanel.add(tableScroll, BorderLayout.CENTER);

        // Log and info panel
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));

        // Log panel
        JPanel logPanel = createCardPanel(cardColor);
        logPanel.setLayout(new BorderLayout());

        JLabel logLabel = new JLabel("Scan Log");
        logLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(250, 250, 250));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(380, 150));

        logPanel.add(logLabel, BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);

        // Port info panel
        JPanel infoPanel = createCardPanel(cardColor);
        infoPanel.setLayout(new BorderLayout());

        JLabel infoLabel = new JLabel("Port Information");
        infoLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        portInfoArea = new JTextArea();
        portInfoArea.setEditable(false);
        portInfoArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        portInfoArea.setBackground(new Color(250, 250, 250));
        portInfoArea.setText("Select a scan result to view detailed information...");

        JScrollPane infoScroll = new JScrollPane(portInfoArea);
        infoScroll.setPreferredSize(new Dimension(380, 150));

        infoPanel.add(infoLabel, BorderLayout.NORTH);
        infoPanel.add(infoScroll, BorderLayout.CENTER);

        bottomPanel.add(logPanel);
        bottomPanel.add(infoPanel);

        // Add all components
        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(configPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(controlPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(progressBar);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(resultsPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(bottomPanel);

        // Add table selection listener
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && resultTable.getSelectedRow() != -1) {
                int row = resultTable.getSelectedRow();
                String ip = (String) tableModel.getValueAt(row, 0);
                int port = (int) tableModel.getValueAt(row, 1);
                updatePortInfo(ip, port);
            }
        });
    }

    private void loadProfile() {
        String selected = (String) scanProfilesCombo.getSelectedItem();
        if (selected != null && scanProfiles.containsKey(selected)) {
            ScanProfile profile = scanProfiles.get(selected);
            ipRangeField.setText(profile.ipRange);
            startPortField.setText(String.valueOf(profile.startPort));
            endPortField.setText(String.valueOf(profile.endPort));
        }
    }

    private void saveCurrentProfile() {
        String name = JOptionPane.showInputDialog(this, "Enter profile name:", "Save Scan Profile", JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            try {
                String ipRange = ipRangeField.getText();
                int startPort = Integer.parseInt(startPortField.getText());
                int endPort = Integer.parseInt(endPortField.getText());

                scanProfiles.put(name, new ScanProfile(name, ipRange, startPort, endPort));
                scanProfilesCombo.addItem(name);
                scanProfilesCombo.setSelectedItem(name);

                log("Profile saved: " + name);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid port numbers", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startScan() {
        String ipRange = ipRangeField.getText().trim();
        int startPort, endPort;

        try {
            startPort = Integer.parseInt(startPortField.getText().trim());
            endPort = Integer.parseInt(endPortField.getText().trim());

            if (startPort < 1 || endPort > 65535 || startPort > endPort) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid port range. Ports must be between 1-65535.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableModel.setRowCount(0);
        logArea.setText("");
        scanning = true;
        openPortsFound = 0;
        scanStartTime = System.currentTimeMillis();
        scanButton.setEnabled(false);
        stopButton.setEnabled(true);
        exportButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        executor = Executors.newFixedThreadPool(20);

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            private int completedScans = 0;
            private int totalScans = 0;
            private List<String> ips;

            @Override
            protected Void doInBackground() {
                try {
                    ips = parseIPRange(ipRange);
                    if (ips.isEmpty()) {
                        publish("Error: No valid IP addresses found. Check your IP range format.");
                        return null;
                    }

                    totalScans = ips.size() * (endPort - startPort + 1);
                    publish("Starting network scan...");
                    publish("Targets: " + ips.size() + " hosts, ports " + startPort + "-" + endPort);
                    publish("Total operations: " + totalScans);
                    publish("Using 20 concurrent scanners");

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setMaximum(totalScans);
                        progressBar.setValue(0);
                    });

                    List<Future<PortScanResult>> futures = new ArrayList<>();

                    // Scan ports on all hosts
                    publish("Scanning ports on " + ips.size() + " hosts...");

                    for (String ip : ips) {
                        if (!scanning) break;

                        for (int port = startPort; port <= endPort; port++) {
                            if (!scanning) break;

                            final int currentPort = port;
                            final String currentIP = ip;

                            Future<PortScanResult> future = executor.submit(() -> {
                                try {
                                    return scanPort(currentIP, currentPort);
                                } catch (Exception e) {
                                    return new PortScanResult(currentIP, currentPort, "ERROR", e.getMessage());
                                }
                            });
                            futures.add(future);
                        }
                    }

                    // Process results
                    for (Future<PortScanResult> future : futures) {
                        if (!scanning) break;
                        try {
                            PortScanResult result = future.get(3, TimeUnit.SECONDS);
                            completedScans++;

                            if ("OPEN".equals(result.status)) {
                                openPortsFound++;
                                String service = getServiceName(result.port);
                                String description = getPortDescription(result.port);
                                String risk = assessRiskLevel(result.port, service);

                                addResult(result.ip, result.port, result.status, service, description, risk);
                                publish("OPEN: " + result.ip + ":" + result.port + " (" + service + ")");
                            }

                            // Update progress
                            final int progress = completedScans;
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(progress);
                                progressBar.setString(progress + " / " + totalScans + " (" + openPortsFound + " open)");
                            });

                        } catch (TimeoutException e) {
                            completedScans++;
                        } catch (Exception e) {
                            completedScans++;
                        }
                    }

                    executor.shutdown();
                    boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
                    if (!terminated) {
                        executor.shutdownNow();
                    }

                    long elapsed = (System.currentTimeMillis() - scanStartTime) / 1000;
                    publish("Scan completed in " + elapsed + " seconds");
                    publish("Found " + openPortsFound + " open ports across " + ips.size() + " hosts");

                    if (openPortsFound == 0) {
                        publish("No open ports found in the specified range");
                    }

                } catch (Exception e) {
                    publish("Scan error: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    log(msg);
                }
            }

            @Override
            protected void done() {
                scanning = false;
                scanButton.setEnabled(true);
                stopButton.setEnabled(false);
                exportButton.setEnabled(true);
                progressBar.setVisible(false);

                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                }
            }
        };

        worker.execute();
    }

    private PortScanResult scanPort(String ip, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 1500);
            socket.close();
            return new PortScanResult(ip, port, "OPEN", "Successfully connected");
        } catch (IOException e) {
            return new PortScanResult(ip, port, "CLOSED", "Connection refused");
        }
    }

    private void stopScan() {
        scanning = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log("Scan stopped by user");
    }

    private void exportResults() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No results to export", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Scan Results");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new java.io.File(file.getAbsolutePath() + ".csv");
                }

                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    // Write header
                    writer.println("IP Address,Port,Status,Service,Description,Risk Level");

                    // Write data
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        writer.printf("%s,%d,%s,%s,%s,%s%n",
                                tableModel.getValueAt(i, 0),
                                tableModel.getValueAt(i, 1),
                                tableModel.getValueAt(i, 2),
                                tableModel.getValueAt(i, 3),
                                tableModel.getValueAt(i, 4),
                                tableModel.getValueAt(i, 5)
                        );
                    }
                }

                log("Results exported to: " + file.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Results exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Helper classes and methods
    private static class PortScanResult {
        String ip;
        int port;
        String status;
        String message;

        PortScanResult(String ip, int port, String status, String message) {
            this.ip = ip;
            this.port = port;
            this.status = status;
            this.message = message;
        }
    }

    private class RiskLevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null) {
                String risk = value.toString();
                switch (risk) {
                    case "HIGH":
                        c.setBackground(new Color(255, 200, 200));
                        c.setForeground(Color.RED);
                        break;
                    case "MEDIUM":
                        c.setBackground(new Color(255, 255, 200));
                        c.setForeground(Color.ORANGE);
                        break;
                    case "LOW":
                        c.setBackground(new Color(200, 255, 200));
                        c.setForeground(Color.GREEN);
                        break;
                    default:
                        c.setBackground(table.getBackground());
                        c.setForeground(table.getForeground());
                }
            }

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            }

            return c;
        }
    }

    private void updatePortInfo(String ip, int port) {
        String service = getServiceName(port);
        String description = getPortDescription(port);
        String risk = assessRiskLevel(port, service);
        String vulnerabilities = getVulnerabilityInfo(port, service);

        String info = "IP Address: " + ip + "\n" +
                "Port: " + port + "\n" +
                "Service: " + service + "\n" +
                "Description: " + description + "\n" +
                "Risk Level: " + risk + "\n\n" +
                "Security Notes:\n" + vulnerabilities + "\n\n" +
                "Recommendations:\n" + getSecurityRecommendations(port, service);

        portInfoArea.setText(info);
    }

    private String assessRiskLevel(int port, String service) {
        // High risk ports
        if (port == 23 || port == 21 || port == 161 || port == 162 || port == 445) return "HIGH";
        // Medium risk ports
        if (port == 22 || port == 25 || port == 53 || port == 110 || port == 143 || port == 3389) return "MEDIUM";
        // Low risk ports
        if (port == 80 || port == 443 || port == 993 || port == 995) return "LOW";
        return "UNKNOWN";
    }

    private String getVulnerabilityInfo(int port, String service) {
        Map<Integer, String> vulns = new HashMap<>();
        vulns.put(21, "• FTP is unencrypted\n• Vulnerable to sniffing\n• Anonymous access possible");
        vulns.put(22, "• SSH weak configurations\n• Brute force attacks");
        vulns.put(23, "• Telnet is completely unencrypted\n• Credentials transmitted in clear text");
        vulns.put(25, "• SMTP open relay vulnerabilities\n• Spam and phishing risks");
        vulns.put(53, "• DNS cache poisoning\n• DNS amplification attacks");
        vulns.put(80, "• HTTP unencrypted\n• Session hijacking possible");
        vulns.put(443, "• Generally secure when properly configured");
        vulns.put(445, "• SMB vulnerabilities (EternalBlue)\n• Worm propagation");
        vulns.put(3389, "• RDP brute force attacks\n• BlueKeep vulnerability");

        return vulns.getOrDefault(port, "• No specific vulnerabilities known\n• Ensure proper configuration");
    }

    private String getSecurityRecommendations(int port, String service) {
        Map<Integer, String> recommendations = new HashMap<>();
        recommendations.put(21, "• Use SFTP/SCP instead\n• Disable anonymous access\n• Implement strong authentication");
        recommendations.put(22, "• Use key-based authentication\n• Disable root login\n• Change default port");
        recommendations.put(23, "• Replace with SSH immediately\n• Never use in production");
        recommendations.put(25, "• Implement SMTP authentication\n• Use TLS encryption\n• Configure proper relay rules");
        recommendations.put(53, "• Use DNSSEC\n• Implement DNS filtering\n• Monitor for unusual queries");
        recommendations.put(80, "• Redirect to HTTPS\n• Implement HSTS\n• Use secure cookies");
        recommendations.put(445, "• Disable SMBv1\n• Use network segmentation\n• Keep systems updated");
        recommendations.put(3389, "• Use VPN instead of direct RDP\n• Implement Network Level Authentication\n• Change default port");

        return recommendations.getOrDefault(port, "• Keep software updated\n• Use strong passwords\n• Monitor for suspicious activity");
    }

    private List<String> parseIPRange(String ipRange) {
        List<String> ips = new ArrayList<>();
        try {
            if (ipRange.contains("-")) {
                String base = ipRange.substring(0, ipRange.lastIndexOf('.') + 1);
                String rangePart = ipRange.substring(ipRange.lastIndexOf('.') + 1);
                String[] rangeParts = rangePart.split("-");
                if (rangeParts.length == 2) {
                    int start = Integer.parseInt(rangeParts[0].trim());
                    int end = Integer.parseInt(rangeParts[1].trim());
                    for (int i = start; i <= end; i++) {
                        ips.add(base + i);
                    }
                }
            } else if (ipRange.contains("/")) {
                // Basic CIDR support for /24 networks
                String[] parts = ipRange.split("/");
                if (parts.length == 2) {
                    String network = parts[0];
                    int prefix = Integer.parseInt(parts[1]);
                    if (prefix == 24) {
                        String base = network.substring(0, network.lastIndexOf('.') + 1);
                        for (int i = 1; i <= 254; i++) {
                            ips.add(base + i);
                        }
                    }
                }
            } else {
                // Single IP
                ips.add(ipRange);
            }
        } catch (Exception e) {
            log("Error parsing IP range: " + e.getMessage());
        }
        return ips;
    }

    private String getServiceName(int port) {
        return switch (port) {
            case 20, 21 -> "FTP";
            case 22 -> "SSH";
            case 23 -> "Telnet";
            case 25 -> "SMTP";
            case 53 -> "DNS";
            case 80 -> "HTTP";
            case 110 -> "POP3";
            case 143 -> "IMAP";
            case 443 -> "HTTPS";
            case 993 -> "IMAPS";
            case 995 -> "POP3S";
            case 3306 -> "MySQL";
            case 3389 -> "RDP";
            case 5432 -> "PostgreSQL";
            case 8080 -> "HTTP-Alt";
            case 8443 -> "HTTPS-Alt";
            case 445 -> "SMB";
            default -> "Unknown";
        };
    }

    private String getPortDescription(int port) {
        return switch (port) {
            case 20, 21 -> "File Transfer Protocol";
            case 22 -> "Secure Shell";
            case 23 -> "Telnet (Insecure)";
            case 25 -> "Email (SMTP)";
            case 53 -> "Domain Name System";
            case 80 -> "Web Server (HTTP)";
            case 110 -> "Email (POP3)";
            case 143 -> "Email (IMAP)";
            case 443 -> "Secure Web Server (HTTPS)";
            case 993 -> "Secure IMAP";
            case 995 -> "Secure POP3";
            case 3306 -> "MySQL Database";
            case 3389 -> "Remote Desktop";
            case 5432 -> "PostgreSQL DB";
            case 8080 -> "HTTP Alternate";
            case 8443 -> "HTTPS Alternate";
            case 445 -> "Windows File Sharing";
            default -> "Custom Service";
        };
    }

    private void addResult(String ip, int port, String status, String service, String description, String risk) {
        SwingUtilities.invokeLater(() ->
                tableModel.addRow(new Object[]{ip, port, status, service, description, risk})
        );
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
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
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
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
}