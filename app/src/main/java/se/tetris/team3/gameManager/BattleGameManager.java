package se.tetris.team3.gameManager;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;

/**
 * 대전 모드를 관리하는 클래스.
 * 2명의 플레이어(왼쪽/오른쪽)를 위한 GameManager를 관리하고,
 * 한 플레이어가 줄을 삭제하면 상대방 보드에 공격 줄을 추가합니다.
 * 시간제한 모드에서는 타이머를 관리하고, 시간 종료 시 점수로 승패를 결정합니다.
 */
public class BattleGameManager {

    // 왼쪽 플레이어(Player 1)와 오른쪽 플레이어(Player 2)
    private final GameManager player1Manager;
    private final GameManager player2Manager;

    // 대전 모드 종류 (일반/아이템/시간제한)
    private final GameMode battleMode;

    // 시간제한 모드
    private final boolean isTimeAttack;
    private final long timeLimit;  // ms 단위
    private long startTime;

    // 게임 상태
    private boolean gameOver;
    private int winner; // 0: 무승부, 1: Player1 승리, 2: Player2 승리

    /**
     * BattleGameManager 생성자
     */
    public BattleGameManager(GameMode mode, Settings settings, int timeLimitSeconds) {
        this.battleMode = mode;

        // 시간제한 모드 여부
        this.isTimeAttack = (mode == GameMode.BATTLE_TIME);
        this.timeLimit = timeLimitSeconds * 1000L;

        // 내부 GameManager 모드 선택
        GameMode internalMode =
            (mode == GameMode.BATTLE_ITEM) ? GameMode.ITEM : GameMode.CLASSIC;

        player1Manager = new GameManager(internalMode);
        player2Manager = new GameManager(internalMode);

        // 공통 설정 적용
        if (settings != null) {
            player1Manager.attachSettings(settings);
            player2Manager.attachSettings(settings);
        }

        gameOver = false;
        winner = 0;
        startTime = System.currentTimeMillis();

        // 줄 삭제 → 공격 이벤트 연결 (람다 리스너)
        player1Manager.setLineClearListener(
            (gm, cleared, garbage) -> handleAttackFromPlayer(1, garbage)
        );

        player2Manager.setLineClearListener(
            (gm, cleared, garbage) -> handleAttackFromPlayer(2, garbage)
        );
    }

    /**
     * 공격 처리 (상대방에게 garbage row 전송)
     */
    private void handleAttackFromPlayer(int attacker, boolean[][] garbageRows) {
        if (garbageRows == null || garbageRows.length == 0) return;

        GameManager defender =
            (attacker == 1) ? player2Manager : player1Manager;

        // GameManager 내부 enqueueGarbage() 에서 10줄 제한을 처리함
        defender.enqueueGarbage(garbageRows);
    }

    /**
     * 게임 시작 시 호출
     */
    public void start() {
        startTime = System.currentTimeMillis();
        gameOver = false;
        winner = 0;
    }

    /**
     * 매 프레임마다 호출되어 게임 상태 갱신
     */
    public void update() {
        if (gameOver) return;

        // 슬로우 모드/파티클 업데이트
        player1Manager.updateSlowMode();
        player2Manager.updateSlowMode();
        player1Manager.updateParticles();
        player2Manager.updateParticles();

        // 게임 오버 체크
        boolean p1Over = player1Manager.isGameOver();
        boolean p2Over = player2Manager.isGameOver();

        if (p1Over || p2Over) {
            gameOver = true;

            if (p1Over && p2Over) {
                winner = 0; // 무승부
            } else if (p1Over) {
                winner = 2; // Player2 승
            } else {
                winner = 1; // Player1 승
            }
            return;
        }

        // 시간제한 모드: 타이머 체크
        if (isTimeAttack) {
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed >= timeLimit) {
                gameOver = true;

                int p1Score = player1Manager.getScore();
                int p2Score = player2Manager.getScore();

                if (p1Score > p2Score)      winner = 1;
                else if (p2Score > p1Score) winner = 2;
                else                        winner = 0; // 무승부
            }
        }
    }

    // Getter

    public GameManager getPlayer1Manager() {
        return player1Manager;
    }

    public GameManager getPlayer2Manager() {
        return player2Manager;
    }

    public GameMode getBattleMode() {
        return battleMode;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * 승자 반환  
     * 0 = 무승부, 1 = Player1, 2 = Player2
     */
    public int getWinner() {
        return winner;
    }

    /**
     * 시간제한 모드에서 남은 시간(초)
     */
    public int getRemainingTimeSeconds() {
        if (!isTimeAttack) return 0;

        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = timeLimit - elapsed;

        if (remaining < 0) remaining = 0;
        return (int)(remaining / 1000);
    }

    /**
     * 게임 재시작
     */
    public void restart() {
        player1Manager.resetGame();
        player2Manager.resetGame();

        startTime = System.currentTimeMillis();
        gameOver = false;
        winner = 0;
    }
}
