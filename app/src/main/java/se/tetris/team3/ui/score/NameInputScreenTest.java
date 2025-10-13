package se.tetris.team3.ui.score;

import se.tetris.team3.ui.AppFrame;

public class NameInputScreenTest {
    public static void main(String[] args) {
        AppFrame app = new AppFrame();
        
        // 이름 입력 화면을 바로 표시
        app.showScreen(new NameInputScreen(app, 15000)); // 테스트 점수 15000
        
        app.setVisible(true);
    }
}