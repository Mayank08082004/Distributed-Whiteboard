package com.example.whiteboard.server.cluster;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.BiConsumer; // Changed from Consumer

public class PeerCommunicator {
    private final int myPort;
    private final BiConsumer<String, Socket> messageHandler;

    public PeerCommunicator(int myPort, BiConsumer<String, Socket> messageHandler) {
        this.myPort = myPort;
        this.messageHandler = messageHandler;
    }

    // Starts a new thread to listen for incoming messages from other servers
    public void startListening() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(myPort)) {
                System.out.println("Peer listener started on port " + myPort);
                while (true) {
                    Socket clientSocket = serverSocket.accept(); // Don't auto-close
                    new Thread(() -> {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                            String message = in.readLine();
                            if (message != null) {
                                messageHandler.accept(message, clientSocket);
                            }
                        } catch(Exception e) {
                            // Handle exceptions
                        } finally {
                            try { clientSocket.close(); } catch (Exception e) {}
                        }
                    }).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static String sendRequestAndGetResponse(ServerInfo target, String message) {
        try (Socket socket = new Socket(target.getHost(), target.getPort());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println(message);
            return in.readLine(); // Wait for and return the response
        } catch (Exception e) {
            System.err.println("Failed request/response to server " + target.getId() + ": " + e.getMessage());
            return null;
        }
    }
    // Sends a message to a specific peer server
    public static boolean sendMessage(ServerInfo target, String message) {
        try (Socket socket = new Socket(target.getHost(), target.getPort());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
            return true; // Success
        } catch (Exception e) {
            System.err.println("Failed to send message to server " + target.getId() + ": " + e.getMessage());
            return false; // Failure
        }
    }
}