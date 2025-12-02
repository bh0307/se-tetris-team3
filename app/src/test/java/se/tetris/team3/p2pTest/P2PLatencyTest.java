package se.tetris.team3.p2pTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.net.P2PConnectionListener;
import se.tetris.team3.net.P2PMessage;
import se.tetris.team3.gameManager.BattleGameManager;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.core.Settings;
import se.tetris.team3.core.GameMode;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P2P 네트워크 지연시간 성능 테스트.
 * 
 * 요구사항: 동일 로컬 네트워크 상에서 연결되었을 때 
 * 키 입력과 화면 표시 지연 200ms 이하
 * 
 * 이 테스트는 로컬호스트 환경에서 메시지 왕복 시간(RTT)을 측정하여
 * 200ms 이하 성능을 보장하는지 검증합니다.
 */
public class P2PLatencyTest {

    @Test
    @DisplayName("로컬 네트워크에서 메시지 왕복 시간(RTT) 200ms 이하")
    void localNetwork_messageRoundTripTime_under200ms() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch serverReadyLatch = new CountDownLatch(1);
        CountDownLatch serverConnectedLatch = new CountDownLatch(1);
        CountDownLatch clientConnectedLatch = new CountDownLatch(1);
        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicLong sendTime = new AtomicLong(0);
        AtomicLong receiveTime = new AtomicLong(0);
        
