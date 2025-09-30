package se.tetris.team3;

import javax.swing.SwingUtilities;
import se.tetris.team3.ui.AppFrame;

public class Tetris {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }
}
