package se.tetris.team3.p2pTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.net.P2PConnectionListener;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 실제 소켓을 사용하여 P2PConnection의 서버-클라이언트 연결 플로우를 통합 테스트합니다.
 * 이 테스트는 로컬 환경에서 포트 바인딩/접속이 가능한지 확인하고,
 * P2PConnection이 등록된 리스너의 `onConnected` 콜백을 호출하는지 검증합니다.
 *
 * 주의: 환경에 따라 포트 충돌이나 방화벽에 의해 실패할 수 있습니다.
 */
public class P2PConnectionIntegrationTest {

    @Test
    void serverAndClient_connectAndInvokeOnConnected() throws Exception {
        // 서버/클라이언트 인스턴스 생성
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);

        // 양측의 onConnected 호출을 기다리기 위한 래치
        CountDownLatch connectedLatch = new CountDownLatch(2);

        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(se.tetris.team3.net.P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) { fail("Server network error: " + e); }
        });

        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(se.tetris.team3.net.P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) { fail("Client network error: " + e); }
        });

        // 서버 바인딩(백그라운드 스레드 내부에서 accept 대기)
        server.startServer();

        // 클라이언트에서 로컬호스트로 접속 시도
        client.connectTo("127.0.0.1");

        // P2PConnection은 콜백을 EDT에 전달하므로, 최대 5초 기다리고 EDT를 플러시해서 invokeLater를 실행시킵니다.
        boolean arrived = connectedLatch.await(5, TimeUnit.SECONDS);

        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}

        assertTrue(arrived, "서버와 클라이언트 모두 onConnected 콜백을 호출해야 합니다.");

        // 정리
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
}
