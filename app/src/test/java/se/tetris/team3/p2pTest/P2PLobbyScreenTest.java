package se.tetris.team3.p2pTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.ui.screen.P2PLobbyScreen;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.core.Settings;
import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.net.P2PMessage;
import se.tetris.team3.net.P2PConnectionListener;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * P2P 로비 화면(`P2PLobbyScreen`)의 주요 동작을 검증하는 단위 테스트.
 * - `reopenAfterGame`로 복귀했을 때 내부 필드와 리스너 설정을 올바르게 수행하는지.
 * - 서버/클라이언트로 연결되었을 때 어떤 메시지를 전송하는지(간단한 핸드셰이크).
 * - 클라이언트 IP 입력 흐름에서 `connectTo`를 호출하는지.
 *
 * 실제 네트워크 연결을 사용하지 않기 위해 `FakeConnection`을 만들어 전송된 메시지와
 * 호출된 메서드를 검사합니다.
 */
public class P2PLobbyScreenTest {

    private AppFrame app;
    private Settings settings;

    /**
     * 테스트용 가짜 연결 객체.
     * - setListener / setIdleTimeoutEnabled 호출을 추적
     * - send()로 보낸 메시지 타입을 수집
     */
    private static class FakeConnection extends P2PConnection {
        public boolean startServerCalled = false;
        public String connectArg = null;
        public final List<P2PMessage.Type> sent = new ArrayList<>();
        private boolean isServerMode = false;
        
        public FakeConnection(P2PConnectionListener l) { super(l); }
        
        @Override public void startServer() { 
            startServerCalled = true;
            isServerMode = true;
        }
        
        @Override public void connectTo(String addr) { 
            connectArg = addr;
            isServerMode = false;
        }
        
        @Override public synchronized void send(P2PMessage msg) { 
            if (msg != null) sent.add(msg.type); 
        }
        
        @Override public String getLocalAddress() { 
            return "127.0.0.1:34567"; 
        }
        
        @Override public boolean isServer() {
            return isServerMode;
        }
        
        public void setServerMode(boolean serverMode) {
            this.isServerMode = serverMode;
        }
    }

    @BeforeEach
    void setUp() {
        app = new AppFrame();
        settings = app.getSettings();
    }

    @Test
    @DisplayName("생성자: connection 없이 생성")
    void testConstructorWithNoConnection() {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        assertNotNull(screen);
    }