        // 서버: HELLO 메시지 받으면 즉시 HELLO_OK 응답
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {
                if (asServer) {
                    if (serverReadyLatch.getCount() > 0) {
                        serverReadyLatch.countDown();
                    } else {
                        serverConnectedLatch.countDown();
                    }
                }
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.HELLO) {
                    server.send(P2PMessage.helloOk());
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 클라이언트: HELLO_OK 받으면 시간 기록
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { clientConnectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.HELLO_OK) {
                    receiveTime.set(System.nanoTime());
                    responseLatch.countDown();
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 연결
        server.startServer();
        serverReadyLatch.await(5, TimeUnit.SECONDS);
        
        client.connectTo("127.0.0.1");
        
        serverConnectedLatch.await(5, TimeUnit.SECONDS);
        clientConnectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 메시지 전송 시작
        sendTime.set(System.nanoTime());
        client.send(P2PMessage.hello());
        
        // 응답 대기
        boolean received = responseLatch.await(1, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(received, "응답 메시지를 수신해야 함");
        
        // RTT 계산 (나노초 → 밀리초)
        long rtt = (receiveTime.get() - sendTime.get()) / 1_000_000;
        
        System.out.println("Message RTT: " + rtt + "ms");
        assertTrue(rtt < 200, "로컬 네트워크 메시지 왕복 시간이 200ms 미만이어야 함 (실제: " + rtt + "ms)");
        
        // 정리
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("공격 메시지 전송 지연시간 200ms 이하")
    void attackMessage_latency_under200ms() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch serverReadyLatch = new CountDownLatch(1);
        CountDownLatch serverConnectedLatch = new CountDownLatch(1);
        CountDownLatch clientConnectedLatch = new CountDownLatch(1);
        CountDownLatch attackLatch = new CountDownLatch(1);
        AtomicLong sendTime = new AtomicLong(0);
        AtomicLong receiveTime = new AtomicLong(0);
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {
                if (asServer) {
                    if (serverReadyLatch.getCount() > 0) {
                        serverReadyLatch.countDown();
                    } else {
                        serverConnectedLatch.countDown();
                    }
                }
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.ATTACK) {
                    receiveTime.set(System.nanoTime());
                    attackLatch.countDown();
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { clientConnectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 연결
        server.startServer();
        serverReadyLatch.await(5, TimeUnit.SECONDS);
        
        client.connectTo("127.0.0.1");
        
        serverConnectedLatch.await(5, TimeUnit.SECONDS);
        clientConnectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 공격 메시지 전송 (키 입력 시뮬레이션)
        sendTime.set(System.nanoTime());
        boolean[][] garbageRows = new boolean[2][10]; // 2줄 공격
        client.send(P2PMessage.attack(garbageRows));
        
        // 수신 대기
        boolean received = attackLatch.await(1, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(received, "공격 메시지를 수신해야 함");
        
        // 지연시간 계산
        long latency = (receiveTime.get() - sendTime.get()) / 1_000_000;
        
        System.out.println("Attack message latency: " + latency + "ms");
        assertTrue(latency < 200, "공격 메시지 전송 지연이 200ms 미만이어야 함 (실제: " + latency + "ms)");
        
        // 정리
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("연속 메시지 전송 시 모든 메시지 지연시간 200ms 이하")
    void continuousMessages_averageLatency_under200ms() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch connectedLatch = new CountDownLatch(2);
        final int MESSAGE_COUNT = 10;
        CountDownLatch messagesLatch = new CountDownLatch(MESSAGE_COUNT);
        long[] latencies = new long[MESSAGE_COUNT];
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.READY_STATE) {
                    // 즉시 응답
                    server.send(P2PMessage.ready(true));
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        final int[] messageIndex = {0};
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.READY_STATE && messageIndex[0] < MESSAGE_COUNT) {
                    long receiveTime = System.nanoTime();
                    latencies[messageIndex[0]] = receiveTime;
                    messageIndex[0]++;
                    messagesLatch.countDown();
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 연결
        server.startServer();
        client.connectTo("127.0.0.1");
        
        connectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 연속 메시지 전송
        long[] sendTimes = new long[MESSAGE_COUNT];
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            sendTimes[i] = System.nanoTime();
            client.send(P2PMessage.ready(false));
            Thread.sleep(10); // 메시지 간 작은 간격
        }
        
        // 모든 응답 대기
        boolean allReceived = messagesLatch.await(2, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(allReceived, "모든 응답 메시지를 수신해야 함");
        
        // 각 메시지 지연시간 검증
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            long latency = (latencies[i] - sendTimes[i]) / 1_000_000;
            System.out.println("Message " + (i + 1) + " latency: " + latency + "ms");
            assertTrue(latency < 200, 
                "메시지 " + (i + 1) + " 지연이 200ms 미만이어야 함 (실제: " + latency + "ms)");
        }
        
        // 정리
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("게임 시작 메시지 전송 지연시간 200ms 이하")
    void gameStartMessage_latency_under200ms() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch connectedLatch = new CountDownLatch(2);
        CountDownLatch gameStartLatch = new CountDownLatch(1);
        AtomicLong sendTime = new AtomicLong(0);
        AtomicLong receiveTime = new AtomicLong(0);
        
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.GAME_START) {
                    receiveTime.set(System.nanoTime());
                    gameStartLatch.countDown();
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 연결
        server.startServer();
        client.connectTo("127.0.0.1");
        
        connectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 게임 시작 메시지 전송
        sendTime.set(System.nanoTime());
        server.send(P2PMessage.gameStart());
        
        // 수신 대기
        boolean received = gameStartLatch.await(1, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(received, "게임 시작 메시지를 수신해야 함");
        
        // 지연시간 계산
        long latency = (receiveTime.get() - sendTime.get()) / 1_000_000;
        
        System.out.println("Game start message latency: " + latency + "ms");
        assertTrue(latency < 200, "게임 시작 메시지 전송 지연이 200ms 미만이어야 함 (실제: " + latency + "ms)");
        
        // 정리
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("키 입력부터 상대 화면 반영까지 지연시간 200ms 이하")
    void keyInputToScreenUpdate_latency_under200ms() throws Exception {
        P2PConnection server = new P2PConnection(null);
        P2PConnection client = new P2PConnection(null);
        
        CountDownLatch connectedLatch = new CountDownLatch(2);
        CountDownLatch screenUpdateLatch = new CountDownLatch(1);
        AtomicLong keyPressTime = new AtomicLong(0);
        AtomicLong screenUpdateTime = new AtomicLong(0);
        
        // 서버: 공격 메시지 받으면 화면 업데이트 시뮬레이션
        server.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.ATTACK) {
                    // 화면 업데이트 시뮬레이션 (공격 줄 추가 처리)
                    screenUpdateTime.set(System.nanoTime());
                    screenUpdateLatch.countDown();
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        client.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { connectedLatch.countDown(); }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 연결
        server.startServer();
        client.connectTo("127.0.0.1");
        
        connectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 키 입력 시뮬레이션: 라인 클리어로 공격 메시지 전송
        keyPressTime.set(System.nanoTime());
        boolean[][] garbageRows = new boolean[4][10]; // 4줄 공격
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                garbageRows[i][j] = (j != i); // 구멍 하나씩
            }
        }
        client.send(P2PMessage.attack(garbageRows));
        
        // 상대 화면 업데이트 대기
        boolean updated = screenUpdateLatch.await(1, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        assertTrue(updated, "상대 화면이 업데이트되어야 함");
        
        // 키 입력부터 화면 업데이트까지 총 지연시간 계산
        long totalLatency = (screenUpdateTime.get() - keyPressTime.get()) / 1_000_000;
        
        System.out.println("Key input to screen update latency: " + totalLatency + "ms");
        assertTrue(totalLatency < 200, 
            "키 입력부터 상대 화면 반영까지 지연이 200ms 미만이어야 함 (실제: " + totalLatency + "ms)");
        
        // 정리
        try { client.close(); } catch (Exception ignore) {}
        try { server.close(); } catch (Exception ignore) {}
    }
    
    @Test
    @DisplayName("키 입력부터 상대방 화면 렌더링까지 전체 지연시간 200ms 이하")
    void keyInput_toRemoteScreenRender_under200ms() throws Exception {
        P2PConnection player1Conn = new P2PConnection(null);
        P2PConnection player2Conn = new P2PConnection(null);
        
        CountDownLatch serverReadyLatch = new CountDownLatch(1);
        CountDownLatch serverConnectedLatch = new CountDownLatch(1);
        CountDownLatch clientConnectedLatch = new CountDownLatch(1);
        CountDownLatch renderCompleteLatch = new CountDownLatch(1);
        AtomicLong keyPressTime = new AtomicLong(0);
        AtomicLong renderCompleteTime = new AtomicLong(0);
        
        // 플레이어1 (클라이언트): 키 입력 후 필드 상태 전송
        BattleGameManager battleManager = new BattleGameManager(
            GameMode.BATTLE_NORMAL, 
            new Settings(), 
            0
        );
        battleManager.start();
        GameManager player1Manager = battleManager.getPlayer1Manager();
        
        player1Conn.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) { 
                clientConnectedLatch.countDown();
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {}
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 플레이어2 (서버): 필드 상태 수신 후 실제 렌더링 수행
        player2Conn.setListener(new P2PConnectionListener() {
            @Override public void onConnected(boolean asServer) {
                if (asServer) {
                    if (serverReadyLatch.getCount() > 0) {
                        serverReadyLatch.countDown();
                    } else {
                        serverConnectedLatch.countDown();
                    }
                }
            }
            @Override public void onDisconnected(String reason) {}
            @Override public void onMessageReceived(P2PMessage msg) {
                if (msg.type == P2PMessage.Type.STATE && msg.field != null) {
                    // 실제 렌더링 작업 시뮬레이션 (EDT가 아닌 곳에서 실행 중)
                    // BufferedImage로 오프스크린 렌더링
                    java.awt.image.BufferedImage img = 
                        new java.awt.image.BufferedImage(400, 800, 
                            java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g2 = img.createGraphics();
                    
                    // 실제 게임 보드 렌더링 (필드 그리기)
                    int blockSize = 30;
                    for (int r = 0; r < 20; r++) {
                        for (int c = 0; c < 10; c++) {
                            if (msg.field[r][c] != 0) {
                                g2.setColor(java.awt.Color.WHITE);
                                g2.fillRect(c * blockSize, r * blockSize, 
                                           blockSize, blockSize);
                                g2.setColor(java.awt.Color.BLACK);
                                g2.drawRect(c * blockSize, r * blockSize, 
                                           blockSize, blockSize);
                            }
                        }
                    }
                    
                    g2.dispose();
                    
                    // 렌더링 완료 시간 기록
                    renderCompleteTime.set(System.nanoTime());
                    renderCompleteLatch.countDown();
                }
            }
            @Override public void onNetworkError(Exception e) {}
        });
        
        // 연결
        player2Conn.startServer();
        serverReadyLatch.await(5, TimeUnit.SECONDS);
        
        player1Conn.connectTo("127.0.0.1");
        
        serverConnectedLatch.await(5, TimeUnit.SECONDS);
        clientConnectedLatch.await(5, TimeUnit.SECONDS);
        try { SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignore) {}
        
        // 블록이 생성될 때까지 대기
        Thread.sleep(100);
        
        // 키 입력 시뮬레이션: 왼쪽 이동
        keyPressTime.set(System.nanoTime());
        int currentX = player1Manager.getBlockX();
        int currentY = player1Manager.getBlockY();
        player1Manager.tryMove(currentX - 1, currentY);
        
        // 필드 상태를 배열로 복사하여 전송
        int[][] field = new int[20][10];
        for (int r = 0; r < 20; r++) {
            for (int c = 0; c < 10; c++) {
                field[r][c] = player1Manager.getFieldValue(r, c);
            }
        }
        P2PMessage stateMsg = new P2PMessage();
        stateMsg.type = P2PMessage.Type.STATE;
        stateMsg.field = field;
        player1Conn.send(stateMsg);
        
        // 상대방이 렌더링 완료할 때까지 대기
        boolean rendered = renderCompleteLatch.await(2, TimeUnit.SECONDS);
        
        assertTrue(rendered, "렌더링이 완료되어야 함");
        
        // 키 입력부터 상대방 화면 렌더링 완료까지 전체 지연시간 계산
        long totalLatency = (renderCompleteTime.get() - keyPressTime.get()) / 1_000_000;
        
        System.out.println("Key input to remote screen render (including rendering): " + totalLatency + "ms");
        assertTrue(totalLatency < 200, 
            "키 입력부터 상대방 화면 렌더링 완료까지 지연이 200ms 미만이어야 함 (실제: " + totalLatency + "ms)");
        
        // 정리
        try { player1Conn.close(); } catch (Exception ignore) {}
        try { player2Conn.close(); } catch (Exception ignore) {}
    }
}
