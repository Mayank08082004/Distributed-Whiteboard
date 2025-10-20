package com.example.whiteboard.server.cluster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClusterConfig {
    public static List<ServerInfo> readConfig(String filePath) throws IOException {
        List<ServerInfo> servers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",");
                servers.add(new ServerInfo(Integer.parseInt(parts[0]), parts[1], Integer.parseInt(parts[2])));
            }
        }
        return servers;
    }
}