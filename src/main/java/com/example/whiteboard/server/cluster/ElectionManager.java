package com.example.whiteboard.server.cluster;

import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.example.whiteboard.command.Command; // Import Command
import com.example.whiteboard.server.CommandManager; // Import CommandManager
import com.example.whiteboard.server.cluster.SerializationUtils; // Import our new utility
import java.io.PrintWriter;
public class ElectionManager {
    private final ServerInfo myInfo;
    private final List<ServerInfo> allServers;
    private ServerInfo leader;
    private boolean isElectionInProgress = false;
    private final CommandManager commandManager; // Each server needs a command manager
    private FailureDetector failureDetector;
    public ElectionManager(ServerInfo myInfo, List<ServerInfo> allServers, CommandManager commandManager) {
        this.myInfo = myInfo;
        this.allServers = allServers;
        this.commandManager = commandManager; // Store the command manager
    }

    public ServerInfo getLeader() {
        return this.leader;
    }

    public synchronized void startElection() {
        if (isElectionInProgress) return;
        System.out.println("Server " + myInfo.getId() + " is starting an election.");
        isElectionInProgress = true;
        sendToNextAvailablePeer("ELECTION:" + myInfo.getId());
    }

    public void setFailureDetector(FailureDetector detector) {
        this.failureDetector = detector;
    }

    public boolean amITheLeader() {
        return leader != null && leader.getId() == myInfo.getId();
    }
    public synchronized void handleMessage(String message, Socket requesterSocket) {
        String[] parts = message.split(":", 2);
        String type = parts[0];

        if ("TIME_REQUEST".equals(type)) {
            // This is the leader's role: respond with the current time
            try (PrintWriter out = new PrintWriter(requesterSocket.getOutputStream(), true)) {
                long currentTime = System.currentTimeMillis();
                out.println("TIME_RESPONSE:" + currentTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return; // Request handled
        }
        String payload = (parts.length > 1) ? parts[1] : "";
        if ("HEARTBEAT".equals(type)) {
            if (failureDetector != null) {
                failureDetector.heartbeatReceived();
            }
            return; // Don't process any further
        }

        if ("ELECTION".equals(type)) {
            if (payload.contains(String.valueOf(myInfo.getId()))) {
                // The message has come full circle. I am the initiator.
                // Determine the leader (highest ID) and announce it.
                int leaderId = Arrays.stream(payload.split(","))
                        .mapToInt(Integer::parseInt)
                        .max()
                        .orElse(-1);
                announceLeader(leaderId);
            } else {
                // Add my ID and forward to the next peer
                String newMessage = "ELECTION:" + payload + "," + myInfo.getId();
                sendToNextAvailablePeer(newMessage);
            }
        } else if ("LEADER_ANNOUNCEMENT".equals(type)) {
            int leaderId = Integer.parseInt(payload);
            this.leader = findServerById(leaderId);
            isElectionInProgress = false;
            System.out.println("Server " + myInfo.getId() + " acknowledges new leader: Server " + leaderId);
        }else if ("REPLICATE".equals(type)) {
            // This is logic for a backup server
            System.out.println("Server " + myInfo.getId() + " received a replicated command.");
            try {
                Command command = (Command) SerializationUtils.deserialize(payload);
                // Apply the command to the backup's local state
                commandManager.executeCommand(command);
            } catch (Exception e) {
                System.err.println("Failed to deserialize or execute replicated command.");
                e.printStackTrace();
            }
        }
    }

    private void sendToNextAvailablePeer(String message) {
        int myIndex = allServers.indexOf(myInfo);
        int currentIndex = (myIndex + 1) % allServers.size();

        while (currentIndex != myIndex) {
            ServerInfo nextPeer = allServers.get(currentIndex);
            if (PeerCommunicator.sendMessage(nextPeer, message)) {
                // Message sent successfully
                return;
            }
            // If sending failed, try the next one in the ring
            currentIndex = (currentIndex + 1) % allServers.size();
        }
        System.err.println("Could not find any available peer to send message to. Election failed.");
    }
    private void announceLeader(int leaderId) {
        // Send a message around the ring to announce the winner
        System.out.println("Server " + myInfo.getId() + " is announcing leader: " + leaderId);
        for(ServerInfo server : allServers) {
            PeerCommunicator.sendMessage(server, "LEADER_ANNOUNCEMENT:" + leaderId);
        }
    }

    private ServerInfo findServerById(int id) {
        return allServers.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    private ServerInfo getNextPeer() {
        int myIndex = allServers.indexOf(myInfo);
        int nextIndex = (myIndex + 1) % allServers.size();
        return allServers.get(nextIndex);
    }
}