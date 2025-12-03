package se.tetris.team3.ui.screen;

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
import se.tetris.team3.ui.AppFrame;

public class P2PLobbyScreen implements Screen, P2PConnectionListener {

    private final AppFrame frame;
    private final Settings settings;

    private enum Phase {
        ROLE_SELECT,        // 서버/클라 선택
        SERVER_WAIT,        // 서버 대기중(accept 전에)
        CLIENT_INPUT_IP,    // 클라 IP 입력중
        CONNECTING,         // 클라 연결 시도중
        LOBBY               // 양쪽 연결 완료
    }

    private Phase phase = Phase.ROLE_SELECT;
    private boolean asServer;

    // 여기 connection 은 "이 로비가 관리하는 연결" 하나 뿐
    private P2PConnection connection;
    private boolean connectionOwnedByThis = false;   // 외부에서 넘어온 connection 인지 여부
    private boolean goingIntoBattle = false;         // onHide에서 connection을 닫을지 말지 판단

    // 클라이언트 IP 입력
    private String inputIP = "";
    private int ipCursorBlink = 0;

    // 로비 상태
    private boolean connected = false;
    private boolean myReady = false;
    private boolean otherReady = false;
    private String statusMessage = "";
    private boolean receivedInitialModeInfo = false;  // 초기 MODE_INFO 수신 여부

    // 서버 전용 설정
    private int selectedModeIndex = 0;   // 0=Normal,1=Item,2=TimeAttack
    private int timeLimitMin = 3;

    // 실제 사용할 모드/시간 (클라에서도 이 값을 표시)
    private GameMode lobbyMode = GameMode.BATTLE_NORMAL;
    private int lobbyTimeLimitSeconds = 0;

    // 최근 IP
    private final List<String> recentIPs =
            new ArrayList<>(SettingsStore.getRecentP2PIPs());

    // ────────── 생성자 ──────────

    // 새 P2P 세션 시작용 (Menu → Lobby)
    public P2PLobbyScreen(AppFrame frame, Settings settings) {
        this(frame, settings, null);
    }

    // 이미 존재하는 연결 재사용용 (예: Battle → 다시 Lobby)
    public P2PLobbyScreen(AppFrame frame, Settings settings, P2PConnection existing) {
        this.frame = frame;
        this.settings = settings;
        this.connection = existing;
        this.connectionOwnedByThis = (existing == null); // null이면 여기서 새로 만든다는 뜻
        System.out.println("CTOR LOBBY this=" + this + " existingConn=" + existing);
    }

    // 게임 종료 후 로비 복귀용
    public static P2PLobbyScreen reopenAfterGame(
            AppFrame frame,
            Settings settings,
            P2PConnection connection,
            boolean asServer
    ) {
        P2PLobbyScreen lobby = new P2PLobbyScreen(frame, settings);
        lobby.connection = connection;
        lobby.asServer = asServer;

        lobby.connected = true;
        lobby.phase = Phase.LOBBY;
        lobby.myReady = false;
        lobby.otherReady = false;
        lobby.statusMessage = "연결 완료 (" + (asServer ? "Server" : "Client") + ")";

        if (connection != null) {
            connection.setListener(lobby);
            // 로비에서는 타임아웃 끔
            connection.setIdleTimeoutEnabled(false);
        }

        return lobby;
    }

    // 네트워크 에러 후, 처음 P2P 진입 화면으로 돌아올 때 사용하는 생성 헬퍼
    public static P2PLobbyScreen createAfterError(
            AppFrame frame,
            Settings settings,
            String errorMessage
    ) {
        P2PLobbyScreen lobby = new P2PLobbyScreen(frame, settings);

        // 에러 상황에서 들어온 것이므로, 항상 처음 화면(ROLE_SELECT)부터 시작
        lobby.phase = Phase.ROLE_SELECT;
        lobby.asServer = false;
        lobby.connected = false;
        lobby.myReady = false;
        lobby.otherReady = false;

        // 상단에 에러 메시지를 보여줌
        lobby.statusMessage = (errorMessage == null ? "" : errorMessage);

        return lobby;
    }

