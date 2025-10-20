package com.example.whiteboard.server;
import java.io.*; // Add these imports
import com.example.whiteboard.command.Command;
import com.example.whiteboard.model.Shape; // Import Shape
import java.util.List; // Import List
import java.util.Stack;

public class CommandManager {
    private final WhiteboardState whiteboardState; // Add reference to the state
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    // Constructor now requires the state it will manage
    public CommandManager(WhiteboardState whiteboardState) {
        this.whiteboardState = whiteboardState;
    }

    public List<Shape> getShapes() {
        return whiteboardState.getShapes();
    }

    /**
     * Saves the current list of shapes to a file.
     * @param filename The path of the file to save to.
     */
    public synchronized void saveStateToFile(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            // We serialize the entire list of shapes.
            oos.writeObject(whiteboardState.getShapes());
            System.out.println("Whiteboard state saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving state to file: " + e.getMessage());
        }
    }

    /**
     * Loads the list of shapes from a file.
     * @param filename The path of the file to load from.
     */
    @SuppressWarnings("unchecked")
    public synchronized void loadStateFromFile(String filename) {
        File saveFile = new File(filename);
        if (!saveFile.exists()) {
            System.out.println("No save file found. Starting with a blank canvas.");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            List<Shape> loadedShapes = (List<Shape>) ois.readObject();
            whiteboardState.clearShapes();
            whiteboardState.addAllShapes(loadedShapes);
            System.out.println("Whiteboard state loaded from " + filename);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading state from file: " + e.getMessage());
        }
    }

    public void executeCommand(Command command) {
        // Pass the state to the command upon execution
        command.execute(whiteboardState);
        undoStack.push(command);
        redoStack.clear();
    }

    // In CommandManager.java on the server

    public void undo() {
        System.out.println("Attempting to undo. Undo stack size: " + undoStack.size()); // DEBUG
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo(whiteboardState);
            redoStack.push(command);
            System.out.println("Undo successful."); // DEBUG
        } else {
            System.out.println("Undo stack is empty. Nothing to undo."); // DEBUG
        }
    }

    public void redo() {
        System.out.println("Attempting to redo. Redo stack size: " + redoStack.size()); // DEBUG
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute(whiteboardState);
            undoStack.push(command);
            System.out.println("Redo successful."); // DEBUG
        } else {
            System.out.println("Redo stack is empty. Nothing to redo."); // DEBUG
        }
    }
}