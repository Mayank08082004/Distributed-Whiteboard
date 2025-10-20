package com.example.whiteboard.remote;

import com.example.whiteboard.command.Command;
import com.example.whiteboard.model.Shape;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WhiteboardService extends Remote {
    // Client sends a command for the server to execute
    void executeCommand(Command command) throws RemoteException;

    // Used when a new client joins to get the current state
    List<Shape> getShapes() throws RemoteException;

    // Methods for clients to join and leave the session
    void registerClient(ClientCallback client) throws RemoteException;
    void unregisterClient(ClientCallback client) throws RemoteException;
    void undo() throws RemoteException;
    void redo() throws RemoteException;
}