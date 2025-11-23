package se.tetris.team3.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.net.P2PConnectionListener;
import se.tetris.team3.net.P2PMessage;
import se.tetris.team3.store.SettingsStore;

/**
 * P2P 대전 모드 로비 화면
 * - 서버 / 클라이언트 선택
 * - 서버: IP 표시 후 대기
 * - 클라이언트: IP 입력 후 접속
 * - 연결 이후: READY 버튼 + 모드 선택(서버만)
 */
public class P2PLobbyScreen implements Screen, P2PConnectionListener {

    private final AppFrame frame;
    private final Settings settings;

    private enum Phase {
        ROLE_SELECT,
        SERVER_WAIT,
        CLIENT_INPUT_IP,
        CONNECTING,
        LOBBY
    }

    private Phase phase = Phase.ROLE_SELECT;
    private boolean asServer;

    private P2PConnection connection;

    // 클라이언트 IP 입력용
    private String inputIP = "";
    private int ipCursorBlink = 0;

    // 로비 상태
    private boolean connected = false;
    private boolean myReady = false;
    private boolean otherReady = false;
    private String statusMessage = "";

    // 서버에서만 사용하는 모드/시간 설정
    private int selectedModeIndex = 0;  // 0=Normal,1=Item,2=TimeAttack
    private int timeLimitMin = 3;

    // 서버가 선택한 모드 정보(클라이언트 쪽에서 표시용)
    private GameMode lobbyMode = GameMode.BATTLE_NORMAL;
    private int lobbyTimeLimitSeconds = 0;

    // 최근 IP
    List<String> recentIPs = new ArrayList<>(SettingsStore.getRecentP2PIPs());

    public P2PLobbyScreen(AppFrame frame, Settings settings) {
        this.frame = frame;
        this.settings = settings;
    }

    @Override
    public void onShow() {
        // nothing
    }

    @Override
    public void onHide() {
        if (connection != null) {
            try { connection.close(); } catch (Exception ignore) {}
            connection = null;
        }
    }

    //  P2PConnectionListener
    @Override
    public void onConnected(boolean asServer) {
        this.asServer = asServer;
        connected = true;
        phase = Phase.LOBBY;
        statusMessage = "연결 완료 (" + (asServer ? "Server" : "Client") + ")";

        // 클라이언트가 접속에 성공했을 때 최근 IP 저장
        if (!asServer) {
            SettingsStore.addRecentP2PIP(inputIP);
        }

        // 초기 handshake
        if (asServer) {
            connection.send(P2PMessage.hello());
            // 현재 선택 중인 모드 정보를 보내 둔다.
            connection.send(P2PMessage.modeInfo(lobbyMode, lobbyTimeLimitSeconds));
        } else {
            connection.send(P2PMessage.hello());
        }
    }

    @Override
    public void onDisconnected(String reason) {
        connected = false;
        statusMessage = "연결이 끊어졌습니다: " + reason;
    }

    @Override
    public void onMessageReceived(P2PMessage msg) {
        switch (msg.type) {
            case HELLO:
                // 상대방 인사에 응답 (서버가 HELLO_OK 보내는 식으로 써도 됨)
                if (connection.isServer()) {
                    connection.send(P2PMessage.helloOk());
                }
                break;
            case HELLO_OK:
                // optional
                break;
            case MODE_INFO:
                lobbyMode = msg.gameMode;
                lobbyTimeLimitSeconds = msg.timeLimitSeconds;
                statusMessage = "상대가 모드를 선택했습니다.";
                break;
            case READY_STATE:
                otherReady = msg.ready;
                break;
            case GAME_START:
                startGameFromLobby();
                break;
            case ATTACK:
                // 아직 게임 화면이 아니라 로비이므로 무시.
                break;
            case ERROR:
            case DISCONNECT:
                statusMessage = msg.text;
                break;
            default:
                break;
        }
    }

    @Override
    public void onNetworkError(Exception e) {
        statusMessage = "네트워크 오류: " + e.getMessage();
    }

