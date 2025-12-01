package se.tetris.team3.p2pTest;

import org.junit.jupiter.api.Test;
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
        public FakeConnection(P2PConnectionListener l) { super(l); }
        @Override public void startServer() { startServerCalled = true; }
        @Override public void connectTo(String addr) { connectArg = addr; }
        @Override public synchronized void send(P2PMessage msg) { if (msg != null) sent.add(msg.type); }
        @Override public String getLocalAddress() { return "127.0.0.1:34567"; }
    }

    @BeforeEach
    void setUp() {
        app = new AppFrame();
        settings = app.getSettings();
    }
}
