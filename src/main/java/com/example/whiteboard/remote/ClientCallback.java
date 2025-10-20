package com.example.whiteboard.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void updateView() throws RemoteException; // Server calls this to tell client to repaint
}