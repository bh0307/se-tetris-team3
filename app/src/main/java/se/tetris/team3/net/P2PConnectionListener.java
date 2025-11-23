package se.tetris.team3.net;

/**
 * P2PConnection 이 수신한 메시지를 UI / 게임로직에 전달하기 위한 콜백.
 */
public interface P2PConnectionListener {

    void onConnected(boolean asServer);

    void onDisconnected(String reason);

    void onMessageReceived(P2PMessage msg);

    void onNetworkError(Exception e);
}
