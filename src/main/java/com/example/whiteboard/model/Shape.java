package com.example.whiteboard.model;

import java.awt.Color;
import java.awt.Graphics;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import java.awt.Point; // Import Point
import java.awt.Rectangle; // Import Rectangle

// abstract class for all shapes
public abstract class Shape implements Serializable { // Serializable is for later phases
    protected final String id;
    protected int x, y;
    protected Color color;

    public Shape(int x, int y, Color color) {
        this.id = UUID.randomUUID().toString(); // Give each shape a unique ID
        this.x = x;
        this.y = y;
        this.color = color;
    }

    // Add a public getter for the ID
    public String getId() {
        return this.id;
    }

    // New abstract methods that all shapes must implement
    public abstract boolean contains(Point p);
    public abstract void move(int dx, int dy);
    public abstract java.awt.Rectangle getBounds();
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }

    // Each shape must know how to draw itself
    public abstract void draw(Graphics g);
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shape shape = (Shape) o;
        // Two shapes are considered equal if they have the same unique ID.
        return id.equals(shape.id);
    }

    @Override
    public int hashCode() {
        // When overriding equals, you must also override hashCode.
        return Objects.hash(id);
    }
}