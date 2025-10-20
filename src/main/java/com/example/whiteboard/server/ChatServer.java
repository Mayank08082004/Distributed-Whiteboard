package com.example.whiteboard.server;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    // Use a thread-safe map to store client names and their writers.
    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    private static class Handler implements Runnable {
        private final Socket socket;
        private String name;
        private PrintWriter out;
        private Scanner in;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client. Keep requesting until a unique one is provided.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null || name.isBlank() || clients.containsKey(name)) {
                        out.println("INVALIDNAME");
                    } else {
                        clients.put(name, out);
                        out.println("NAMEACCEPTED " + name);
                        System.out.println(name + " has joined.");
                        break;
                    }
                }

                // Announce the new user and broadcast the updated user list.
                broadcast("MESSAGE " + name + " has joined the chat.");
                broadcastUserList();

                // Process messages from this client.
                while (in.hasNextLine()) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/w ")) {
                        handleWhisper(input);
                    } else if (input.equals("TYPING_START")) {
                        broadcast("STATUS " + name + " is typing...");
                    } else if (input.equals("TYPING_STOP")) {
                        broadcast("STATUS "); // Clear status
                    } else {
                        broadcast("MESSAGE " + name + ": " + input);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error with client " + name + ": " + e);
            } finally {
                if (name != null) {
                    System.out.println(name + " has left.");
                    clients.remove(name);
                    broadcast("MESSAGE " + name + " has left the chat.");
                    broadcastUserList();
                }
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        private void handleWhisper(String input) {
            String[] parts = input.split(" ", 3);
            if (parts.length < 3) {
                out.println("SYSTEM Usage: /w <username> <message>");
                return;
            }
            String targetUser = parts[1];
            String message = parts[2];
            PrintWriter targetWriter = clients.get(targetUser);
            if (targetWriter != null) {
                targetWriter.println("WHISPER from " + name + ": " + message);
                out.println("WHISPER to " + targetUser + ": " + message);
            } else {
                out.println("SYSTEM User '" + targetUser + "' not found.");
            }
        }

        private void broadcast(String message) {
            for (PrintWriter writer : clients.values()) {
                writer.println(message);
            }
        }

        private void broadcastUserList() {
            String userList = String.join(",", clients.keySet());
            broadcast("USERLIST " + userList);
        }
    }
}