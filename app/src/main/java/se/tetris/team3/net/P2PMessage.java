package se.tetris.team3.net;

import java.awt.Color;
import java.io.Serializable;

import se.tetris.team3.core.GameMode;

/**
 * P2P 통신에 사용되는 직렬화 가능한 메시지 객체.
 * - 로비: HELLO, HELLO_OK, MODE_INFO, READY_STATE, GAME_START, ERROR, DISCONNECT
 * - 게임: STATE(선택), ATTACK
 * - 랙 경고: LAG_WARNING (텍스트만)
 */
public class P2PMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        HELLO,          // 연결 직후 인사 + 버전
        HELLO_OK,       // 서버가 클라이언트의 HELLO 수락
        MODE_INFO,      // (서버→클라이언트) 선택된 모드/시간제한 전달
        READY_STATE,    // 양쪽 준비 상태(true/false)
        GAME_START,     // 실제 게임 시작
        INPUT,          // (현재 사용 안 함)
        STATE,          // 게임 상태 스냅샷
        ATTACK,         // 쓰레기 줄 정보
        LAG_WARNING,    // 랙 경고 메시지
        ERROR,          // 오류 메시지
        DISCONNECT,      // 연결 종료
        PAUSE_STATE     // 일시정지 상태
    }

    public Type type;

    /** 공통 텍스트 필드 (버전, 상태, 에러, 랙 경고 이유 등) */
    public String text;

    // MODE_INFO 
    public GameMode gameMode;
    public int timeLimitSeconds;

    // READY_STATE 
    public boolean ready;

    // ATTACK
    public boolean[][] garbageRows;

    // PAUSE_STATE
    public boolean paused;

    // STATE (게임 중 상태 스냅샷) 
    public int myScore;
    public int myLevel;
    public boolean gameOver;

    public int[][] field;           // 고정 블럭
    public char[][] itemField;      // 각 칸의 아이템 정보

    public int[][] curShape;        // 현재 조작 중 블럭 모양
    public Color   curColor;        // 현재 블럭 색
    public int     curX, curY;      // 현재 블럭 위치
    public char    curItemType;     // 현재 블럭에 박힌 아이템 타입
    public int     curItemRow;      // 그 아이템의 shape 내부 row
    public int     curItemCol;      // 그 아이템의 shape 내부 col

    public int[][] nextShape;       // 다음 블럭 모양
    public Color   nextColor;       // 다음 블럭 색
    public char    nextItemType;    // 다음 블럭 아이템 타입
    public int     nextItemRow;     // 다음 블럭 아이템 row
    public int     nextItemCol;     // 다음 블럭 아이템 col

    public boolean[][] garbagePreview; // 대기 중인 공격 줄 미리보기

    // 정적 헬퍼 생성 메서드 

    /** 버전 정보 포함 HELLO */
    public static P2PMessage hello() {
        P2PMessage m = new P2PMessage();
        m.type = Type.HELLO;
        m.text = "SE-TETRIS v1";
        return m;
    }

    public static P2PMessage helloOk() {
        P2PMessage m = new P2PMessage();
        m.type = Type.HELLO_OK;
        return m;
    }

    public static P2PMessage modeInfo(GameMode mode, int timeLimitSeconds) {
        P2PMessage m = new P2PMessage();
        m.type = Type.MODE_INFO;
        m.gameMode = mode;
        m.timeLimitSeconds = timeLimitSeconds;
        return m;
    }

    public static P2PMessage ready(boolean ready) {
        P2PMessage m = new P2PMessage();
        m.type = Type.READY_STATE;
        m.ready = ready;
        return m;
    }

    public static P2PMessage gameStart() {
        P2PMessage m = new P2PMessage();
        m.type = Type.GAME_START;
        return m;
    }

    public static P2PMessage attack(boolean[][] rows) {
        P2PMessage m = new P2PMessage();
        m.type = Type.ATTACK;
        m.garbageRows = rows;
        return m;
    }

        public static P2PMessage pauseState(boolean paused) {
        P2PMessage m = new P2PMessage();
        m.type = Type.PAUSE_STATE;
        m.paused = paused;
        return m;
    }

    /** 랙 경고용 메시지 (텍스트만 사용) */
    public static P2PMessage lagWarning(String text) {
        P2PMessage m = new P2PMessage();
        m.type = Type.LAG_WARNING;
        m.text = text;
        return m;
    }

    public static P2PMessage error(String text) {
        P2PMessage m = new P2PMessage();
        m.type = Type.ERROR;
        m.text = text;
        return m;
    }

    public static P2PMessage disconnect(String reason) {
        P2PMessage m = new P2PMessage();
        m.type = Type.DISCONNECT;
        m.text = reason;
        return m;
    }

    public static P2PMessage emptyState() {
        P2PMessage m = new P2PMessage();
        m.type = Type.STATE;
        // field, shapes 등이 null인 빈 상태 전송
        return m;
    }

    @Override
    public String toString() {
        return "P2PMessage{" + type + ", text=" + text + "}";
    }
}

