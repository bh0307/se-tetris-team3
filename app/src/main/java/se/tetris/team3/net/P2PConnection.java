package se.tetris.team3.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * 서버/클라이언트 공통으로 사용하는 P2P 연결 래퍼.
 * - 서버 모드: startServer()
 * - 클라이언트 모드: connectTo()
 * 내부에서 수신 스레드를 돌리며, P2PConnectionListener 로 이벤트 전달.
 */
public class P2PConnection implements Closeable {

    public static final int DEFAULT_PORT = 34567;
    public static final int SOCKET_TIMEOUT = 3000;      // read timeout (ms)
    public static final int DISCONNECT_TIMEOUT = 8000;  // 이 시간 이상 응답 없으면 끊김 처리

    private P2PConnectionListener listener;
    private volatile boolean running;
    private boolean isServer;

    private ServerSocket serverSocket;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private long lastReceiveTime;

    public P2PConnection(P2PConnectionListener listener) {
        this.listener = listener;
    }

    public void setListener(P2PConnectionListener listener) {
        this.listener = listener;
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

    // 서버로 동작
    public void startServer() {
        isServer = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(DEFAULT_PORT);
                socket = serverSocket.accept();
                initStreams();
                listener.onConnected(true);
                startReadLoop();
            } catch (IOException e) {
                listener.onNetworkError(e);
                closeSilently();
            }
        }, "P2P-Server-Accept").start();
    }

    // 클라이언트로 동작
    public void connectTo(String host) {
        isServer = false;
        new Thread(() -> {
            try {
                socket = new Socket(host, DEFAULT_PORT);
                initStreams();
                listener.onConnected(false);
                startReadLoop();
            } catch (IOException e) {
                listener.onNetworkError(e);
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
            listener.onNetworkError(e);
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
                        listener.onMessageReceived(msg);
                    } catch (SocketTimeoutException toe) {
                        long now = System.currentTimeMillis();
                        if (now - lastReceiveTime > DISCONNECT_TIMEOUT) {
                            listener.onDisconnected("상대방 응답 없음");
                            break;
                        } else if (now - lastReceiveTime > SOCKET_TIMEOUT * 2) {
                            // 랙 경고
                            listener.onMessageReceived(
                                P2PMessage.lagWarning("네트워크 지연: " + (now - lastReceiveTime) + "ms")
                            );
                        }
                    }
                }
            } catch (Exception e) {
                listener.onNetworkError(e);
            } finally {
                closeSilently();
            }
        }, "P2P-ReadLoop").start();
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
