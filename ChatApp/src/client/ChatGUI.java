package client;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private final JButton connectButton = new JButton("Connect");
    private final JButton sendButton = new JButton("Send");

    private Socket socket;
    private PrintWriter out;

    public ChatGUI() {
        setTitle("FluxChat Opportunities");
        setSize(720, 560);
        setMinimumSize(new Dimension(620, 460));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        feedArea.setEditable(false);
        feedArea.setLineWrap(true);
        feedArea.setWrapStyleWord(true);
        feedArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(feedArea), BorderLayout.CENTER);
        add(buildComposer(), BorderLayout.SOUTH);

        sendButton.setEnabled(false);
        messageField.setEnabled(false);

        connectButton.addActionListener(event -> connect());
        sendButton.addActionListener(event -> sendMessage());
        messageField.addActionListener(event -> sendMessage());

        append("FluxChat is now an opportunity network.");
        append("Use /help after connecting to see career commands.");
        append("Example: /profile Junior Java Developer | Java, SQL | Johannesburg");
        append("Example: /adduser Thabo | Recruiter | Hiring, interviews | Cape Town");
        append("Example: /post Support Engineer | Acme | Remote | Help customers succeed");

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

    private JPanel buildComposer() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
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

            connectButton.setEnabled(false);
            hostField.setEnabled(false);
            portField.setEnabled(false);
            nameField.setEnabled(false);
            sendButton.setEnabled(true);
            messageField.setEnabled(true);
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
        if (out == null) {
            append("Connect before sending a message.");
            return;
        }

        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        out.println(message);
        messageField.setText("");
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
