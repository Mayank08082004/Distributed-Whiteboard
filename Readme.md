Distributed Collaborative Whiteboard

A real-time, fault-tolerant collaborative whiteboard application built from the ground up to demonstrate core distributed systems concepts. This system allows multiple users to draw, chat, and collaborate on a shared canvas, and is resilient to server failures.

Features

Real-Time Collaboration: Multiple users can draw on the same canvas and see each other's changes instantly.

Multi-User Chat: An integrated, multi-threaded chat room for live communication.

Variety of Drawing Tools: Includes tools for drawing circles, rectangles, free-form pencil lines, and adding text.

Shape Manipulation: A selection tool allows users to move and delete existing shapes on the canvas.

Undo/Redo: A robust, command-based undo/redo system for all drawing actions.

Save as Image: Client-side functionality to export the current state of the whiteboard as a PNG image.

Fault Tolerance: The server cluster can survive a leader crash, automatically elect a new leader, and continue operating without data loss.

Client-Side Recovery: The client application can detect a leader failure and automatically reconnect to the newly elected leader.

Distributed Systems Concepts Implemented

This project was built to demonstrate a wide range of fundamental distributed systems concepts:

1.  Java RMI (Remote Method Invocation)

Implementation: RMI forms the core communication backbone for the whiteboard. The client invokes remote methods on the leader server (e.g., executeCommand, getShapes) via a WhiteboardService interface.

2.  Multi-threading

Implementation: The integrated chat server is fully multi-threaded. It spawns a new thread for each connected client, allowing it to handle multiple simultaneous conversations without blocking.

3.  Leader Election (Ring Algorithm)

Implementation: The server nodes form a logical ring. When a failure is detected, an election is initiated where an ELECTION message is passed around the ring. The node with the highest ID is elected as the new leader and announces its victory.

4.  Failure Detection (Heartbeat Mechanism)

Implementation: The leader periodically sends "heartbeat" messages to all backup servers. If a backup does not receive a heartbeat within a set timeout, it presumes the leader has crashed and initiates a new election.

5.  Data Replication & Consistency (Primary-Backup Model)

Implementation: The elected leader acts as the Primary server. When it receives a drawing command, it first applies it to its local state, then replicates the command to all Backup servers. This ensures the whiteboard state is consistent across the cluster.

6.  Clock Synchronization (Cristian's Algorithm)

Implementation: Backup servers periodically ask the leader for its current time. By measuring the round-trip time, they calculate the clock offset and can adjust their local time, demonstrating a working synchronization algorithm.

7.  Load Balancing (Service Discovery)

Implementation: A LoadBalancer component acts as a single, stable entry point for all clients. Clients connect to the Load Balancer, which is responsible for knowing who the current leader is and providing the client with the correct RMI stub to connect to.

System Architecture

The application is composed of four main components that run as separate processes:

Server Cluster (3+ nodes): The core of the system. These nodes use a peer-to-peer network to communicate for leader election, replication, and heartbeats. Only the elected leader binds the RMI service.

Chat Server (1 instance): A standalone, multi-threaded server that manages all chat-related communication using standard sockets.

Load Balancer (1 instance): A lightweight service that runs its own RMI registry. It polls the server cluster's registry to find the leader and acts as a directory service for incoming clients.

Client (Multiple instances): The user-facing Java Swing application that users run on their machines. It connects to the Load Balancer to find the leader and then establishes a direct RMI connection.

How to Run the Application

Prerequisites

Java 24 (or compatible)

Apache Maven

1. Build the Project

First, compile the code and create the necessary JAR files using Maven. From the project's root directory, run:

mvn clean package


This will generate executable JARs in the target/ directory.

2. Running Order

The components must be started in the following order:

A. Start the Chat Server

java -jar target/ChatServer.jar


B. Start the Load Balancer

java -jar target/LoadBalancer.jar


C. Start the Server Cluster
Open three separate terminal windows. In each one, run the WhiteboardServer.jar with a different server ID (0, 1, and 2).

Terminal 1: java -jar target/WhiteboardServer.jar 0

Terminal 2: java -jar target/WhiteboardServer.jar 1

Terminal 3: java -jar target/WhiteboardServer.jar 2

Wait for the servers to elect a leader. You will see I am the leader. RMI service is bound. in one of the terminals.

D. Start the Client
Once the leader is ready, you can launch the client application.

java -jar target/Client.jar


You can run this command multiple times to open multiple client windows.

Project Structure

src/main/java/com/example/whiteboard/

client/: Contains all client-side UI (Swing) and controller logic.

server/: Contains the core server logic, including the RMI service implementation and the ServerMain launcher.

cluster/: Holds all the logic for the server cluster, including configuration, peer communication, leader election, and failure detection.

loadbalancer/: The standalone Load Balancer application.

chat/: The standalone multi-threaded Chat Server.

remote/: Shared RMI interfaces used by both client and server.

model/: Data model classes for shapes (Circle, Rectangle, etc.).

command/: The implementation of the Command design pattern (AddShapeCommand, etc.).

servers.config: Configuration file listing the IDs and ports for each server in the cluster.

pom.xml: The Maven project configuration file.
