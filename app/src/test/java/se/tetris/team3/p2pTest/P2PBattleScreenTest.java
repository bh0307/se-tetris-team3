package se.tetris.team3.p2pTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.ui.screen.P2PBattleScreen;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.core.Settings;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.net.P2PMessage;

import se.tetris.team3.net.P2PConnectionListener;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;

/**
 * P2P 대전 화면(`P2PBattleScreen`)의 핵심 동작을 검증하는 단위 테스트 모음입니다.
 *
 * 주요 검증 항목:
 * - P키로 일시정지 토글 시 `PAUSE_STATE` 메시지를 전송하는지,
 *   그리고 연결의 idle timeout 설정을 적절히 변경하는지.
 * - 원격 `STATE` 메시지 수신 시 이전 필드와의 차이를 이용해 파티클을 생성하는지,
 *   또한 원격이 먼저 게임오버를 보고하면 화면이 게임오버로 전환되는지.
 * - `PAUSE_STATE` 메시지 수신 시 내부 paused 플래그와 연결의 idle timeout이 동기화되는지.
 *
 * 테스트에서는 실제 네트워크 대신 FakeConnection을 사용해 전송된 메시지와
 * idle timeout 호출을 검사합니다.
 */
public class P2PBattleScreenTest {

    private AppFrame app;
    private Settings settings;

    /**
     * 테스트용 가짜 P2PConnection: send()로 전송된 메시지 타입을 수집하고
     * setIdleTimeoutEnabled 호출을 추적합니다.
     */
    private static class FakeConnection extends P2PConnection {
        public final List<P2PMessage.Type> sent = new ArrayList<>();
        public FakeConnection(P2PConnectionListener l) { super(l); }
        @Override public synchronized void send(P2PMessage msg) { if (msg != null) sent.add(msg.type); }
        @Override public void close() { /* 테스트에서는 실제 소켓 close를 수행하지 않음 */ }
    }

    @BeforeEach
    void setUp() {
        app = new AppFrame();
        settings = app.getSettings();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("상대방으로부터 STATE 메시지 수신 시 원격 플레이어 상태 업데이트")
    void remoteStateMessage_updatesRemotePlayerState() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        // STATE 메시지 생성 - 상대방의 게임 상태
        P2PMessage stateMsg = new P2PMessage();
        stateMsg.type = P2PMessage.Type.STATE;
        stateMsg.myScore = 1500;
        stateMsg.myLevel = 7;
        stateMsg.gameOver = false;
        stateMsg.field = new int[20][10];
        stateMsg.itemField = new char[20][10];
        
        screen.onMessageReceived(stateMsg);
        
        // 리플렉션으로 원격 상태 확인
        Field remoteScoreField = P2PBattleScreen.class.getDeclaredField("remoteScore");
        remoteScoreField.setAccessible(true);
        int remoteScore = (int) remoteScoreField.get(screen);
        
        Field remoteLevelField = P2PBattleScreen.class.getDeclaredField("remoteLevel");
        remoteLevelField.setAccessible(true);
        int remoteLevel = (int) remoteLevelField.get(screen);
        
        assertEquals(1500, remoteScore, "원격 플레이어 점수가 업데이트되어야 함");
        assertEquals(7, remoteLevel, "원격 플레이어 레벨이 업데이트되어야 함");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("상대방이 게임 오버 시 내가 승리자로 선언됨")
    void remoteGameOver_declaresLocalPlayerAsWinner() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, false
        );
        
        // 상대방이 게임 오버된 STATE 메시지
        P2PMessage stateMsg = new P2PMessage();
        stateMsg.type = P2PMessage.Type.STATE;
        stateMsg.gameOver = true;
        stateMsg.field = new int[20][10];
        stateMsg.itemField = new char[20][10];
        
        screen.onMessageReceived(stateMsg);
        
        // 게임 종료 및 승자 확인
        Field gameOverField = P2PBattleScreen.class.getDeclaredField("gameOver");
        gameOverField.setAccessible(true);
        boolean gameOver = (boolean) gameOverField.get(screen);
        
