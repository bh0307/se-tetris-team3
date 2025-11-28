package se.tetris.team3.gameManager;

public class MenuItem {
    private String label; private Runnable action;

    public MenuItem(String l, Runnable a) {label = l; action = a; }

    public String getLabel() {
        return label;
    }
    public Runnable getAction() {
        return action;
    }
    
    }