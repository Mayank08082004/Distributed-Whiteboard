package com.example.whiteboard.client;

import com.example.whiteboard.model.Shape;
import com.example.whiteboard.remote.ClientCallback;
import com.example.whiteboard.remote.ServiceFinder;
import com.example.whiteboard.remote.WhiteboardService;
import com.example.whiteboard.server.WhiteboardState;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ClientMain {

    public static void main(String[] args) {
        try {
            System.out.println("Client starting...");

            // 1. Connect to the Load Balancer on its unique port (1100)
            System.out.println("Connecting to Load Balancer to find the leader...");
            Registry lbRegistry = LocateRegistry.getRegistry("localhost", 1100);
            ServiceFinder finder = (ServiceFinder) lbRegistry.lookup("ServiceFinder");

            // 2. Ask the Load Balancer for the current leader's service stub
            WhiteboardService serverService = finder.findWhiteboardService();
            System.out.println("Successfully obtained leader's service from Load Balancer.");

            // The client's local state object
            WhiteboardState clientState = new WhiteboardState();
            // Get the initial state from the server
            clientState.addAllShapes(serverService.getShapes());

            MainView view = new MainView(clientState);

            // We have a circular dependency: Controller needs the Callback, and Callback needs the Controller.
            // We solve this by creating them and then linking them together.

            // a. Create the controller (pass null for the callback stub for now)
            CanvasController controller = new CanvasController(serverService, view, null);

            // b. Create the callback and give it the controller
            ClientCallbackImpl callback = new ClientCallbackImpl(view.getCanvasPanel(), clientState, controller);
            ClientCallback stub = (ClientCallback) UnicastRemoteObject.exportObject(callback, 0);

            // c. Now give the controller the callback stub it needs
            controller.setClientCallbackStub(stub);

            // d. Register with the initial leader
            serverService.registerClient(stub);

            // --- Chat client setup ---
            ChatClient chatClient = new ChatClient("localhost", 59001, view.chatArea, view.userListModel, view.typingStatusLabel);

            view.getChatInput().addActionListener(e -> {
                String message = view.getChatInput().getText();
                if (!message.isEmpty()) {
                    chatClient.sendMessage(message);
                    view.getChatInput().setText("");
                }
            });

            // "Is typing" feature setup
            Timer typingTimer = new Timer(2000, e -> chatClient.sendTypingStop());
            typingTimer.setRepeats(false);

            view.getChatInput().getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { changed(); }
                public void removeUpdate(DocumentEvent e) { /* Do nothing */ }
                public void changedUpdate(DocumentEvent e) { /* Do nothing for styled docs */ }
                private void changed() {
                    chatClient.sendTypingStart();
                    typingTimer.restart();
                }
            });

            // Connect to the chat server in a background thread
            new Thread(chatClient::connect).start();

            SwingUtilities.invokeLater(() -> view.setVisible(true));

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to connect to the server. Is the server cluster running?", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Inner class for the client callback implementation.
     * This is the object the server calls to notify the client of updates.
     */
    static class ClientCallbackImpl implements ClientCallback {
        private final CanvasPanel canvasPanel;
        private final WhiteboardState localState;
        private final CanvasController controller; // Holds the single source of truth for the server connection

        public ClientCallbackImpl(CanvasPanel panel, WhiteboardState state, CanvasController controller) {
            this.canvasPanel = panel;
            this.localState = state;
            this.controller = controller;
        }

        @Override
        public void updateView() {
            // Use SwingWorker to fetch data on a background thread to avoid freezing the UI
            new SwingWorker<List<Shape>, Void>() {
                @Override
                protected List<Shape> doInBackground() throws RemoteException {
                    // Always get the CURRENT, valid service from the controller,
                    // which handles reconnection logic.
                    return controller.getWhiteboardService().getShapes();
                }

                @Override
                protected void done() {
                    try {
                        // This runs on the UI thread after doInBackground is finished
                        List<Shape> newShapes = get(); // Get the result from the background task
                        localState.clearShapes();
                        localState.addAllShapes(newShapes);
                        canvasPanel.repaint(); // Now repaint with the fresh state
                    } catch (InterruptedException | ExecutionException e) {
                        // If an error happens here (like a ConnectException during failover),
                        // it will trigger the controller's recovery logic.
                        System.err.println("Error during view update, attempting recovery...");
                        controller.handleAndRecover(e);
                    }
                }
            }.execute();
        }
    }
}