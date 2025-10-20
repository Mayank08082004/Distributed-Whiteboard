package com.example.whiteboard.server;

import com.example.whiteboard.model.Shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhiteboardState {
    // Use a thread-safe list for future phases
    private final List<Shape> shapes = Collections.synchronizedList(new ArrayList<>());

    public void addShape(Shape shape) {
        shapes.add(shape);
    }

    public List<Shape> getShapes() {
        return shapes;
    }
    // Inside your WhiteboardState.java class
    public void clearShapes() {
        shapes.clear();
    }
    public void removeShape(Shape shape) {
        shapes.remove(shape);
    }

    // Also, to support the new ClearCommand, add this method:
    public void addAllShapes(List<Shape> shapesToAdd) {
        shapes.addAll(shapesToAdd);
    }
    // In WhiteboardState.java
    public Shape getShapeById(String id) {
        // This is not efficient for very large lists, but fine for this project.
        return shapes.stream()
                .filter(shape -> shape.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}