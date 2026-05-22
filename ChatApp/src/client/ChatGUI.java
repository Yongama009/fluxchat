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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatGUI extends JFrame {

    private final JTextArea feedArea = new JTextArea();
    private final JTextField hostField = new JTextField("localhost", 12);
    private final JTextField portField = new JTextField("5000", 5);
    private final JTextField nameField = new JTextField("Job Seeker", 12);
    private final JTextField messageField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField(14);
    private final JTextField roleField = new JTextField();
    private final JTextField skillsField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final JTextField newUserNameField = new JTextField();
    private final JTextField newUserRoleField = new JTextField();
    private final JTextField newUserSkillsField = new JTextField();
    private final JTextField newUserLocationField = new JTextField();
    private final JTextField jobTitleField = new JTextField();
    private final JTextField companyField = new JTextField();
    private final JTextField jobLocationField = new JTextField();
    private final JTextArea jobDescriptionArea = new JTextArea(4, 24);
    private final JTextField applyJobIdField = new JTextField();
    private final JTextArea applicationMessageArea = new JTextArea(4, 24);
    private final JTextField applicationsJobIdField = new JTextField();
    private final JButton connectButton = new JButton("Connect");
    private final JButton sendButton = new JButton("Send");
    private final JTabbedPane actionsTabs = new JTabbedPane();

    private Socket socket;
    private PrintWriter out;

    public ChatGUI() {
        setTitle("FluxChat Opportunities");
        setSize(1040, 680);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        feedArea.setEditable(false);
        feedArea.setLineWrap(true);
        feedArea.setWrapStyleWord(true);
        feedArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        jobDescriptionArea.setLineWrap(true);
        jobDescriptionArea.setWrapStyleWord(true);
        applicationMessageArea.setLineWrap(true);
        applicationMessageArea.setWrapStyleWord(true);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);
        add(buildComposer(), BorderLayout.SOUTH);

        setConnectedState(false);

        connectButton.addActionListener(event -> connect());
        sendButton.addActionListener(event -> sendMessage());
        messageField.addActionListener(event -> sendMessage());

        append("FluxChat is now an opportunity network.");
        append("Connect, then use the action tabs or type commands manually.");

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(245, 247, 250));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.gridy = 0;

        gbc.gridx = 0;
        panel.add(new JLabel("Name"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Host"), gbc);
        gbc.gridx = 3;
        panel.add(hostField, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Port"), gbc);
        gbc.gridx = 5;
        panel.add(portField, gbc);

        gbc.gridx = 6;
        panel.add(connectButton, gbc);

        return panel;
    }

    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JScrollPane feedScroll = new JScrollPane(feedArea);
        feedScroll.setPreferredSize(new Dimension(560, 420));
        panel.add(feedScroll, BorderLayout.CENTER);

        actionsTabs.addTab("Account", buildAccountPanel());
        actionsTabs.addTab("Profile", buildProfilePanel());
        actionsTabs.addTab("Jobs", buildJobsPanel());
        actionsTabs.addTab("Apply", buildApplyPanel());
        actionsTabs.addTab("Users", buildUsersPanel());
        actionsTabs.setPreferredSize(new Dimension(380, 420));
        panel.add(actionsTabs, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildComposer() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildAccountPanel() {
        JPanel panel = formPanel();
        addFormRow(panel, "Password", passwordField);

        JPanel buttons = buttonRow();
        buttons.add(button("Register", event -> sendCommand("/register " + password())));
        buttons.add(button("Login", event -> sendCommand("/login " + password())));

        panel.add(buttons);
        panel.add(hint("Register once per name. Use login when reconnecting with the same name."));
        return panel;
    }

    private JPanel buildProfilePanel() {
        JPanel panel = formPanel();
        addFormRow(panel, "Role", roleField);
        addFormRow(panel, "Skills", skillsField);
        addFormRow(panel, "Location", locationField);

        JPanel buttons = buttonRow();
        buttons.add(button("Save Profile", event -> sendProfile()));
        buttons.add(button("Refresh Users", event -> sendCommand("/users")));

        panel.add(buttons);
        panel.add(Box.createVerticalStrut(12));
        panel.add(sectionLabel("Add Another User"));
        addFormRow(panel, "Name", newUserNameField);
        addFormRow(panel, "Role", newUserRoleField);
        addFormRow(panel, "Skills", newUserSkillsField);
        addFormRow(panel, "Location", newUserLocationField);
        panel.add(button("Add User", event -> sendAddUser()));
        return panel;
    }

    private JPanel buildJobsPanel() {
        JPanel panel = formPanel();
        addFormRow(panel, "Title", jobTitleField);
        addFormRow(panel, "Company", companyField);
        addFormRow(panel, "Location", jobLocationField);
        addFormRow(panel, "Description", new JScrollPane(jobDescriptionArea));

        JPanel buttons = buttonRow();
        buttons.add(button("Post Job", event -> sendJobPost()));
        buttons.add(button("Refresh Jobs", event -> sendCommand("/jobs")));

        panel.add(buttons);
        return panel;
    }

    private JPanel buildApplyPanel() {
        JPanel panel = formPanel();
        addFormRow(panel, "Job ID", applyJobIdField);
        addFormRow(panel, "Message", new JScrollPane(applicationMessageArea));
        panel.add(button("Apply", event -> sendApplication()));

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
        buttons.add(button("Help", event -> sendCommand("/help")));

        panel.add(buttons);
        panel.add(hint("Results appear in the activity feed on the left."));
        return panel;
    }

    private void connect() {
        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());

            socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            setConnectedState(true);
            messageField.requestFocusInWindow();

            append("Connected to " + host + ":" + port);
            out.println(nameField.getText().trim());

            Thread receiver = new Thread(() -> receiveMessages(in));
            receiver.setDaemon(true);
            receiver.start();
        } catch (NumberFormatException e) {
            append("Port must be a number.");
        } catch (IOException e) {
            append("Connection failed: " + e.getMessage());
        }
    }

    private void receiveMessages(BufferedReader in) {
        String serverMessage;

        try {
            while ((serverMessage = in.readLine()) != null) {
                append(serverMessage);
            }
        } catch (IOException e) {
            append("Disconnected from server.");
        }
    }

    private void sendMessage() {
        sendCommandFromField(messageField);
    }

    private void sendCommandFromField(JTextField field) {
        if (out == null) {
            append("Connect before sending a message.");
            return;
        }

        String message = field.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        sendCommand(message);
        field.setText("");
    }

    private void sendCommand(String command) {
        if (out == null) {
            append("Connect before sending.");
            return;
        }

        if (command == null || command.trim().isEmpty()) {
            append("Fill in the form before sending.");
            return;
        }

        out.println(command.trim());
    }

    private void sendProfile() {
        sendCommand("/profile " + roleField.getText().trim()
                + " | " + skillsField.getText().trim()
                + " | " + locationField.getText().trim());
    }

    private void sendAddUser() {
        sendCommand("/adduser " + newUserNameField.getText().trim()
                + " | " + newUserRoleField.getText().trim()
                + " | " + newUserSkillsField.getText().trim()
                + " | " + newUserLocationField.getText().trim());
    }

    private void sendJobPost() {
        sendCommand("/post " + jobTitleField.getText().trim()
                + " | " + companyField.getText().trim()
                + " | " + jobLocationField.getText().trim()
                + " | " + jobDescriptionArea.getText().trim().replaceAll("\\s+", " "));
    }

    private void sendApplication() {
        sendCommand("/apply " + applyJobIdField.getText().trim()
                + " " + applicationMessageArea.getText().trim().replaceAll("\\s+", " "));
    }

    private void sendApplicationsQuery() {
        sendCommand("/applications " + applicationsJobIdField.getText().trim());
    }

    private String password() {
        return new String(passwordField.getPassword()).trim();
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
        JLabel label = new JLabel("<html><body style='width: 300px'>" + text + "</body></html>");
        label.setForeground(new Color(89, 99, 110));
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
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

    private void setConnectedState(boolean connected) {
        connectButton.setEnabled(!connected);
        hostField.setEnabled(!connected);
        portField.setEnabled(!connected);
        nameField.setEnabled(!connected);
        sendButton.setEnabled(connected);
        messageField.setEnabled(connected);
        actionsTabs.setEnabled(connected);
    }

    private void append(String message) {
        SwingUtilities.invokeLater(() -> {
            feedArea.append(message + System.lineSeparator());
            feedArea.setCaretPosition(feedArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatGUI::new);
    }
}
