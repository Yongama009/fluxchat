package client;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatGUI extends JFrame {
    private static final String AUTH_VIEW = "auth";
    private static final String APP_VIEW = "app";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JTextArea feedArea = new JTextArea();
    private final JLabel authStatusLabel = new JLabel(" ");
    private final JTextField hostField = new JTextField("localhost", 12);
    private final JTextField portField = new JTextField("5000", 5);
    private final JTextField firstNameField = new JTextField();
    private final JTextField lastNameField = new JTextField();
    private final JTextField idNumberField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final JTextField roleField = new JTextField();
    private final JTextField skillsField = new JTextField();
    private final JTextArea educationArea = new JTextArea(3, 24);
    private final JTextArea experienceArea = new JTextArea(3, 24);
    private final JPasswordField registerPasswordField = new JPasswordField();
    private final JTextField loginIdField = new JTextField();
    private final JPasswordField loginPasswordField = new JPasswordField();
    private final JTextField messageField = new JTextField();
    private final JTextField jobTitleField = new JTextField();
    private final JTextField companyField = new JTextField();
    private final JTextField jobLocationField = new JTextField();
    private final JTextField jobSourceUrlField = new JTextField();
    private final JTextArea jobDescriptionArea = new JTextArea(4, 24);
    private final JTextField applyJobIdField = new JTextField();
    private final JTextArea applicationMessageArea = new JTextArea(4, 24);
    private final JTextField applicationsJobIdField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private Socket socket;
    private PrintWriter out;
    private boolean signedIn;

    public ChatGUI() {
        setTitle("FluxChat Opportunities");
        setSize(1040, 720);
        setMinimumSize(new Dimension(920, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        configureTextAreas();
        root.add(buildAuthView(), AUTH_VIEW);
        root.add(buildAppView(), APP_VIEW);
        add(root);

        cards.show(root, AUTH_VIEW);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void configureTextAreas() {
        feedArea.setEditable(false);
        feedArea.setLineWrap(true);
        feedArea.setWrapStyleWord(true);
        feedArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        educationArea.setLineWrap(true);
        educationArea.setWrapStyleWord(true);
        experienceArea.setLineWrap(true);
        experienceArea.setWrapStyleWord(true);
        jobDescriptionArea.setLineWrap(true);
        jobDescriptionArea.setWrapStyleWord(true);
        applicationMessageArea.setLineWrap(true);
        applicationMessageArea.setWrapStyleWord(true);
    }

    private JPanel buildAuthView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel connection = new JPanel();
        connection.add(new JLabel("Host"));
        connection.add(hostField);
        connection.add(new JLabel("Port"));
        connection.add(portField);
        panel.add(connection, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Register", buildRegisterPanel());
        tabs.addTab("Login", buildLoginPanel());
        panel.add(tabs, BorderLayout.CENTER);
        authStatusLabel.setForeground(new Color(128, 48, 48));
        authStatusLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(authStatusLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = formPanel();
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 8));
        addGridField(grid, "First Name", firstNameField);
        addGridField(grid, "Last Name", lastNameField);
        addGridField(grid, "SA ID Number", idNumberField);
        addGridField(grid, "Email", emailField);
        addGridField(grid, "Phone", phoneField);
        addGridField(grid, "Location", locationField);
        addGridField(grid, "Target Role", roleField);
        addGridField(grid, "Skills", skillsField);
        addGridField(grid, "Password", registerPasswordField);
        panel.add(grid);

        addFormRow(panel, "Education", new JScrollPane(educationArea));
        addFormRow(panel, "Experience", new JScrollPane(experienceArea));
        panel.add(button("Create Account and CV", event -> register()));
        panel.add(hint("Password needs 8+ characters with uppercase, lowercase, digit, and special character. Registration validates required fields, duplicate ID numbers, and the South African ID date/checksum."));
        return panel;
    }

    private JPanel buildLoginPanel() {
        JPanel panel = formPanel();
        addFormRow(panel, "SA ID Number", loginIdField);
        addFormRow(panel, "Password", loginPasswordField);
        panel.add(button("Login", event -> login()));
        return panel;
    }

    private JPanel buildAppView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(feedArea), BorderLayout.CENTER);
        panel.add(buildActions(), BorderLayout.EAST);
        panel.add(buildComposer(), BorderLayout.SOUTH);
        return panel;
    }

    private JTabbedPane buildActions() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Jobs", buildJobsPanel());
        tabs.addTab("Apply", buildApplyPanel());
        tabs.addTab("Users", buildUsersPanel());
        tabs.setPreferredSize(new Dimension(390, 460));
        return tabs;
    }

    private JPanel buildJobsPanel() {
        JPanel panel = formPanel();
        addFormRow(panel, "Title", jobTitleField);
        addFormRow(panel, "Company", companyField);
        addFormRow(panel, "Location", jobLocationField);
        addFormRow(panel, "Source Link", jobSourceUrlField);
        addFormRow(panel, "Description", new JScrollPane(jobDescriptionArea));

        JPanel buttons = buttonRow();
        buttons.add(button("Post Job", event -> sendJobPost()));
        buttons.add(button("Refresh Jobs", event -> sendCommand("/jobs")));
        buttons.add(button("My Matches", event -> sendCommand("/matches")));
        panel.add(buttons);
        panel.add(hint("Jobs with suspicious wording, shortened links, invalid links, or mismatched company domains are rejected."));
        return panel;
    }

    private JPanel buildApplyPanel() {
        JPanel panel = formPanel();
        addFormRow(panel, "Job ID", applyJobIdField);
        addFormRow(panel, "Message", new JScrollPane(applicationMessageArea));
        panel.add(button("Apply with My CV", event -> sendApplication()));
        panel.add(Box.createVerticalStrut(12));
        panel.add(sectionLabel("View Applications"));
        addFormRow(panel, "Job ID", applicationsJobIdField);
        panel.add(button("Show Applications", event -> sendApplicationsQuery()));
        return panel;
    }

    private JPanel buildUsersPanel() {
        JPanel panel = formPanel();
        JPanel buttons = buttonRow();
        buttons.add(button("List Users", event -> sendCommand("/users")));
        buttons.add(button("List Jobs", event -> sendCommand("/jobs")));
        buttons.add(button("My Matches", event -> sendCommand("/matches")));
        buttons.add(button("Help", event -> sendCommand("/help")));
        panel.add(buttons);
        panel.add(hint("Applications include the applicant CV summary automatically."));
        return panel;
    }

    private JPanel buildComposer() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        messageField.addActionListener(event -> sendMessage());
        sendButton.addActionListener(event -> sendMessage());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        return panel;
    }

    private void register() {
        if (!connectIfNeeded()) {
            return;
        }

        sendCommand("/registercv " + value(firstNameField)
                + " | " + value(lastNameField)
                + " | " + value(idNumberField)
                + " | " + value(emailField)
                + " | " + value(phoneField)
                + " | " + value(locationField)
                + " | " + value(roleField)
                + " | " + value(skillsField)
                + " | " + text(educationArea)
                + " | " + text(experienceArea)
                + " | " + password(registerPasswordField));
    }

    private void login() {
        if (!connectIfNeeded()) {
            return;
        }

        sendCommand("/loginid " + value(loginIdField) + " " + password(loginPasswordField));
    }

    private boolean connectIfNeeded() {
        if (out != null) {
            return true;
        }

        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("Guest");

            Thread receiver = new Thread(() -> receiveMessages(in));
            receiver.setDaemon(true);
            receiver.start();
            return true;
        } catch (NumberFormatException e) {
            showAuthError("Port must be a number.");
        } catch (IOException e) {
            showAuthError("Connection failed: " + e.getMessage());
        }

        return false;
    }

    private void receiveMessages(BufferedReader in) {
        String serverMessage;

        try {
            while ((serverMessage = in.readLine()) != null) {
                String message = serverMessage;
                SwingUtilities.invokeLater(() -> handleServerMessage(message));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> append("Disconnected from server."));
        }
    }

    private void handleServerMessage(String message) {
        append(message);
        if (message.startsWith("Account registered and CV profile completed")
                || message.startsWith("Logged in as ")) {
            signedIn = true;
            cards.show(root, APP_VIEW);
            sendCommand("/matches");
        } else if (!signedIn && isUsefulAuthMessage(message)) {
            authStatusLabel.setText(message);
        }
    }

    private void showAuthError(String message) {
        authStatusLabel.setText(message);
    }

    private boolean isUsefulAuthMessage(String message) {
        return !message.equals("Enter your name:")
                && !message.startsWith("Welcome,")
                && !message.startsWith("FluxChat")
                && !message.equals("Commands:")
                && !message.startsWith("/")
                && !message.startsWith("Send any other text");
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            sendCommand(message);
            messageField.setText("");
        }
    }

    private void sendJobPost() {
        sendCommand("/post " + value(jobTitleField)
                + " | " + value(companyField)
                + " | " + value(jobLocationField)
                + " | " + text(jobDescriptionArea)
                + " | " + value(jobSourceUrlField));
    }

    private void sendApplication() {
        sendCommand("/apply " + value(applyJobIdField) + " " + text(applicationMessageArea));
    }

    private void sendApplicationsQuery() {
        sendCommand("/applications " + value(applicationsJobIdField));
    }

    private void sendCommand(String command) {
        if (out == null) {
            append("Log in or register first.");
            return;
        }

        if (command == null || command.trim().isEmpty()) {
            append("Fill in the form before sending.");
            return;
        }

        out.println(command.trim());
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private JPanel buttonRow() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private JButton button(String label, ActionListener action) {
        JButton button = new JButton(label);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.addActionListener(action);
        return button;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel hint(String text) {
        JLabel label = new JLabel("<html><body style='width: 330px'>" + text + "</body></html>");
        label.setForeground(new Color(89, 99, 110));
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private void addGridField(JPanel panel, String label, JTextField field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void addFormRow(JPanel panel, String label, java.awt.Component field) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(fieldLabel);

        if (field instanceof JTextField textField) {
            textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        }

        if (field instanceof JScrollPane scrollPane) {
            scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        }

        panel.add(field);
        panel.add(Box.createVerticalStrut(8));
    }

    private String value(JTextField field) {
        return field.getText().trim().replaceAll("\\s+", " ");
    }

    private String text(JTextArea area) {
        return area.getText().trim().replaceAll("\\s+", " ");
    }

    private String password(JPasswordField field) {
        return new String(field.getPassword()).trim();
    }

    private void append(String message) {
        feedArea.append(message + System.lineSeparator());
        feedArea.setCaretPosition(feedArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatGUI::new);
    }
}
