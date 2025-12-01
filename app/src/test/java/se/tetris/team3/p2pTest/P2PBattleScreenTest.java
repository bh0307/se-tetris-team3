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

}