    // -------------------- 입력 처리 --------------------
    @Override
    public void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (phase == Phase.ROLE_SELECT) {
            if (key == KeyEvent.VK_1 || key == KeyEvent.VK_S) {
                asServer = true;
                phase = Phase.SERVER_WAIT;
                statusMessage = "서버 모드: 접속 대기중...";
                connection = new P2PConnection(this);
                connection.startServer();
            } else if (key == KeyEvent.VK_2 || key == KeyEvent.VK_C) {
                asServer = false;
                phase = Phase.CLIENT_INPUT_IP;
                statusMessage = "클라이언트 모드: IP 입력 후 Enter";
            } else if (key == KeyEvent.VK_ESCAPE) {
                frame.showScreen(new MenuScreen(frame));
            }
            return;
        }

        if (phase == Phase.CLIENT_INPUT_IP) {
            handleClientIpInput(key, e);
            return;
        }

        if (phase == Phase.LOBBY && connected) {
            handleLobbyInput(key, e);
            return;
        }

        if (key == KeyEvent.VK_ESCAPE) {
            frame.showScreen(new MenuScreen(frame));
        }
    }

    private void handleClientIpInput(int key, KeyEvent e) {
        if (key == KeyEvent.VK_ENTER) {
            if (!inputIP.isEmpty()) {
                statusMessage = "서버 접속 중...";
                phase = Phase.CONNECTING;
                SettingsStore.addRecentP2PIP(inputIP);
                connection = new P2PConnection(this);
                connection.connectTo(inputIP.trim());
            }
        } else if (key == KeyEvent.VK_BACK_SPACE) {
            if (!inputIP.isEmpty()) {
                inputIP = inputIP.substring(0, inputIP.length() - 1);
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            frame.showScreen(new MenuScreen(frame));
        } else {
            char ch = e.getKeyChar();
            if (Character.isDigit(ch) || ch == '.' || ch == ':') {
                inputIP += ch;
            }
        }
    }

    private void handleLobbyInput(int key, KeyEvent e) {

        // Enter : READY 토글만 사용 (채팅 입력 없음)
        if (key == KeyEvent.VK_ENTER) {
            myReady = !myReady;
            connection.send(P2PMessage.ready(myReady));
            return;
        }

        // 서버만 모드/시간 설정 가능
        if (asServer) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
                selectedModeIndex = (selectedModeIndex + 2) % 3;
                updateLobbyModeFromSelection();
            } else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                selectedModeIndex = (selectedModeIndex + 1) % 3;
                updateLobbyModeFromSelection();
            } else if (selectedModeIndex == 2 &&
                    (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A)) {
                if (timeLimitMin > 1) timeLimitMin--;
                updateLobbyModeFromSelection();
            } else if (selectedModeIndex == 2 &&
                    (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D)) {
                if (timeLimitMin < 10) timeLimitMin++;
                updateLobbyModeFromSelection();
            } else if (key == KeyEvent.VK_SPACE) {
                // 양쪽 ready 면 게임 시작
                if (myReady && otherReady) {
                    connection.send(P2PMessage.gameStart());
                    startGameFromLobby();
                }
            }
        }

        if (key == KeyEvent.VK_ESCAPE) {
            frame.showScreen(new MenuScreen(frame));
        }
    }

    private void updateLobbyModeFromSelection() {
        switch (selectedModeIndex) {
            case 0:
                lobbyMode = GameMode.BATTLE_NORMAL;
                lobbyTimeLimitSeconds = 0;
                break;
            case 1:
                lobbyMode = GameMode.BATTLE_ITEM;
                lobbyTimeLimitSeconds = 0;
                break;
            case 2:
                lobbyMode = GameMode.BATTLE_TIME;
                lobbyTimeLimitSeconds = timeLimitMin * 60;
                break;
        }
        if (connection != null) {
            connection.send(P2PMessage.modeInfo(lobbyMode, lobbyTimeLimitSeconds));
        }
    }

    private void startGameFromLobby() {
        // 실제 게임 화면으로 전환
        P2PBattleScreen battle = new P2PBattleScreen(
                frame,
                connection,
                lobbyMode,
                settings,
                lobbyTimeLimitSeconds,
                asServer
        );
        frame.showScreen(battle);
    }

    // -------------------- 렌더링 --------------------
    @Override
    public void render(Graphics2D g2) {
        int w = frame.getWidth();
        int h = frame.getHeight();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (phase) {
            case ROLE_SELECT:
                renderRoleSelect(g2, w, h);
                break;
            case SERVER_WAIT:
            case CONNECTING:
            case CLIENT_INPUT_IP:
            case LOBBY:
                renderLobby(g2, w, h);
                break;
        }
    }

    private void renderRoleSelect(Graphics2D g2, int w, int h) {
        g2.setColor(Color.CYAN);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 40));
        String title = "P2P BATTLE";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (w - tw) / 2, 100);

        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 24));
        drawCenterLine(g2, w, 200, "1 / S : 서버로 방 만들기");
        drawCenterLine(g2, w, 240, "2 / C : 클라이언트로 접속");
        drawCenterLine(g2, w, 280, "ESC : 메인 메뉴");
    }

    private void renderLobby(Graphics2D g2, int w, int h) {
        g2.setColor(Color.CYAN);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 32));
        String title = "P2P LOBBY - " + (asServer ? "SERVER" : "CLIENT");
        drawCenterLine(g2, w, 60, title);

        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        drawCenterLine(g2, w, 90, statusMessage);

        if (phase == Phase.CLIENT_INPUT_IP) {
            drawClientIpInput(g2, w, h);
            return;
        }

        // 좌측: 모드/상태
        int leftX = 60;
        int y = 140;
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        g2.drawString("모드 설정 (서버만 네트워크 연결 후 변경 가능)", leftX, y); y += 30;

        String[] modeNames = {"Normal Battle", "Item Battle", "Time Attack"};
        String modeStr = modeNames[selectedModeIndex];
        if (!asServer) {
            // 클라이언트는 서버가 선택한 실제 모드 기준으로 표시
            if (lobbyMode == GameMode.BATTLE_ITEM) modeStr = "Item Battle";
            else if (lobbyMode == GameMode.BATTLE_TIME) modeStr = "Time Attack";
            else modeStr = "Normal Battle";
        }
        g2.setColor(Color.YELLOW);
        g2.drawString("Mode: " + modeStr, leftX, y); y += 25;

        if ((asServer && selectedModeIndex == 2) ||
            (!asServer && lobbyMode == GameMode.BATTLE_TIME)) {
            g2.setColor(Color.ORANGE);
            int minutes = asServer ? timeLimitMin : lobbyTimeLimitSeconds / 60;
            g2.drawString("Time Limit: " + minutes + " min", leftX, y);
        }

        y += 40;

        g2.setFont(new Font("맑은 고딕", Font.BOLD, 18)); 
        g2.setColor(Color.LIGHT_GRAY);

        String myReadyStr    = "READY(Enter) : " + (myReady ? "READY" : "NOT READY");
        String otherReadyStr = "상대 상태 : "    + (otherReady ? "READY" : "NOT READY");
        
        g2.drawString(myReadyStr, leftX, y);  y += 28;
        g2.drawString(otherReadyStr, leftX, y);  y += 40;

        // READY 영역과 키 설명 사이 여백 조금 더
        y += 10;

        // 아래부터는 키 설명(조작법) - 폰트/색 살짝 줄이기
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);
        
        if (asServer) {
            g2.drawString("Space : 양쪽 READY 이면 게임 시작", leftX, y); y += 22;
            g2.drawString("↑/↓ 또는 W/S : 모드 선택", leftX, y); y += 22;
            g2.drawString("←/→ 또는 A/D : 시간 조정(Time Attack)", leftX, y); y += 22;
        } else {
            y += 20; // 서버 아니면 한 줄 띄워주기 정도
        }

        // ESC 안내
        g2.setColor(Color.GRAY);
        g2.drawString("ESC: 메인 메뉴로", 60, h - 40);
    }

    private void drawClientIpInput(Graphics2D g2, int w, int h) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        drawCenterLine(g2, w, 160, "접속할 서버 IP를 입력하고 Enter 를 누르세요.");

        String show = "IP: " + inputIP + ((ipCursorBlink / 20) % 2 == 0 ? "_" : "");
        ipCursorBlink++;
        drawCenterLine(g2, w, 200, show);

        g2.setColor(Color.GRAY);
        drawCenterLine(g2, w, 260, "예: 192.168.0.10  또는  192.168.0.10:34567 (포트는 옵션)");
        drawCenterLine(g2, w, 300, "ESC: 취소");

        // 최근 IP 목록
        if (!recentIPs.isEmpty()) {
            int y = 340;
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("최근 접속 IP:", 80, y); y += 20;
            for (String ip : recentIPs) {
                g2.drawString(" - " + ip, 100, y);
                y += 18;
            }
        }
    }

    private void drawCenterLine(Graphics2D g2, int w, int y, String text) {
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (w - tw) / 2, y);
    }
}