    @Test
    @DisplayName("생성자: 기존 connection으로 생성")
    void testConstructorWithExistingConnection() {
        FakeConnection conn = new FakeConnection(null);
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings, conn);
        assertNotNull(screen);
    }

    @Test
    @DisplayName("reopenAfterGame: 게임 종료 후 로비 재진입")
    void testReopenAfterGame() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PLobbyScreen screen = P2PLobbyScreen.reopenAfterGame(app, settings, conn, true);
        
        // phase가 LOBBY로 설정되어야 함
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        Object phase = phaseField.get(screen);
        assertEquals("LOBBY", phase.toString());
        
        // connected가 true여야 함
        Field connectedField = P2PLobbyScreen.class.getDeclaredField("connected");
        connectedField.setAccessible(true);
        assertTrue(connectedField.getBoolean(screen));
    }

    @Test
    @DisplayName("onShow: connection 생성 확인")
    void testOnShowCreatesConnection() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        screen.onShow();
        
        // connection이 생성되어야 함
        Field connField = P2PLobbyScreen.class.getDeclaredField("connection");
        connField.setAccessible(true);
        assertNotNull(connField.get(screen));
    }

    @Test
    @DisplayName("onHide: connection 종료 확인")
    void testOnHideClosesConnection() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        screen.onShow();
        
        Field goingField = P2PLobbyScreen.class.getDeclaredField("goingIntoBattle");
        goingField.setAccessible(true);
        goingField.set(screen, false); // 게임으로 안 가는 경우
        
        assertDoesNotThrow(() -> screen.onHide());
    }

    @Test
    @DisplayName("onConnected: 서버로 연결 시 HELLO와 MODE_INFO 전송")
    void testOnConnectedAsServer() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings, conn);
        Field connField = P2PLobbyScreen.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(screen, conn);
        
        screen.onConnected(true);
        
        // 서버로 연결되면 HELLO와 MODE_INFO 메시지를 보내야 함
        assertTrue(conn.sent.contains(P2PMessage.Type.HELLO));
        assertTrue(conn.sent.contains(P2PMessage.Type.MODE_INFO));
    }

    @Test
    @DisplayName("onConnected: 클라이언트로 연결 시 HELLO 전송")
    void testOnConnectedAsClient() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings, conn);
        Field connField = P2PLobbyScreen.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(screen, conn);
        
        screen.onConnected(false);
        
        // 클라이언트로 연결되면 HELLO 메시지를 보내야 함
        assertTrue(conn.sent.contains(P2PMessage.Type.HELLO));
    }

    @Test
    @DisplayName("onDisconnected: 연결 끊김 시 connected 상태 false")
    void testOnDisconnected() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        screen.onDisconnected("Test disconnect");
        
        Field connectedField = P2PLobbyScreen.class.getDeclaredField("connected");
        connectedField.setAccessible(true);
        assertFalse(connectedField.getBoolean(screen));
    }

    @Test
    @DisplayName("onMessageReceived: HELLO 수신 시 서버는 HELLO_OK 응답")
    void testOnMessageReceivedHello() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        conn.setServerMode(true); // 서버 모드로 설정
        
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings, conn);
        Field connField = P2PLobbyScreen.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(screen, conn);
        
        screen.onConnected(true); // 서버로 연결
        conn.sent.clear();
        
        screen.onMessageReceived(P2PMessage.hello());
        
        // 서버는 HELLO를 받으면 HELLO_OK를 보내야 함
        assertTrue(conn.sent.contains(P2PMessage.Type.HELLO_OK));
    }

    @Test
    @DisplayName("onMessageReceived: MODE_INFO 수신 시 로비 모드 설정")
    void testOnMessageReceivedModeInfo() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        P2PMessage msg = P2PMessage.modeInfo(se.tetris.team3.core.GameMode.BATTLE_ITEM, 180);
        
        screen.onMessageReceived(msg);
        
        Field modeField = P2PLobbyScreen.class.getDeclaredField("lobbyMode");
        modeField.setAccessible(true);
        assertEquals(se.tetris.team3.core.GameMode.BATTLE_ITEM, modeField.get(screen));
    }

    @Test
    @DisplayName("onMessageReceived: READY_STATE 수신 시 상대 준비 상태 업데이트")
    void testOnMessageReceivedReadyState() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        P2PMessage msg = P2PMessage.ready(true);
        
        screen.onMessageReceived(msg);
        
        Field otherReadyField = P2PLobbyScreen.class.getDeclaredField("otherReady");
        otherReadyField.setAccessible(true);
        assertTrue(otherReadyField.getBoolean(screen));
    }

    @Test
    @DisplayName("onNetworkError: 네트워크 오류 처리")
    void testOnNetworkError() {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        assertDoesNotThrow(() -> screen.onNetworkError(new Exception("Test error")));
    }

    @Test
    @DisplayName("키 입력: S키로 서버 모드 선택")
    void testRoleSelectServerKey() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        
        KeyEvent keyS = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_S, 'S');
        screen.onKeyPressed(keyS);
        
        assertEquals("SERVER_WAIT", phaseField.get(screen).toString());
    }

    @Test
    @DisplayName("키 입력: C키로 클라이언트 모드 선택")
    void testRoleSelectClientKey() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        
        KeyEvent keyC = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_C, 'C');
        screen.onKeyPressed(keyC);
        
        assertEquals("CLIENT_INPUT_IP", phaseField.get(screen).toString());
    }

    @Test
    @DisplayName("IP 입력: Backspace로 문자 삭제")
    void testClientIpInputBackspace() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "CLIENT_INPUT_IP"));
        
        Field ipField = P2PLobbyScreen.class.getDeclaredField("inputIP");
        ipField.setAccessible(true);
        ipField.set(screen, "192.168");
        
        KeyEvent backspace = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_BACK_SPACE, '\b');
        screen.onKeyPressed(backspace);
        
        assertEquals("192.16", ipField.get(screen));
    }

    @Test
    @DisplayName("IP 입력: 숫자 입력 처리")
    void testClientIpInputDigits() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "CLIENT_INPUT_IP"));
        
        Field ipField = P2PLobbyScreen.class.getDeclaredField("inputIP");
        ipField.setAccessible(true);
        ipField.set(screen, "");
        
        KeyEvent key1 = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_1, '1');
        screen.onKeyPressed(key1);
        
        assertEquals("1", ipField.get(screen));
    }

    @Test
    @DisplayName("로비: Enter키로 준비 상태 토글")
    void testLobbyInputReadyToggle() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings, conn);
        
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "LOBBY"));
        
        Field connField = P2PLobbyScreen.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(screen, conn);
        
        Field connectedField = P2PLobbyScreen.class.getDeclaredField("connected");
        connectedField.setAccessible(true);
        connectedField.set(screen, true);
        
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        
        Field myReadyField = P2PLobbyScreen.class.getDeclaredField("myReady");
        myReadyField.setAccessible(true);
        assertTrue(myReadyField.getBoolean(screen));
    }

    @Test
    @DisplayName("서버: UP키로 모드 선택 변경")
    void testServerModeSelectionUp() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "LOBBY"));
        
        Field asServerField = P2PLobbyScreen.class.getDeclaredField("asServer");
        asServerField.setAccessible(true);
        asServerField.set(screen, true);
        
        Field connectedField = P2PLobbyScreen.class.getDeclaredField("connected");
        connectedField.setAccessible(true);
        connectedField.set(screen, true);
        
        Field selectedField = P2PLobbyScreen.class.getDeclaredField("selectedModeIndex");
        selectedField.setAccessible(true);
        selectedField.set(screen, 1);
        
        KeyEvent up = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_UP, ' ');
        screen.onKeyPressed(up);
        
        // 1에서 UP하면 (1+2)%3 = 0
        assertEquals(0, selectedField.get(screen));
    }

    @Test
    @DisplayName("서버: DOWN키로 모드 선택 변경")
    void testServerModeSelectionDown() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "LOBBY"));
        
        Field asServerField = P2PLobbyScreen.class.getDeclaredField("asServer");
        asServerField.setAccessible(true);
        asServerField.set(screen, true);
        
        Field connectedField = P2PLobbyScreen.class.getDeclaredField("connected");
        connectedField.setAccessible(true);
        connectedField.set(screen, true);
        
        Field selectedField = P2PLobbyScreen.class.getDeclaredField("selectedModeIndex");
        selectedField.setAccessible(true);
        selectedField.set(screen, 0);
        
        KeyEvent down = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_DOWN, ' ');
        screen.onKeyPressed(down);
        
        assertEquals(1, selectedField.get(screen));
    }

    @Test
    @DisplayName("서버: LEFT키로 시간 제한 감소")
    void testServerTimeLimitDecrease() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "LOBBY"));
        
        Field asServerField = P2PLobbyScreen.class.getDeclaredField("asServer");
        asServerField.setAccessible(true);
        asServerField.set(screen, true);
        
        Field connectedField = P2PLobbyScreen.class.getDeclaredField("connected");
        connectedField.setAccessible(true);
        connectedField.set(screen, true);
        
        Field selectedField = P2PLobbyScreen.class.getDeclaredField("selectedModeIndex");
        selectedField.setAccessible(true);
        selectedField.set(screen, 2); // Time Attack
        
        Field timeField = P2PLobbyScreen.class.getDeclaredField("timeLimitMin");
        timeField.setAccessible(true);
        timeField.set(screen, 5);
        
        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_LEFT, ' ');
        screen.onKeyPressed(left);
        
        assertEquals(4, timeField.get(screen));
    }

    @Test
    @DisplayName("서버: RIGHT키로 시간 제한 증가")
    void testServerTimeLimitIncrease() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "LOBBY"));
        
        Field asServerField = P2PLobbyScreen.class.getDeclaredField("asServer");
        asServerField.setAccessible(true);
        asServerField.set(screen, true);
        
        Field connectedField = P2PLobbyScreen.class.getDeclaredField("connected");
        connectedField.setAccessible(true);
        connectedField.set(screen, true);
        
        Field selectedField = P2PLobbyScreen.class.getDeclaredField("selectedModeIndex");
        selectedField.setAccessible(true);
        selectedField.set(screen, 2);
        
        Field timeField = P2PLobbyScreen.class.getDeclaredField("timeLimitMin");
        timeField.setAccessible(true);
        timeField.set(screen, 3);
        
        KeyEvent right = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_RIGHT, ' ');
        screen.onKeyPressed(right);
        
        assertEquals(4, timeField.get(screen));
    }

    @Test
    @DisplayName("렌더링: 예외 없이 렌더링")
    void testRenderDoesNotThrow() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(800, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("렌더링: ROLE_SELECT 화면")
    void testRenderRoleSelect() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "ROLE_SELECT"));
        
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(800, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("렌더링: LOBBY 화면")
    void testRenderLobbyPhase() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "LOBBY"));
        
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(800, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("렌더링: CLIENT_INPUT_IP 화면")
    void testRenderClientInputIp() throws Exception {
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings);
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "CLIENT_INPUT_IP"));
        
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(800, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("렌더링: SERVER_WAIT 화면 (connection 포함)")
    void testRenderServerWaitWithConnection() throws Exception {
        FakeConnection conn = new FakeConnection(null);
        P2PLobbyScreen screen = new P2PLobbyScreen(app, settings, conn);
        
        Field phaseField = P2PLobbyScreen.class.getDeclaredField("phase");
        phaseField.setAccessible(true);
        phaseField.set(screen, getPhaseEnum(screen, "SERVER_WAIT"));
        
        Field asServerField = P2PLobbyScreen.class.getDeclaredField("asServer");
        asServerField.setAccessible(true);
        asServerField.set(screen, true);
        
        Field connField = P2PLobbyScreen.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(screen, conn);
        
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(800, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    // Helper method to get Phase enum value
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object getPhaseEnum(P2PLobbyScreen screen, String phaseName) throws Exception {
        Class<?>[] declaredClasses = P2PLobbyScreen.class.getDeclaredClasses();
        for (Class<?> c : declaredClasses) {
            if (c.getSimpleName().equals("Phase")) {
                return Enum.valueOf((Class<Enum>) c, phaseName);
            }
        }
        throw new IllegalArgumentException("Phase enum not found");
    }
}
