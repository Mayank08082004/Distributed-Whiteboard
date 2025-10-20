package com.example.whiteboard.command;

import com.example.whiteboard.model.Shape;
import com.example.whiteboard.server.WhiteboardState;

public class DeleteShapeCommand implements Command {
    // We store the actual shape object to allow for a perfect undo.
    private final Shape shapeToDelete;

    public DeleteShapeCommand(Shape shapeToDelete) {
        this.shapeToDelete = shapeToDelete;
    }

    @Override
    public void execute(WhiteboardState state) {
        state.removeShape(shapeToDelete);
    }

    @Override
    public void undo(WhiteboardState state) {
        state.addShape(shapeToDelete);
    }
}