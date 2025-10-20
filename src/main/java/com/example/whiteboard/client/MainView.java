package com.example.whiteboard.client;

import com.example.whiteboard.server.WhiteboardState;
import javax.swing.*;
import java.awt.*;

public class MainView extends JFrame {
    private final CanvasPanel canvasPanel;
    // Expose the buttons so the controller can access them
    public final JButton circleButton;
    public final JButton rectButton;
    public final JButton textButton;
    public final JButton colorButton;
    public final JButton clearButton;
    public final JButton pencilButton;
    public final JButton undoButton;
    public final JButton redoButton;
    public final JTextArea chatArea;
    public final JTextField chatInput;
    public final JList<String> userList;
    public final DefaultListModel<String> userListModel;
    public final JLabel typingStatusLabel;
    public  final JButton selectButton;
    public final JButton deleteButton;
    // In MainView.java, with your other button fields
    public final JButton saveButton;

    public MainView(WhiteboardState state) {
        setTitle("Collaborative Whiteboard - Client");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Create Toolbar ---
        JToolBar toolBar = new JToolBar();
        circleButton = new JButton("Circle");
        rectButton = new JButton("Rectangle");
        textButton = new JButton("Text");
        colorButton = new JButton("Color");
        clearButton = new JButton("Clear");
        pencilButton = new JButton("Pencil");
        undoButton = new JButton("Undo");
        redoButton = new JButton("Redo");
        selectButton = new JButton("Select");
        deleteButton = new JButton("Delete");
        saveButton = new JButton("Save as PNG");
        // --- Toolbar (North) ---
        toolBar.add(circleButton);
        toolBar.add(rectButton);
        toolBar.add(textButton);
        toolBar.add(pencilButton);
        toolBar.add(colorButton);
        toolBar.addSeparator();
        toolBar.add(undoButton);
        toolBar.add(redoButton);
        toolBar.add(selectButton);
        toolBar.add(deleteButton);
        toolBar.add(clearButton);
        toolBar.addSeparator(); // A nice visual separator
        toolBar.add(saveButton);
        add(toolBar, BorderLayout.NORTH);

        // --- Create Canvas ---
        canvasPanel = new CanvasPanel(state);
        add(canvasPanel, BorderLayout.CENTER);

        // --- User List (West) ---
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Online Users"));
        userListScrollPane.setPreferredSize(new Dimension(150, 0));
        add(userListScrollPane, BorderLayout.WEST);

        // --- Chat Area (East) ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Chat Room"));
        chatScrollPane.setPreferredSize(new Dimension(300, 0));
        add(chatScrollPane, BorderLayout.EAST);

        // --- Chat Input (South) ---
        chatInput = new JTextField();
        typingStatusLabel = new JLabel(" ");
        typingStatusLabel.setPreferredSize(new Dimension(200, 20)); // Give it space

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(new JLabel(" Message: "), BorderLayout.WEST);
        southPanel.add(chatInput, BorderLayout.CENTER);
        southPanel.add(typingStatusLabel, BorderLayout.EAST);
        add(southPanel, BorderLayout.SOUTH);
    }

    public CanvasPanel getCanvasPanel() {
        return canvasPanel;
    }
    public JTextArea getChatArea() {
        return chatArea;
    }

    public JTextField getChatInput() {
        return chatInput;
    }
}