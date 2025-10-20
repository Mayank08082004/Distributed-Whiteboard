package com.example.whiteboard.client;

import com.example.whiteboard.model.Shape;
import com.example.whiteboard.server.WhiteboardState;

import javax.swing.JPanel;
import java.awt.*;
import java.util.List;
import java.awt.BasicStroke; // Import
import java.awt.Graphics2D;  // Import
import java.awt.Stroke;      // Import

public class CanvasPanel extends JPanel {
    private final WhiteboardState whiteboardState;
    private Shape previewShape = null; // Add this field
    private Shape selectedShape = null;

    public CanvasPanel(WhiteboardState state) {
        this.whiteboardState = state;
        setBackground(Color.WHITE);
    }

    public WhiteboardState getWhiteboardState() {
        return this.whiteboardState;
    }

    public void setSelectedShape(Shape shape) {
        this.selectedShape = shape;
    }
    public void setPreviewShape(Shape shape) {
        this.previewShape = shape;
    }

    // This method is the key to drawing in Swing.
    // It's called automatically whenever the panel needs to be redrawn.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        List<Shape> shapes = whiteboardState.getShapes();
        // Loop through all shapes in the state and tell each one to draw itself
        synchronized (shapes) { // Synchronize to prevent errors in later phases
            for (Shape shape : shapes) {
                shape.draw(g);
            }
        }

        if (previewShape != null) {
            // Make the preview look different (e.g., semi-transparent)
            Graphics2D g2d = (Graphics2D) g;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            previewShape.draw(g);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        if (selectedShape != null) {
            Graphics2D g2d = (Graphics2D) g;
            Stroke oldStroke = g2d.getStroke();
            // Create a dashed line stroke
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g2d.setColor(Color.CYAN);
            Rectangle bounds = selectedShape.getBounds();
            g2d.drawRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4);
            g2d.setStroke(oldStroke); // Reset the stroke
        }
    }
}