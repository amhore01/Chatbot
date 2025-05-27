package org.aryanoor.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.aryanoor.services.IAM;
import org.aryanoor.services.OpenRouterChat;
import java.util.*;
import java.nio.file.*;
import java.util.List;

public class GUI extends JFrame {
    private String apiUrl;
    private String apiKey;
    private static final String CONFIG_FILE = "config.properties";

    private JPanel currentPanel;
    private CardLayout cardLayout;
    private JTextArea chatArea;
    private JTextField inputField;
    private OpenRouterChat chatBot;

    public GUI() {
        setTitle("Chatbot");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            loadConfig();
            chatBot = new OpenRouterChat(apiUrl, apiKey);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading configuration: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        cardLayout = new CardLayout();
        currentPanel = new JPanel(cardLayout);

        // Create panels
        JPanel registerPanel = createRegisterPanel();
        JPanel loginPanel = createLoginPanel();
        JPanel chatPanel = createChatPanel();

        currentPanel.add(registerPanel, "register");
        currentPanel.add(loginPanel, "login");
        currentPanel.add(chatPanel, "chat");

        add(currentPanel);

        // Check if user needs to register
        if (!Files.exists(Paths.get("user.data"))) {
            cardLayout.show(currentPanel, "register");
        } else {
            cardLayout.show(currentPanel, "login");
        }

        setVisible(true);
    }

    private void loadConfig() throws IOException {
        Properties properties = new Properties();
        if (Files.exists(Paths.get(CONFIG_FILE))) {
            List<String> lines = Files.readAllLines(Paths.get(CONFIG_FILE));
            for (String line : lines) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    if (parts[0].trim().equalsIgnoreCase("apiUrl")) {
                        apiUrl = parts[1].trim();
                    } else if (parts[0].trim().equalsIgnoreCase("apiKey")) {
                        apiKey = parts[1].trim();
                    }
                }
            }
        } else {
            throw new IOException("Configuration file not found. Please create 'config.properties' with apiUrl and apiKey.");
        }
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("User Registration", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                IAM newUser = new IAM(username, password);
                newUser.signUp();
                JOptionPane.showMessageDialog(this, "Registration successful!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(currentPanel, "login");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(titleLabel);
        panel.add(inputPanel);
        panel.add(registerButton);

        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Login", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            try {
                IAM user = new IAM(username, password);
                if (user.login(username, password)) {
                    cardLayout.show(currentPanel, "chat");
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Login failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(titleLabel);
        panel.add(inputPanel);
        panel.add(loginButton);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);

        // Welcome message
        chatArea.append("Welcome to the chatbot! Type your questions below.\n");
        chatArea.append("Type 'exit' to quit.\n\n");

        return panel;
    }

    private void sendMessage() {
        String question = inputField.getText().trim();
        inputField.setText("");

        if (question.equalsIgnoreCase("exit")) {
            dispose();
            System.exit(0);
        }

        if (!question.isEmpty()) {
            chatArea.append("You: " + question + "\n");
            chatArea.append("Bot is thinking...\n");

            new Thread(() -> {
                try {
                    String response = chatBot.sendChatRequest(question);
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append("Bot: " + response + "\n\n");
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append("Error: " + e.getMessage() + "\n\n");
                    });
                }
            }).start();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GUI();
        });
    }
}