package com.example.whiteboard.command;

import com.example.whiteboard.server.WhiteboardState; // Import server's state
import java.io.Serializable;

// Make Command Serializable so it can be sent over RMI
public interface Command extends Serializable {
    void execute(WhiteboardState state);
    void undo(WhiteboardState state);
}