    @Override
    public void onShow() {
        System.out.println("onShow LOBBY this=" + this + " phase=" + phase);

        // 기존 connection 재사용 or 새로 만들기
        if (connection == null) {
            connection = new P2PConnection();
            connectionOwnedByThis = true;
        }
        connection.setListener(this);
    }

    @Override
    public void onHide() {
        System.out.println("onHide LOBBY this=" + this +
                " goingIntoBattle=" + goingIntoBattle +
                " owned=" + connectionOwnedByThis);

        // 배틀 화면으로 넘어가는 경우에는 연결을 계속 사용해야 하므로 닫지 않는다.
        if (!goingIntoBattle && connectionOwnedByThis && connection != null) {
            try { connection.close(); } catch (Exception ignore) {}
        }
    }

    // ────────── P2PConnectionListener ──────────

    @Override
    public void onConnected(boolean asServer) {
        System.out.println("onConnected this=" + this + " asServer=" + asServer);

        this.asServer = asServer;
        this.connected = true;
        this.phase = Phase.LOBBY;
        this.statusMessage = "연결 완료 (" + (asServer ? "Server" : "Client") + ")";

        // 클라이언트가 성공적으로 접속했다면 최근 IP 저장
        if (!asServer && inputIP != null && !inputIP.trim().isEmpty()) {
            SettingsStore.addRecentP2PIP(inputIP.trim());
        }

        // 간단한 핸드셰이크
        if (asServer) {
            connection.send(P2PMessage.hello());
            connection.send(P2PMessage.modeInfo(lobbyMode, lobbyTimeLimitSeconds));
        } else {
            connection.send(P2PMessage.hello());
        }

        frame.repaint();
    }

    @Override
    public void onDisconnected(String reason) {
        connected = false;
        statusMessage = reason;
        frame.repaint();
    }

    @Override
    public void onMessageReceived(P2PMessage msg) {
        switch (msg.type) {
            case HELLO:
                if (connection.isServer()) {
                    connection.send(P2PMessage.helloOk());
                }
                break;
            case HELLO_OK:
                // 필요시 상태 표시만
                break;
            case MODE_INFO:
                lobbyMode = msg.gameMode;
                lobbyTimeLimitSeconds = msg.timeLimitSeconds;
                // 초기 연결 시 받는 첫 MODE_INFO는 메시지 표시 안 함
                if (receivedInitialModeInfo) {
                    statusMessage = "상대가 모드를 선택했습니다.";
                } else {
                    receivedInitialModeInfo = true;
                }
                break;
            case READY_STATE:
                otherReady = msg.ready;
                break;
            case GAME_START:
                startGameFromLobby();
                break;
            case ATTACK:
                // 로비에서는 무시
                break;
            case ERROR:
            case DISCONNECT:
                statusMessage = msg.text;
                break;
            default:
                break;
        }
        frame.repaint();
    }

