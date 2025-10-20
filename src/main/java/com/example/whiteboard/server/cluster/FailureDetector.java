package com.example.whiteboard.server.cluster;

public class FailureDetector {
    private final ElectionManager electionManager;
    private volatile long lastHeartbeatTime;
    private static final long TIMEOUT_MS = 6000; // 6 seconds

    public FailureDetector(ElectionManager electionManager) {
        this.electionManager = electionManager;
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public void startMonitoring() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(TIMEOUT_MS);
                    if (electionManager.getLeader() != null && !electionManager.amITheLeader()) {
                        if ((System.currentTimeMillis() - lastHeartbeatTime) > TIMEOUT_MS) {
                            System.err.println("Leader failure detected! Starting a new election.");
                            electionManager.startElection();
                            // Reset timer to avoid starting multiple elections
                            heartbeatReceived();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void heartbeatReceived() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }
}