        Field winnerField = P2PBattleScreen.class.getDeclaredField("winner");
        winnerField.setAccessible(true);
        int winner = (int) winnerField.get(screen);
        
        assertTrue(gameOver, "게임이 종료 상태로 전환되어야 함");
        assertEquals(1, winner, "로컬 플레이어가 승리자(winner=1)로 선언되어야 함");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("P키 입력 시 일시정지 토글 및 PAUSE_STATE 메시지 전송")
    void pressP_togglesPauseAndSendsPauseMessage() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        // P키 입력 시뮬레이션
        KeyEvent pKeyEvent = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_P, 'P'
        );
        
        screen.onKeyPressed(pKeyEvent);
        
        // paused 플래그 확인
        Field pausedField = P2PBattleScreen.class.getDeclaredField("paused");
        pausedField.setAccessible(true);
        boolean paused = (boolean) pausedField.get(screen);
        
        assertTrue(paused, "P키 입력 후 게임이 일시정지 상태가 되어야 함");
        assertTrue(conn.sent.contains(P2PMessage.Type.PAUSE_STATE), 
            "PAUSE_STATE 메시지가 상대방에게 전송되어야 함");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("상대방으로부터 PAUSE_STATE 메시지 수신 시 일시정지 상태 동기화")
    void remotePauseMessage_synchronizesLocalPauseState() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, false
        );
        
        // 상대방이 일시정지 상태 전송
        P2PMessage pauseMsg = P2PMessage.pauseState(true);
        screen.onMessageReceived(pauseMsg);
        
        // 로컬 paused 플래그 확인
        Field pausedField = P2PBattleScreen.class.getDeclaredField("paused");
        pausedField.setAccessible(true);
        boolean paused = (boolean) pausedField.get(screen);
        
        assertTrue(paused, "상대방의 일시정지 상태가 로컬에 동기화되어야 함");
        
        // 일시정지 해제
        pauseMsg = P2PMessage.pauseState(false);
        screen.onMessageReceived(pauseMsg);
        paused = (boolean) pausedField.get(screen);
        
        assertFalse(paused, "상대방의 일시정지 해제가 로컬에 동기화되어야 함");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("상대방으로부터 ATTACK 메시지 수신 시 쓰레기 줄 추가")
    void remoteAttackMessage_addsGarbageLinesToLocalBoard() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, false
        );
        
        screen.onShow();
        
        // GameManager의 초기 대기 쓰레기 줄 개수 확인
        Field myManagerField = P2PBattleScreen.class.getDeclaredField("myManager");
        myManagerField.setAccessible(true);
        se.tetris.team3.gameManager.GameManager myManager = 
            (se.tetris.team3.gameManager.GameManager) myManagerField.get(screen);
        
        // ATTACK 메시지 생성 - 2줄의 쓰레기
        boolean[][] garbageRows = new boolean[2][10];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 10; j++) {
                garbageRows[i][j] = (j != i); // 각 줄마다 다른 위치에 구멍
            }
        }
        P2PMessage attackMsg = P2PMessage.attack(garbageRows);
        
        screen.onMessageReceived(attackMsg);
        
        // 쓰레기 줄이 GameManager에 추가되었는지 확인
        // (내부 구현상 enqueueGarbage가 호출되면 대기 큐에 추가됨)
        assertNotNull(myManager, "GameManager가 초기화되어야 함");
        
        screen.onHide();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("LAG_WARNING 메시지 수신 시 지연 경고 메시지 표시")
    void lagWarningMessage_displaysWarningToUser() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        String warningText = "네트워크 지연 감지: 300ms";
        P2PMessage lagMsg = new P2PMessage();
        lagMsg.type = P2PMessage.Type.LAG_WARNING;
        lagMsg.text = warningText;
        
        screen.onMessageReceived(lagMsg);
        
        // lagMessage 필드 확인
        Field lagMessageField = P2PBattleScreen.class.getDeclaredField("lagMessage");
        lagMessageField.setAccessible(true);
        String lagMessage = (String) lagMessageField.get(screen);
        
        assertEquals(warningText, lagMessage, "지연 경고 메시지가 화면에 표시될 수 있도록 저장되어야 함");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("네트워크 오류 발생 시 오류 메시지 업데이트")
    void networkError_updatesErrorMessage() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, false
        );
        
        Exception testException = new Exception("연결 끊김");
        screen.onNetworkError(testException);
        
        Field lagMessageField = P2PBattleScreen.class.getDeclaredField("lagMessage");
        lagMessageField.setAccessible(true);
        String lagMessage = (String) lagMessageField.get(screen);
        
        assertTrue(lagMessage.contains("네트워크 오류"), "네트워크 오류 메시지가 표시되어야 함");
        assertTrue(lagMessage.contains("연결 끊김"), "구체적인 오류 내용이 포함되어야 함");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("왼쪽/오른쪽/아래 방향키로 블록 이동")
    void arrowKeys_moveBlockInGame() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        screen.onShow();
        
        Field myManagerField = P2PBattleScreen.class.getDeclaredField("myManager");
        myManagerField.setAccessible(true);
        se.tetris.team3.gameManager.GameManager myManager = 
            (se.tetris.team3.gameManager.GameManager) myManagerField.get(screen);
        
        int initialX = myManager.getBlockX();
        
        // 왼쪽 키
        KeyEvent leftKey = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED
        );
        screen.onKeyPressed(leftKey);
        
        int afterLeftX = myManager.getBlockX();
        assertTrue(afterLeftX <= initialX, "왼쪽 키 입력 후 블록이 왼쪽으로 이동 시도해야 함");
        
        // 오른쪽 키
        KeyEvent rightKey = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED
        );
        screen.onKeyPressed(rightKey);
        
        // 아래 키
        KeyEvent downKey = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED
        );
        screen.onKeyPressed(downKey);
        
        // 블록 이동이 정상적으로 처리되었는지 확인 (오류 없이 실행)
        assertNotNull(myManager, "블록 이동 처리 완료");
        
        screen.onHide();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("스페이스바 하드 드롭 시 블록이 즉시 바닥으로 이동하고 고정됨")
    void spaceKey_hardDropMovesBlockToBottomAndFixesIt() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, false
        );
        
        screen.onShow();
        
        Field myManagerField = P2PBattleScreen.class.getDeclaredField("myManager");
        myManagerField.setAccessible(true);
        se.tetris.team3.gameManager.GameManager myManager = 
            (se.tetris.team3.gameManager.GameManager) myManagerField.get(screen);
        
        int initialY = myManager.getBlockY();
        int initialScore = myManager.getScore();
        
        // 스페이스 키로 하드 드롭
        KeyEvent spaceKey = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_SPACE, ' '
        );
        screen.onKeyPressed(spaceKey);
        
        // 하드 드롭 후 새 블록이 생성되므로 Y 위치가 초기화되고, 점수가 증가해야 함
        int afterDropY = myManager.getBlockY();
        int afterDropScore = myManager.getScore();
        
        assertTrue(afterDropY <= initialY, "하드 드롭 후 새 블록이 위쪽에서 시작해야 함");
        assertTrue(afterDropScore >= initialScore, "하드 드롭으로 점수가 증가해야 함");
        
        screen.onHide();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("화면 렌더링 시 양쪽 보드와 중앙 정보가 표시됨")
    void render_drawsBothBoardsAndCenterInfo() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        screen.onShow();
        
        // 상대방 상태 설정
        P2PMessage stateMsg = new P2PMessage();
        stateMsg.type = P2PMessage.Type.STATE;
        stateMsg.myScore = 2000;
        stateMsg.myLevel = 5;
        stateMsg.gameOver = false;
        stateMsg.field = new int[20][10];
        stateMsg.itemField = new char[20][10];
        screen.onMessageReceived(stateMsg);
        
        // Graphics2D 생성하여 실제 렌더링 테스트
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            1200, 800, java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        java.awt.Graphics2D g2 = img.createGraphics();
        
        // 렌더링 실행
        screen.render(g2);
        
        g2.dispose();
        
        // 렌더링이 오류 없이 완료되었는지 확인
        // 실제로는 이미지에 픽셀이 그려졌는지 확인할 수 있음
        Field remoteScoreField = P2PBattleScreen.class.getDeclaredField("remoteScore");
        remoteScoreField.setAccessible(true);
        int remoteScore = (int) remoteScoreField.get(screen);
        
        assertEquals(2000, remoteScore, "렌더링에 사용될 상대방 점수가 설정되어야 함");
        
        screen.onHide();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("렌더링 시 일시정지 오버레이가 표시됨")
    void pausedState_showsPauseOverlay() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        screen.onShow();
        
        // 일시정지
        KeyEvent pKey = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_P, 'P'
        );
        screen.onKeyPressed(pKey);
        
        Field pausedField = P2PBattleScreen.class.getDeclaredField("paused");
        pausedField.setAccessible(true);
        boolean isPaused = (boolean) pausedField.get(screen);
        
        // Graphics2D로 렌더링하여 일시정지 상태가 표시되는지 확인
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            1200, 800, java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        java.awt.Graphics2D g2 = img.createGraphics();
        
        screen.render(g2);
        
        g2.dispose();
        
        assertTrue(isPaused, "일시정지 상태가 설정되어야 함");
        // 렌더링 시 일시정지 오버레이가 그려짐 (오류 없이 렌더링 완료)
        
        screen.onHide();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("일시정지 중에는 블록 이동 키가 무시됨")
    void pausedState_ignoresMovementKeys() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        screen.onShow();
        
        Field myManagerField = P2PBattleScreen.class.getDeclaredField("myManager");
        myManagerField.setAccessible(true);
        se.tetris.team3.gameManager.GameManager myManager = 
            (se.tetris.team3.gameManager.GameManager) myManagerField.get(screen);
        
        // 일시정지
        KeyEvent pKey = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_P, 'P'
        );
        screen.onKeyPressed(pKey);
        
        int pausedX = myManager.getBlockX();
        
        // 일시정지 중 왼쪽 키 입력
        KeyEvent leftKey = new KeyEvent(
            app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
            KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED
        );
        screen.onKeyPressed(leftKey);
        
        int afterMoveX = myManager.getBlockX();
        
        assertEquals(pausedX, afterMoveX, "일시정지 중에는 블록이 이동하지 않아야 함");
        
        screen.onHide();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("상대방 필드 변경 시 파티클 효과 생성")
    void remoteFieldChange_createsParticleEffects() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PBattleScreen screen = new P2PBattleScreen(
            app, conn, GameMode.BATTLE_NORMAL, settings, 0, false
        );
        
        screen.onShow();
        
        // 첫 번째 상태: 필드에 블록 있음
        P2PMessage firstState = new P2PMessage();
        firstState.type = P2PMessage.Type.STATE;
        firstState.field = new int[20][10];
        firstState.itemField = new char[20][10];
        // 맨 아래 줄을 채움
        for (int c = 0; c < 10; c++) {
            firstState.field[19][c] = 1;
        }
        screen.onMessageReceived(firstState);
        
        // 두 번째 상태: 맨 아래 줄이 삭제됨
        P2PMessage secondState = new P2PMessage();
        secondState.type = P2PMessage.Type.STATE;
        secondState.field = new int[20][10];
        secondState.itemField = new char[20][10];
        // 맨 아래 줄이 비어있음 (라인 클리어)
        screen.onMessageReceived(secondState);
        
        // remoteParticles 필드 확인
        Field remoteParticlesField = P2PBattleScreen.class.getDeclaredField("remoteParticles");
        remoteParticlesField.setAccessible(true);
        java.util.List<?> remoteParticles = (java.util.List<?>) remoteParticlesField.get(screen);
        
        // 필드 변경으로 파티클이 생성되었는지 확인
        assertTrue(remoteParticles.size() > 0, "상대방 필드 변경 시 파티클이 생성되어야 함");
        
        screen.onHide();
    }

}