    @Override
    public void onNetworkError(Exception e) {
        connected = false;
        String errorMsg = e.getMessage();
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            errorMsg = e.getClass().getSimpleName();
        }
        statusMessage = "네트워크 오류: " + errorMsg;
        frame.repaint();
    }

    // ────────── 입력 처리 ──────────

    @Override
    public void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (phase == Phase.ROLE_SELECT) {
            handleRoleSelectKey(key);
            return;
        }

        if (phase == Phase.CLIENT_INPUT_IP) {
            handleClientIpInput(key, e);
            return;
        }

        if (phase == Phase.LOBBY && connected) {
            handleLobbyInput(key);
            return;
        }

        if (key == KeyEvent.VK_ESCAPE) {
            // 완전히 메뉴로 빠져나가는 경우 → 여기서 connection은 onHide에서 닫힘
            goingIntoBattle = false;
            frame.showScreen(new MenuScreen(frame));
        }
    }

    private void handleRoleSelectKey(int key) {
        if (key == KeyEvent.VK_1 || key == KeyEvent.VK_S) {
            asServer = true;
            phase = Phase.SERVER_WAIT;
            statusMessage = "서버 모드: 접속 대기중...";

            if (connection == null) {
                connection = new P2PConnection();
                connectionOwnedByThis = true;
            }
            connection.setListener(this);
            connection.setIdleTimeoutEnabled(false);   // 로비에서는 끔
            connection.startServer();

        } else if (key == KeyEvent.VK_2 || key == KeyEvent.VK_C) {
            asServer = false;
            phase = Phase.CLIENT_INPUT_IP;
            statusMessage = "클라이언트 모드: IP 입력 후 Enter";
        } else if (key == KeyEvent.VK_ESCAPE) {
            goingIntoBattle = false;
            frame.showScreen(new MenuScreen(frame));
        }
    }

    private void handleClientIpInput(int key, KeyEvent e) {
        if (key == KeyEvent.VK_ENTER) {
            if (!inputIP.isEmpty()) {
                statusMessage = "서버 접속 중...";
                phase = Phase.CONNECTING;

                if (connection == null) {
                    connection = new P2PConnection();
                    connectionOwnedByThis = true;
                }
                connection.setListener(this);
                connection.setIdleTimeoutEnabled(false);   // 로비에서는 끔
                connection.connectTo(inputIP.trim());
            }
        } else if (key == KeyEvent.VK_BACK_SPACE) {
            if (!inputIP.isEmpty()) {
                inputIP = inputIP.substring(0, inputIP.length() - 1);
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            goingIntoBattle = false;
            frame.showScreen(new MenuScreen(frame));
        } else {
            char ch = e.getKeyChar();
            if (Character.isDigit(ch) || ch == '.' || ch == ':') {
                inputIP += ch;
            }
        }
    }

    private void handleLobbyInput(int key) {
        // READY 토글
        if (key == KeyEvent.VK_ENTER) {
            myReady = !myReady;
            connection.send(P2PMessage.ready(myReady));
            return;
        }

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
                if (myReady && otherReady) {
                    connection.send(P2PMessage.gameStart());
                    startGameFromLobby();
                }
            }
        }

        if (key == KeyEvent.VK_ESCAPE) {
            goingIntoBattle = false;
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
        goingIntoBattle = true;

        if (connection != null) {
        connection.setIdleTimeoutEnabled(true);
        }

        P2PBattleScreen battle = new P2PBattleScreen(
                frame,
                connection,
                lobbyMode,
                settings,
                lobbyTimeLimitSeconds,
                asServer
        );
        // BattleScreen 쪽에서 connection.setListener(this)를 다시 호출함
        frame.showScreen(battle);
    }

    // ────────── 렌더링 ──────────

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

        // 에러 / 안내 메시지 표시
        if (statusMessage != null && !statusMessage.isEmpty()) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            drawCenterLine(g2, w, 140, statusMessage);
        }

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

        // ip 주소 렌더링
        if (asServer && connection != null) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));

            String ipText = "서버 IP: " + connection.getLocalAddress();
            drawCenterLine(g2, w, 130, ipText);
        }

        if (phase == Phase.CLIENT_INPUT_IP) {
            drawClientIpInput(g2, w, h);
            return;
        }

        int leftX = 60;
        int y = 170;
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        g2.drawString("모드 설정 (서버만 네트워크 연결 후 변경 가능)", leftX, y); y += 30;

        String[] modeNames = {"Normal Battle", "Item Battle", "Time Attack"};
        String modeStr = modeNames[selectedModeIndex];
        if (!asServer) {
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
        g2.drawString("READY(Enter) : " + (myReady ? "READY" : "NOT READY"), leftX, y); y += 28;
        g2.drawString("상대 상태 : " + (otherReady ? "READY" : "NOT READY"), leftX, y); y += 40;

        y += 10;

        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);

        if (asServer) {
            g2.drawString("Space : 양쪽 READY 이면 게임 시작", leftX, y); y += 22;
            g2.drawString("↑/↓ 또는 W/S : 모드 선택", leftX, y); y += 22;
            g2.drawString("←/→ 또는 A/D : 시간 조정(Time Attack)", leftX, y); y += 22;
        }

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
        drawCenterLine(g2, w, 260,
                "예: 192.168.0.10  또는  192.168.0.10:34567 (포트는 옵션)");
        drawCenterLine(g2, w, 300, "ESC: 취소");

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
