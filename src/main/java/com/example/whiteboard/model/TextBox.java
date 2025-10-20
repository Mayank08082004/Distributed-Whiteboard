package com.example.whiteboard.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

public class TextBox extends Shape {
    private final String text;

    public TextBox(int x, int y, String text, Color color) {
        super(x, y, color);
        this.text = text;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString(text, x, y);
    }

    @Override
    public Rectangle getBounds() {
        // This is an estimation. For perfect bounds, we'd need a Graphics context
        // to measure the font. For this project, this is a good approximation.
        // The y-coordinate is adjusted up because drawString draws from the baseline.
        int width = text.length() * 8; // Approx. 8 pixels per character
        int height = 16; // Approx. 16 pixels font height
        return new Rectangle(x, y - height, width, height);
    }

    @Override
    public boolean contains(Point p) {
        return getBounds().contains(p);
    }

    @Override
    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }
}