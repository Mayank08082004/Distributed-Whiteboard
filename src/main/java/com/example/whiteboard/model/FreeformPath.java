package com.example.whiteboard.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D; // Import Line2D for distance calculation
import java.util.ArrayList;
import java.util.List;

public class FreeformPath extends Shape {
    private final List<Point> points = new ArrayList<>();

    public FreeformPath(Point startPoint, Color color) {
        super(startPoint.x, startPoint.y, color);
        points.add(startPoint);
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (points.isEmpty()) {
            return new Rectangle(x, y, 0, 0);
        }
        // Find the min and max x/y coordinates to define the bounding box
        int minX = points.get(0).x;
        int minY = points.get(0).y;
        int maxX = points.get(0).x;
        int maxY = points.get(0).y;

        for (Point p : points) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public boolean contains(Point p) {
        // Check if the click is within a small threshold (e.g., 5 pixels)
        // of any line segment in the path.
        final double threshold = 5.0;
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            if (Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, p.x, p.y) < threshold) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void move(int dx, int dy) {
        // Move the base point
        this.x += dx;
        this.y += dy;
        // And translate every point in the path
        for (Point p : points) {
            p.translate(dx, dy);
        }
    }
}