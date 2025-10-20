package com.example.whiteboard.server;

import com.example.whiteboard.command.Command;
import com.example.whiteboard.model.Shape;
import com.example.whiteboard.remote.ClientCallback;
import com.example.whiteboard.remote.WhiteboardService;
import com.example.whiteboard.server.cluster.PeerCommunicator;
import com.example.whiteboard.server.cluster.SerializationUtils;
import com.example.whiteboard.server.cluster.ServerInfo;
import java.io.IOException;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhiteboardServer extends UnicastRemoteObject implements WhiteboardService {
    private final WhiteboardState whiteboardState = new WhiteboardState();
    // This line now correctly matches the new CommandManager constructor
    private  CommandManager commandManager;
    private final List<ClientCallback> clients = Collections.synchronizedList(new ArrayList<>());
    private final List<ServerInfo> peers; // List of all servers in the cluster
    private final ServerInfo myInfo;      // This server's own info

    public WhiteboardServer(List<ServerInfo> peers, ServerInfo myInfo) throws RemoteException {
        super();
        this.peers = peers;
        this.myInfo = myInfo;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }


    @Override
    public void executeCommand(Command command) throws RemoteException {
        // 1. Execute the command on the leader's local state first.
        commandManager.executeCommand(command);
        System.out.println("Leader executed command. Replicating to backups...");

        // 2. Serialize the command and replicate it to all backup servers.
        try {
            String serializedCommand = SerializationUtils.serialize(command);
            String message = "REPLICATE:" + serializedCommand;

            for (ServerInfo peer : peers) {
                if (peer.getId() != myInfo.getId()) { // Don't send to myself
                    PeerCommunicator.sendMessage(peer, message);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to serialize or replicate command.");
            e.printStackTrace();
            // In a real system, you might need a rollback mechanism here.
        }

        // 3. Finally, notify all connected clients to update their view.
        System.out.println("Replication complete. Notifying clients...");
        notifyClients();
    }

    @Override
    public List<Shape> getShapes() throws RemoteException {
        // FIX: Instead of accessing a local state, get the shapes
        // from the CommandManager, which is the single source of truth.
        return commandManager.getShapes();
    }

    @Override
    public void registerClient(ClientCallback client) throws RemoteException {
        clients.add(client);
        System.out.println("New client registered. Total clients: " + clients.size());
    }

    @Override
    public void unregisterClient(ClientCallback client) throws RemoteException {
        clients.remove(client);
        System.out.println("Client unregistered. Total clients: " + clients.size());
    }

    @Override
    public void undo() throws RemoteException {
        // Delegate the action to the command manager
        commandManager.undo();
        System.out.println("Undo action performed. Notifying clients...");
        // Notify all clients of the state change
        notifyClients();
    }

    @Override
    public void redo() throws RemoteException {
        // Delegate the action to the command manager
        commandManager.redo();
        System.out.println("Redo action performed. Notifying clients...");
        // Notify all clients of the state change
        notifyClients();
    }

    private void notifyClients() {
        // Create a copy to iterate over to prevent ConcurrentModificationException
        for (ClientCallback client : new ArrayList<>(clients)) {
            try {
                client.updateView();
            } catch (RemoteException e) {
                // Client is likely disconnected, remove them
                clients.remove(client);
                System.out.println("Removed disconnected client.");
            }
        }
    }
}