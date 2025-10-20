package com.example.whiteboard.loadbalancer;

import com.example.whiteboard.remote.ServiceFinder;
import com.example.whiteboard.remote.WhiteboardService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class LoadBalancer implements ServiceFinder {

    // This holds the cached RMI stub for the current leader's service.
    // 'volatile' ensures changes are visible across threads.
    private volatile WhiteboardService leaderStub;

    public LoadBalancer() {
        // Start a background thread that continuously polls the server cluster's
        // RMI registry to discover who the current leader is.
        new Thread(this::discoverLeaderLoop).start();
    }

    /**
     * This is the remote method that clients will call.
     * It returns the cached stub for the leader.
     */
    @Override
    public WhiteboardService findWhiteboardService() throws RemoteException {
        if (leaderStub == null) {
            // If we haven't found a leader yet, tell the client to try again.
            throw new RemoteException("Leader is not currently available. Please try again in a moment.");
        }
        return leaderStub;
    }

    /**
     * This method runs in an infinite loop to poll for the leader.
     */
    private void discoverLeaderLoop() {
        while (true) {
            try {
                // Connect to the SERVER CLUSTER's RMI registry on port 1099.
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                // Look up the service. Only the leader will have bound it.
                this.leaderStub = (WhiteboardService) registry.lookup("WhiteboardService");
                System.out.println("Load Balancer: Discovered and cached the current leader's service.");
            } catch (Exception e) {
                // This will happen if the leader is down or during a re-election. It's normal.
                this.leaderStub = null;
                System.err.println("Load Balancer: Could not find the leader service. Will retry... (" + e.getMessage() + ")");
            }

            try {
                // Wait 5 seconds before checking again.
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void main(String[] args) {
        try {
            LoadBalancer lb = new LoadBalancer();
            ServiceFinder stub = (ServiceFinder) UnicastRemoteObject.exportObject(lb, 0);

            // The Load Balancer creates and binds to its OWN RMI registry on a DIFFERENT port.
            // This is crucial to avoid conflicts. We'll use port 1100.
            Registry registry = LocateRegistry.createRegistry(1100);
            registry.bind("ServiceFinder", stub);

            System.out.println("Load Balancer is running on RMI port 1100...");
        } catch (Exception e) {
            System.err.println("Load Balancer exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}