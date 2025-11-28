package se.tetris.team3.gameManager;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;

/**
 * 대전 모드를 관리하는 클래스.
 * 2명의 플레이어(왼쪽/오른쪽)를 위한 별도의 GameManager를 관리하고,
 * 한 플레이어가 줄을 삭제하면 상대방 보드에 공격 줄을 추가합니다.
 * 시간제한 모드에서는 타이머를 관리하고, 시간 종료 시 점수로 승패를 결정합니다.
 */
public class BattleGameManager implements LineClearListener {
    // 왼쪽 플레이어(Player 1)와 오른쪽 플레이어(Player 2)의 게임 매니저
    private final GameManager player1Manager;
    private final GameManager player2Manager;
    
    // 대전 모드 종류 (일반/아이템/시간제한)
    private final GameMode battleMode;
    
    // 시간제한 모드 관련
    private final boolean isTimeAttack;
    private final long timeLimit;  // 밀리초 단위 (예: 180000 = 3분)
    private long startTime;
    
    // 게임 상태
    private boolean gameOver;
    private int winner;  // 0: 무승부, 1: Player1 승리, 2: Player2 승리
    
    /**
     * BattleGameManager 생성자
     * @param mode 대전 모드 (BATTLE_NORMAL, BATTLE_ITEM, BATTLE_TIME)
     * @param settings 게임 설정
     * @param timeLimitSeconds 시간제한 모드일 경우 제한 시간(초), 다른 모드는 0
     */
    public BattleGameManager(GameMode mode, Settings settings, int timeLimitSeconds) {
        this.battleMode = mode;
        
        // 시간제한 모드 여부 및 제한 시간 설정
        this.isTimeAttack = (mode == GameMode.BATTLE_TIME);
        this.timeLimit = timeLimitSeconds * 1000L;
        
        // 각 플레이어용 GameManager 생성
        // 대전 모드에 따라 일반 모드 또는 아이템 모드로 내부 GameManager 생성
        GameMode internalMode = (mode == GameMode.BATTLE_ITEM) ? GameMode.ITEM : GameMode.CLASSIC;
        
        player1Manager = new GameManager(internalMode);
        player2Manager = new GameManager(internalMode);
        
        // 설정 적용
        if (settings != null) {
            player1Manager.attachSettings(settings);
            player2Manager.attachSettings(settings);
        }
        
        gameOver = false;
        winner = 0;
        startTime = System.currentTimeMillis();

        player1Manager.setLineClearListener((gm, clearedRows, garbageRows) -> 
            handleAttackFromPlayer(1, garbageRows)
        );
        player2Manager.setLineClearListener((gm, clearedRows, garbageRows) -> 
            handleAttackFromPlayer(2, garbageRows)
        );        
    }

    // 상대방에게 공격 줄을 보내는 내부 처리 메서드
    private void handleAttackFromPlayer(int attacker, boolean[][] garbageRows) {
        if (garbageRows == null || garbageRows.length == 0) return;

        GameManager defender = (attacker == 1) ? player2Manager : player1Manager;
        if (defender == null) return;

        // 10줄 제한은 GameManager.enqueueGarbage() 안에서 처리
        defender.enqueueGarbage(garbageRows);
    }
    
    /**
     * 게임 시작 시 호출 (타이머 시작)
     */
    public void start() {
        startTime = System.currentTimeMillis();
        gameOver = false;
        winner = 0;
    }
    
    /**
     * 매 프레임마다 호출되어 게임 상태를 업데이트합니다.
     * - 각 플레이어의 게임 오버 체크
     * - 시간제한 모드의 타이머 체크
     * - 승패 판정
     */
    public void update() {
        if (gameOver) return;
        
        // 느린 모드 등 아이템 효과 업데이트
        player1Manager.updateSlowMode();
        player2Manager.updateSlowMode();
        player1Manager.updateParticles();
        player2Manager.updateParticles();
        
        // 각 플레이어의 게임 오버 체크
        boolean p1Over = player1Manager.isGameOver();
        boolean p2Over = player2Manager.isGameOver();
        
        // 한 쪽이라도 게임 오버면 승패 판정
        if (p1Over || p2Over) {
            gameOver = true;
            if (p1Over && p2Over) {
                winner = 0;  // 동시 게임 오버 = 무승부 (거의 없음)
            } else if (p1Over) {
                winner = 2;  // Player1 오버 = Player2 승리
            } else {
                winner = 1;  // Player2 오버 = Player1 승리
            }
            return;
        }
        
        // 시간제한 모드: 시간 종료 체크
        if (isTimeAttack) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeLimit) {
                gameOver = true;
                // 점수 비교로 승자 결정
                int p1Score = player1Manager.getScore();
                int p2Score = player2Manager.getScore();
                if (p1Score > p2Score) {
                    winner = 1;
                } else if (p2Score > p1Score) {
                    winner = 2;
                } else {
                    winner = 0;  // 동점 = 무승부
                }
            }
        }
    }

    /**
     * 한 GameManager가 2줄 이상을 한 번에 삭제했을 때 호출됨.
     * 여기서 상대 플레이어에게 넘어갈 쓰레기 줄을 큐에 추가한다.
     *
     * @param attacker    줄을 지운 GameManager (player1 or player2)
     * @param clearedRows 실제 삭제된 줄 인덱스들 (0 = 위쪽)
     * @param garbageRows 공격 줄 패턴 (true = 블록, false = 빈칸)
     */
    @Override
    public void onAttack(GameManager attacker, int[] clearedRows, boolean[][] garbageRows) {
        if (garbageRows == null || garbageRows.length == 0 || attacker == null) return;

        int attackerId =
            (attacker == player1Manager) ? 1 :
            (attacker == player2Manager) ? 2 : -1;

        if (attackerId == -1) return;

        handleAttackFromPlayer(attackerId, garbageRows);
    }


    // Getter 메서드들
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
     * @return 0: 무승부, 1: Player1 승리, 2: Player2 승리
     */
    public int getWinner() {
        return winner;
    }
    
    /**
     * 시간제한 모드에서 남은 시간(초) 반환
     * @return 남은 시간(초), 시간제한 모드가 아니면 0
     */
    public int getRemainingTimeSeconds() {
        if (!isTimeAttack) return 0;
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = timeLimit - elapsed;
        if (remaining < 0) remaining = 0;
        return (int) (remaining / 1000);
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
