package se.tetris.team3.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.swing.SwingUtilities;

public class P2PConnection implements Closeable {

    public static final int DEFAULT_PORT = 34567;
    public static final int SOCKET_TIMEOUT = 3000;
    public static final int DISCONNECT_TIMEOUT = 8000;

    private volatile P2PConnectionListener listener;
    private volatile boolean running;
    private boolean isServer;

    private ServerSocket serverSocket;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private long lastReceiveTime;

    private volatile boolean idleTimeoutEnabled = true; //기본: 켜짐

    public P2PConnection() {
    }

    public P2PConnection(P2PConnectionListener listener) {
        this.listener = listener;
    }

    public void setListener(P2PConnectionListener listener) {
        this.listener = listener;
    }

    // 로비에서는 false, 게임 중에는 true 로 사용하는 플래그
    public void setIdleTimeoutEnabled(boolean enabled) {
        this.idleTimeoutEnabled = enabled;
    }

    public boolean isServer() {
        return isServer;
    }

    public String getLocalAddress() {
        if (socket != null && socket.isConnected()) {
            return socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();
        }
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + DEFAULT_PORT;
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ────────── 서버 모드 ──────────
    public void startServer() {
        isServer = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(DEFAULT_PORT);
                deliverConnected(true);        // UI 에서 "서버 대기 시작" 등 표시할 수도 있음
                socket = serverSocket.accept();
                initStreams();
                deliverConnected(true);        // 실제 연결 완료
                startReadLoop();
            } catch (IOException e) {
                deliverNetworkError(e);
                closeSilently();
            }
        }, "P2P-Server-Accept").start();
    }

    // ────────── 클라이언트 모드 ──────────
    public void connectTo(String addr) {
        isServer = false;
        new Thread(() -> {
            try {
                // "ip" or "ip:port" 지원
                String host = addr.trim();
                int port = DEFAULT_PORT;
                int idx = host.indexOf(':');
                if (idx >= 0) {
                    String portStr = host.substring(idx + 1).trim();
                    host = host.substring(0, idx).trim();
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (NumberFormatException ignore) {
                        // 잘못된 포트면 기본 포트 사용
                    }
                }

                socket = new Socket(host, port);
                initStreams();
                deliverConnected(false);
                startReadLoop();
            } catch (IOException e) {
                deliverNetworkError(e);
                closeSilently();
            }
        }, "P2P-Client-Connect").start();
    }

    private void initStreams() throws IOException {
        socket.setSoTimeout(SOCKET_TIMEOUT);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        running = true;
        lastReceiveTime = System.currentTimeMillis();
    }

    public synchronized void send(P2PMessage msg) {
        if (!running || out == null) return;
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            deliverNetworkError(e);
            closeSilently();
        }
    }

    private void startReadLoop() {
        new Thread(() -> {
            try {
                while (running) {
                    try {
                        Object obj = in.readObject();
                        if (!(obj instanceof P2PMessage)) continue;
                        P2PMessage msg = (P2PMessage) obj;
                        lastReceiveTime = System.currentTimeMillis();
                        deliverMessage(msg);
                    } catch (SocketTimeoutException toe) {
                        if (!idleTimeoutEnabled) { // 로비는 타임아웃 건너뛰기
                            continue;
                        }

                        long now = System.currentTimeMillis();
                        if (now - lastReceiveTime > DISCONNECT_TIMEOUT) {
                            deliverDisconnected("상대방 응답 없음");
                            break;
                        } else if (now - lastReceiveTime > SOCKET_TIMEOUT * 2) {
                            deliverMessage(P2PMessage.lagWarning(
                                    "네트워크 지연: " + (now - lastReceiveTime) + "ms"));
                        }
                    }
                }
            } catch (Exception e) {
                deliverNetworkError(e);
            } finally {
                closeSilently();
            }
        }, "P2P-ReadLoop").start();
    }

    private void deliverConnected(boolean asServer) {
        P2PConnectionListener l = listener;
        if (l == null) return;
        SwingUtilities.invokeLater(() -> l.onConnected(asServer));
    }

    private void deliverDisconnected(String reason) {
        P2PConnectionListener l = listener;
        if (l == null) return;
        SwingUtilities.invokeLater(() -> l.onDisconnected(reason));
    }

    private void deliverNetworkError(Exception e) {
        P2PConnectionListener l = listener;
        if (l == null) return;
        SwingUtilities.invokeLater(() -> l.onNetworkError(e));
    }

    private void deliverMessage(P2PMessage msg) {
        P2PConnectionListener l = listener;
        if (l == null) return;
        SwingUtilities.invokeLater(() -> l.onMessageReceived(msg));
    }

    private void closeSilently() {
        try { close(); } catch (IOException ignore) {}
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (in != null) try { in.close(); } catch (IOException ignore) {}
        if (out != null) try { out.close(); } catch (IOException ignore) {}
        if (socket != null) try { socket.close(); } catch (IOException ignore) {}
        if (serverSocket != null) try { serverSocket.close(); } catch (IOException ignore) {}
    }
}
