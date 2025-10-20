package com.example.whiteboard.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceFinder extends Remote {
    /**
     * Finds the current leader's WhiteboardService and returns its RMI stub.
     * @return The remote stub for the active WhiteboardService.
     * @throws RemoteException if a leader is not currently available.
     */
    WhiteboardService findWhiteboardService() throws RemoteException;
}