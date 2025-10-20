package com.example.whiteboard.command;

import com.example.whiteboard.model.Shape;
import com.example.whiteboard.server.WhiteboardState;

import java.io.Serializable; // Import Serializable
import java.util.ArrayList;
import java.util.List;

// The class must be Serializable to be sent over the network
public class ClearCommand implements Command, Serializable {
    // This field must not be final, as it's set during execution
    private List<Shape> shapesBeforeClear;

    // The constructor is now empty, as the client creates this command
    // without knowing the server's state.
    public ClearCommand() {
    }

    @Override
    public void execute(WhiteboardState state) {
        // The state is now passed in by the server's CommandManager
        // Before clearing, save the current list of shapes
        this.shapesBeforeClear = new ArrayList<>(state.getShapes());
        state.clearShapes();
    }

    @Override
    public void undo(WhiteboardState state) {
        // The state is passed in again to perform the undo operation
        // To undo, add all the saved shapes back to the state
        state.addAllShapes(shapesBeforeClear);
    }
}