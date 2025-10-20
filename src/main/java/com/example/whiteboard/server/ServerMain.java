package com.example.whiteboard.server;

import com.example.whiteboard.server.cluster.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMain {
    private static WhiteboardServer rmiServer;
    private static Thread heartbeatThread;
    private static Thread autoSaveThread; // Added to manage the save thread
    private static final Set<Integer> deadPeers = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ServerMain <my-server-id>");
            System.exit(1);
        }

        try {
            int myId = Integer.parseInt(args[0]);
            List<ServerInfo> allServers = ClusterConfig.readConfig("servers.config");
            ServerInfo myInfo = allServers.stream().filter(s -> s.getId() == myId).findFirst().orElseThrow();

            System.out.println("Starting Server " + myId + "...");

            WhiteboardState whiteboardState = new WhiteboardState();
            CommandManager commandManager = new CommandManager(whiteboardState);

            ElectionManager electionManager = new ElectionManager(myInfo, allServers, commandManager);
            FailureDetector failureDetector = new FailureDetector(electionManager);
            electionManager.setFailureDetector(failureDetector);

            PeerCommunicator communicator = new PeerCommunicator(myInfo.getPort(), electionManager::handleMessage);
            communicator.startListening();

            failureDetector.startMonitoring();
            electionManager.startElection();

            AtomicBoolean amILeader = new AtomicBoolean(false);
            Thread clockSyncThread = null;

            // --- MAIN SERVER LOOP ---
            while (true) {
                Thread.sleep(3000);

                ServerInfo currentLeader = electionManager.getLeader();
                boolean shouldBeLeader = (currentLeader != null && currentLeader.getId() == myId);

                if (shouldBeLeader && !amILeader.get()) {
                    // --- Promotion to LEADER ---
                    System.out.println("Transitioning to LEADER role.");
                    if (clockSyncThread != null) clockSyncThread.interrupt();
                    amILeader.set(true);
                    startLeaderDuties(allServers, myInfo, commandManager);
                } else if (!shouldBeLeader && amILeader.get()) {
                    // --- Demotion to BACKUP ---
                    System.out.println("Transitioning to BACKUP role.");
                    amILeader.set(false);
                    stopLeaderDuties();
                }

                // If I am a backup, ensure the clock sync thread is running.
                if (!amILeader.get() && (clockSyncThread == null || !clockSyncThread.isAlive())) {
                    clockSyncThread = new Thread(() -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                Thread.sleep(30000); // Sync every 30 seconds
                                ServerInfo leader = electionManager.getLeader();
                                if (leader != null && leader.getId() != myId) {
                                    synchronizeClock(leader);
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        System.out.println("Clock sync thread for Server " + myId + " stopped.");
                    });
                    clockSyncThread.start();
                }
            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void synchronizeClock(ServerInfo leader) {
        long T_start = System.currentTimeMillis();
        String response = PeerCommunicator.sendRequestAndGetResponse(leader, "TIME_REQUEST");
        long T_end = System.currentTimeMillis();

        if (response != null && response.startsWith("TIME_RESPONSE:")) {
            try {
                long T_leader = Long.parseLong(response.substring(14));
                long RTT = T_end - T_start;
                long synchronizedTime = T_leader + (RTT / 2);
                long offset = synchronizedTime - T_end;

                System.out.println("Clock synchronized with leader. Offset: " + offset + "ms");
            } catch (NumberFormatException e) {
                System.err.println("Invalid time response from leader: " + response);
            }
        }
    }

    private static void startLeaderDuties(List<ServerInfo> allServers, ServerInfo myInfo, CommandManager commandManager) {
        try {
            System.out.println("I am the new leader. Loading state from file...");
            commandManager.loadStateFromFile("whiteboard.save");

            if (rmiServer == null) {
                rmiServer = new WhiteboardServer(allServers, myInfo);
                rmiServer.setCommandManager(commandManager);
            }

            try {
                LocateRegistry.createRegistry(1099);
                System.out.println("RMI registry created on port 1099.");
            } catch (RemoteException e) {
                System.out.println("RMI registry already running.");
            }

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            registry.rebind("WhiteboardService", rmiServer);
            System.out.println("I am the leader. RMI service is bound.");

            // Start the auto-save thread
            autoSaveThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(60000); // Save every 60 seconds
                        commandManager.saveStateToFile("whiteboard.save");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("Auto-save thread stopped.");
            });
            autoSaveThread.start();

            // Start the heartbeat thread
            heartbeatThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(2000);
                        for (ServerInfo peer : allServers) {
                            if (peer.getId() == myInfo.getId() || deadPeers.contains(peer.getId())) {
                                continue;
                            }
                            boolean success = PeerCommunicator.sendMessage(peer, "HEARTBEAT:" + myInfo.getId());
                            if (!success) {
                                System.out.println("Heartbeat to Server " + peer.getId() + " failed. Marking as presumed dead.");
                                deadPeers.add(peer.getId());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("Heartbeat thread stopped.");
            });
            heartbeatThread.start();
        } catch (Exception e) {
            System.err.println("Error starting leader duties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void stopLeaderDuties() {
        // Stop the heartbeat thread
        if (heartbeatThread != null && heartbeatThread.isAlive()) {
            heartbeatThread.interrupt();
        }
        // Stop the auto-save thread
        if (autoSaveThread != null && autoSaveThread.isAlive()) {
            autoSaveThread.interrupt();
        }

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            registry.unbind("WhiteboardService");
            System.out.println("I am no longer the leader. RMI service unbound.");

            if (rmiServer != null) {
                UnicastRemoteObject.unexportObject(rmiServer, true);
                rmiServer = null;
                System.out.println("RMI server object unexported.");
            }

        } catch (Exception e) {
            System.err.println("Could not unbind or unexport RMI service: " + e.getMessage());
        }
    }
}