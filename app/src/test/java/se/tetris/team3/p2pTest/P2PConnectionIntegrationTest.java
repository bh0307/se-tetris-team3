package se.tetris.team3.p2pTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.net.P2PConnectionListener;
import se.tetris.team3.net.P2PMessage;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 실제 소켓을 사용하여 P2PConnection의 서버-클라이언트 연결 플로우를 통합 테스트합니다.
 * 이 테스트는 로컬 환경에서 포트 바인딩/접속이 가능한지 확인하고,
 * P2PConnection이 등록된 리스너의 `onConnected` 콜백을 호출하는지 검증합니다.
 *
 * 주의: 환경에 따라 포트 충돌이나 방화벽에 의해 실패할 수 있습니다.
 */
public class P2PConnectionIntegrationTest {

    @Test
    @DisplayName("서버와 클라이언트 연결 시 onConnected 콜백 호출")
    void serverAndClient_connectAndInvokeOnConnected() throws Exception {
        // 서버/클라이언트 인스턴스 생성
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);

        CountDownLatch serverReadyLatch = new CountDownLatch(1);
        CountDownLatch serverConnectedLatch = new CountDownLatch(1);
        CountDownLatch clientConnectedLatch = new CountDownLatch(1);
        final int[] serverConnectCount = {0};

        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {
                serverConnectCount[0]++;
                if (serverConnectCount[0] == 1) {
                    serverReadyLatch.countDown();
                } else {
                    serverConnectedLatch.countDown();
                }
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(se.tetris.team3.net.P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) { fail("Server network error: " + e); }
        });

        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { clientConnectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(se.tetris.team3.net.P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) { fail("Client network error: " + e); }
        });

        // 서버 바인딩(백그라운드 스레드 내부에서 accept 대기)
        server.startServer();
        serverReadyLatch.await(2, TimeUnit.SECONDS);

        // 클라이언트에서 로컬호스트로 접속 시도
        client.connectTo("127.0.0.1");

        // 양측 연결 완료 대기
        boolean serverConnected = serverConnectedLatch.await(5, TimeUnit.SECONDS);
        boolean clientConnected = clientConnectedLatch.await(5, TimeUnit.SECONDS);

        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}

        assertTrue(serverConnected && clientConnected, "서버와 클라이언트 모두 onConnected 콜백을 호출해야 합니다.");

        // 정리
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("서버 연결 시 isServer 파라미터가 true")
    void serverConnection_hasCorrectServerFlag() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch serverConnectedLatch = new CountDownLatch(1);
        AtomicBoolean serverFlag = new AtomicBoolean(false);
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { 
                serverFlag.set(asServer);
                serverConnectedLatch.countDown(); 
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {}
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        server.startServer();
        client.connectTo("127.0.0.1");
        
        serverConnectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(serverFlag.get(), "서버 측 onConnected의 asServer 파라미터는 true여야 함");
        
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("클라이언트 연결 시 isServer 파라미터가 false")
    void clientConnection_hasCorrectServerFlag() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch serverReadyLatch = new CountDownLatch(1);
        CountDownLatch clientConnectedLatch = new CountDownLatch(1);
        AtomicBoolean clientServerFlag = new AtomicBoolean(true);
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {
                serverReadyLatch.countDown();
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {
                clientServerFlag.set(asServer);
                clientConnectedLatch.countDown();
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        server.startServer();
        // 서버가 대기 상태가 될 때까지 기다림
        serverReadyLatch.await(2, TimeUnit.SECONDS);
        
        client.connectTo("127.0.0.1");
        
        clientConnectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertFalse(clientServerFlag.get(), "클라이언트 측 onConnected의 asServer 파라미터는 false여야 함");
        
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("클라이언트 종료 시 서버에서 onDisconnected 호출")
    void clientDisconnects_serverReceivesDisconnectCallback() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch serverReadyLatch = new CountDownLatch(1);
        CountDownLatch serverConnectedLatch = new CountDownLatch(1);
        CountDownLatch clientConnectedLatch = new CountDownLatch(1);
        CountDownLatch disconnectedLatch = new CountDownLatch(1);
        AtomicReference<String> disconnectReason = new AtomicReference<>();
        final int[] serverConnectCount = {0};
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {
                serverConnectCount[0]++;
                if (serverConnectCount[0] == 1) {
                    serverReadyLatch.countDown();
                } else {
                    serverConnectedLatch.countDown();
                }
            }
            @Override public void onDisconnected(String reason) {
                disconnectReason.set(reason);
                disconnectedLatch.countDown();
            }
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { clientConnectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        server.startServer();
        serverReadyLatch.await(2, TimeUnit.SECONDS);
        
        client.connectTo("127.0.0.1");
        
        serverConnectedLatch.await(5, TimeUnit.SECONDS);
        clientConnectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 클라이언트 종료
        client.close();
        
        boolean disconnected = disconnectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(disconnected, "클라이언트 종료 시 서버에서 onDisconnected 콜백 호출");
        assertNotNull(disconnectReason.get(), "연결 종료 이유가 전달되어야 함");
        
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("메시지 전송 후 상대방이 수신")
    void messageSent_receivedByPeer() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch connectedLatch = new CountDownLatch(2);
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<P2PMessage.Type> receivedType = new AtomicReference<>();
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                receivedType.set(msg.type);
                messageLatch.countDown();
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        server.startServer();
        client.connectTo("127.0.0.1");
        
        connectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 클라이언트에서 서버로 메시지 전송
        client.send(P2PMessage.hello());
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(received, "메시지가 상대방에게 전달되어야 함");
        assertEquals(P2PMessage.Type.HELLO, receivedType.get(), "HELLO 메시지 타입이 올바르게 전달됨");
        
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("서버 IP 주소 캐싱 - 연결 종료 후에도 유지")
    void serverIPAddress_cachedAfterDisconnect() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch connectedLatch = new CountDownLatch(2);
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        server.startServer();
        client.connectTo("127.0.0.1");
        
        connectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 연결된 상태에서 IP 주소 확인
        String addressWhileConnected = server.getLocalAddress();
        assertNotNull(addressWhileConnected, "연결 중 IP 주소를 가져올 수 있어야 함");
        assertFalse(addressWhileConnected.contains("0.0.0.0"), "유효한 IP 주소여야 함");
        
        // 클라이언트 종료
        client.close();
        Thread.sleep(100); // 연결 종료 대기
        
        // 연결 종료 후에도 IP 주소가 유지되는지 확인
        String addressAfterDisconnect = server.getLocalAddress();
        assertEquals(addressWhileConnected, addressAfterDisconnect, 
            "연결 종료 후에도 캐싱된 IP 주소가 유지되어야 함");
        
        try { server.close(); } catch (Exception ignore) {}
    }
}

