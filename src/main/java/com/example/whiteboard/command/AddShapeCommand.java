package com.example.whiteboard.command;

import com.example.whiteboard.model.Shape;
import com.example.whiteboard.server.WhiteboardState;

public class AddShapeCommand implements Command {
    private final Shape shape;
    // The command no longer holds a reference to the state

    public AddShapeCommand(Shape shape) {
        this.shape = shape;
    }

    @Override
    public void execute(WhiteboardState state) {
        state.addShape(shape);
    }

    @Override
    public void undo(WhiteboardState state) {
        state.removeShape(shape);
    }
}