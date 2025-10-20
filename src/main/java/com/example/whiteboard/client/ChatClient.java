package com.example.whiteboard.client;

import javax.swing.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class ChatClient {
    private final String serverAddress;
    private final int serverPort;
    private final JTextArea chatArea;
    private final DefaultListModel<String> userListModel;
    private final JLabel typingStatusLabel;
    private PrintWriter writer;

    public ChatClient(String serverAddress, int serverPort, JTextArea chatArea, DefaultListModel<String> userListModel, JLabel typingStatusLabel) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.chatArea = chatArea;
        this.userListModel = userListModel;
        this.typingStatusLabel = typingStatusLabel;
    }

    public void connect() {
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            Scanner scanner = new Scanner(socket.getInputStream());
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Thread for reading server messages
            new Thread(() -> {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    parseServerMessage(line);
                }
            }).start();
        } catch (Exception e) {
            handleException("Error connecting to chat server.", e);
        }
    }

    private void parseServerMessage(String message) {
        // All UI updates must happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("SUBMITNAME")) {
                String name = JOptionPane.showInputDialog(null, "Enter your name:", "Name Required", JOptionPane.PLAIN_MESSAGE);
                writer.println(name);
            } else if (message.startsWith("INVALIDNAME")) {
                JOptionPane.showMessageDialog(null, "This name is already taken or invalid.", "Name Error", JOptionPane.ERROR_MESSAGE);
            } else if (message.startsWith("NAMEACCEPTED")) {
                chatArea.append("You have joined the chat as " + message.substring(13) + ".\n");
            } else if (message.startsWith("USERLIST ")) {
                userListModel.clear();
                Arrays.stream(message.substring(9).split(",")).forEach(userListModel::addElement);
            } else if (message.startsWith("MESSAGE ")) {
                chatArea.append(message.substring(8) + "\n");
            } else if (message.startsWith("WHISPER ")) {
                chatArea.append("[Private] " + message.substring(8) + "\n");
            } else if (message.startsWith("SYSTEM ")) {
                chatArea.append("[Server] " + message.substring(7) + "\n");
            } else if (message.startsWith("STATUS ")) {
                typingStatusLabel.setText(message.substring(7));
            }
        });
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // Separate methods for typing status for clarity
    public void sendTypingStart() {
        if (writer != null) {
            writer.println("TYPING_START");
        }
    }

    public void sendTypingStop() {
        if (writer != null) {
            writer.println("TYPING_STOP");
        }
    }

    private void handleException(String message, Exception e) {
        e.printStackTrace();
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }
}