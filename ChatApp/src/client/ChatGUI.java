package client;

import javax.swing.*;
import java.awt.*;

public class ChatGUI extends JFrame {

    JTextArea chatArea;
    JTextField messageField;
    JButton sendButton;

    public ChatGUI() {

        setTitle("Java Chat App");

        setSize(400, 500);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();

        chatArea.setEditable(false);

        JScrollPane scrollPane =
                new JScrollPane(chatArea);

        messageField = new JTextField();

        sendButton = new JButton("Send");

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(messageField, BorderLayout.CENTER);

        panel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);

        add(panel, BorderLayout.SOUTH);

        setVisible(true);
    }

    public static void main(String[] args) {

        new ChatGUI();
    }
}