package com.example.whiteboard.command;

import com.example.whiteboard.model.Shape;
import com.example.whiteboard.server.WhiteboardState;

public class MoveShapeCommand implements Command {
    private final String shapeId;
    private final int dx; // Delta X
    private final int dy; // Delta Y

    public MoveShapeCommand(String shapeId, int dx, int dy) {
        this.shapeId = shapeId;
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void execute(WhiteboardState state) {
        Shape shape = state.getShapeById(shapeId);
        if (shape != null) {
            shape.move(dx, dy);
        }
    }

    @Override
    public void undo(WhiteboardState state) {
        Shape shape = state.getShapeById(shapeId);
        if (shape != null) {
            // Move it back by the opposite amount
            shape.move(-dx, -dy);
        }
    }
}