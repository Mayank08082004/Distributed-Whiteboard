package com.example.whiteboard.client;

import com.example.whiteboard.command.*;
import com.example.whiteboard.model.*;
import com.example.whiteboard.model.Rectangle;
import com.example.whiteboard.model.Shape;
import com.example.whiteboard.remote.ClientCallback;
import com.example.whiteboard.remote.WhiteboardService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CanvasController {
    private final MainView mainView;
    private WhiteboardService whiteboardService;
    private ClientCallback clientCallbackStub;

    private Tool currentTool = Tool.CIRCLE;
    private Color currentColor = Color.BLACK;
    private Point startPoint;
    private FreeformPath currentPath;
    private Shape selectedShape = null;
    private Point dragStartPoint = null;

    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    public CanvasController(WhiteboardService service, MainView view, ClientCallback callbackStub) {
        this.whiteboardService = service;
        this.mainView = view;
        this.clientCallbackStub = callbackStub;
        addListeners();
    }

    public void setClientCallbackStub(ClientCallback stub) {
        this.clientCallbackStub = stub;
    }

    public WhiteboardService getWhiteboardService() {
        return this.whiteboardService;
    }

    private void addListeners() {
        CanvasPanel canvas = mainView.getCanvasPanel();

        // --- MOUSE EVENT LISTENERS (RESTRUCTURED AND CORRECTED) ---

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();

                switch (currentTool) {
                    case SELECT:
                        // Find which shape was clicked on (looping backwards to get the top-most one)
                        List<Shape> shapes = mainView.getCanvasPanel().getWhiteboardState().getShapes();
                        selectedShape = null; // Deselect first
                        for (int i = shapes.size() - 1; i >= 0; i--) {
                            Shape shape = shapes.get(i);
                            if (shape.contains(e.getPoint())) {
                                selectedShape = shape;
                                dragStartPoint = e.getPoint(); // For calculating total move distance
                                break;
                            }
                        }
                        mainView.getCanvasPanel().setSelectedShape(selectedShape);
                        mainView.deleteButton.setEnabled(selectedShape != null);
                        canvas.repaint();
                        break;

                    case PENCIL:
                        currentPath = new FreeformPath(startPoint, currentColor);
                        break;

                    case TEXT:
                        String text = JOptionPane.showInputDialog(mainView, "Enter text:", "Text Tool", JOptionPane.PLAIN_MESSAGE);
                        if (text != null && !text.isEmpty()) {
                            sendCommand(new AddShapeCommand(new TextBox(e.getX(), e.getY(), text, currentColor)));
                        }
                        break;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (startPoint == null) return;

                Command command = null;
                switch (currentTool) {
                    case SELECT:
                        if (selectedShape != null && dragStartPoint != null) {
                            int totalDx = e.getX() - dragStartPoint.x;
                            int totalDy = e.getY() - dragStartPoint.y;
                            if (totalDx != 0 || totalDy != 0) {
                                command = new MoveShapeCommand(selectedShape.getId(), totalDx, totalDy);
                            }
                        }
                        break;

                    case PENCIL:
                        command = new AddShapeCommand(currentPath);
                        currentPath = null;
                        break;

                    case CIRCLE:
                    case RECTANGLE:
                        Shape newShape = createPreviewShape(e.getPoint());
                        if (newShape != null) {
                            command = new AddShapeCommand(newShape);
                        }
                        break;
                }

                if (command != null) {
                    sendCommand(command);
                }

                canvas.setPreviewShape(null);
                dragStartPoint = null;
                startPoint = null;
                // We wait for the server's callback to do the final repaint.
            }
        });

        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPoint == null) return;

                switch (currentTool) {
                    case SELECT:
                        if (selectedShape != null) {
                            int dx = e.getX() - startPoint.x;
                            int dy = e.getY() - startPoint.y;
                            selectedShape.move(dx, dy);
                            startPoint = e.getPoint(); // Update start point for the next micro-drag
                        }
                        break;

                    case PENCIL:
                        if (currentPath != null) {
                            currentPath.addPoint(e.getPoint());
                            canvas.setPreviewShape(currentPath);
                        }
                        break;

                    case CIRCLE:
                    case RECTANGLE:
                        canvas.setPreviewShape(createPreviewShape(e.getPoint()));
                        break;
                }
                canvas.repaint();
            }
        });

        // --- TOOLBAR BUTTON LISTENERS ---
        mainView.selectButton.addActionListener(e -> {
            currentTool = Tool.SELECT;
            selectedShape = null;
            mainView.getCanvasPanel().setSelectedShape(null);
            mainView.deleteButton.setEnabled(false);
            mainView.getCanvasPanel().repaint();
        });

        mainView.circleButton.addActionListener(e -> currentTool = Tool.CIRCLE);
        mainView.rectButton.addActionListener(e -> currentTool = Tool.RECTANGLE);
        mainView.textButton.addActionListener(e -> currentTool = Tool.TEXT);
        mainView.pencilButton.addActionListener(e -> currentTool = Tool.PENCIL);

        mainView.deleteButton.addActionListener(e -> {
            if (selectedShape != null) {
                sendCommand(new DeleteShapeCommand(selectedShape));
                selectedShape = null;
                mainView.getCanvasPanel().setSelectedShape(null);
                mainView.deleteButton.setEnabled(false);
            }
        });

        mainView.undoButton.addActionListener(e -> {
            try {
                whiteboardService.undo();
            } catch (RemoteException ex) {
                handleAndRecover(ex);
            }
        });

        mainView.redoButton.addActionListener(e -> {
            try {
                whiteboardService.redo();
            } catch (RemoteException ex) {
                handleAndRecover(ex);
            }
        });

        mainView.colorButton.addActionListener(e -> {
            Color chosenColor = JColorChooser.showDialog(mainView, "Choose a color", currentColor);
            if (chosenColor != null) {
                currentColor = chosenColor;
            }
        });

        mainView.clearButton.addActionListener(e -> sendCommand(new ClearCommand()));

        mainView.saveButton.addActionListener(e -> saveCanvasAsPNG());
    }

    private void sendCommand(Command command) {
        try {
            whiteboardService.executeCommand(command);
        } catch (RemoteException ex) {
            handleAndRecover(ex);
        }
    }

    private void saveCanvasAsPNG() {
        CanvasPanel canvas = mainView.getCanvasPanel();
        // Create a blank image with the same dimensions as the canvas
        BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Get the graphics context of the image
        Graphics2D g2d = image.createGraphics();

        // Ask the canvas to paint itself onto our image's graphics context
        canvas.paint(g2d);

        // Dispose of the graphics context to free up resources
        g2d.dispose();

        // --- PROMPT USER FOR FILE LOCATION ---
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Whiteboard As...");
        fileChooser.setSelectedFile(new File("whiteboard.png")); // Suggest a filename

        int userSelection = fileChooser.showSaveDialog(mainView);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Ensure the file has a .png extension
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".png");
            }

            try {
                // Use ImageIO to write the buffered image to the chosen file
                ImageIO.write(image, "png", fileToSave);
                JOptionPane.showMessageDialog(mainView, "Whiteboard saved successfully!", "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainView, "Error saving whiteboard: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public void handleAndRecover(Exception e) {
        if (isReconnecting.compareAndSet(false, true)) {
            JOptionPane.showMessageDialog(mainView, "Connection lost. Finding new leader...", "Reconnecting", JOptionPane.INFORMATION_MESSAGE);
            new Thread(() -> {
                try {
                    System.out.println("Looking for new leader in the RMI Registry...");
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    this.whiteboardService = (WhiteboardService) registry.lookup("WhiteboardService");
                    this.whiteboardService.registerClient(this.clientCallbackStub);
                    System.out.println("Reconnected and re-registered with the new leader successfully!");
                    this.clientCallbackStub.updateView();
                } catch (Exception ex) {
                    System.err.println("Failed to find a new leader: " + ex.getMessage());
                } finally {
                    isReconnecting.set(false);
                }
            }).start();
        }
    }

    private Shape createPreviewShape(Point endPoint) {
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(startPoint.x - endPoint.x);
        int height = Math.abs(startPoint.y - endPoint.y);

        switch (currentTool) {
            case CIRCLE:
                int diameter = Math.max(width, height);
                return new Circle(x + diameter / 2, y + diameter / 2, diameter / 2, currentColor);
            case RECTANGLE:
                return new Rectangle(x, y, width, height, currentColor);
            default:
                return null;
        }
    }
}