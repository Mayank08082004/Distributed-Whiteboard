package com.example.whiteboard.model;

import java.awt.*;

public class Circle extends Shape {
    private final int radius;

    public Circle(int x, int y, int radius, Color color) {
        super(x, y, color);
        this.radius = radius;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        // Draw a filled circle centered at (x, y)
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }
    // In Circle.java
    @Override
    public boolean contains(Point p) {
        // Use the distance formula to see if the point is inside the circle
        return p.distance(x, y) <= radius;
    }

    @Override
    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle(x - radius, y - radius, radius * 2, radius * 2);
    }
}