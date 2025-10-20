package com.example.whiteboard.server.cluster;

public class ServerInfo {
    private final int id;
    private final String host;
    private final int port;

    public ServerInfo(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public int getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
}