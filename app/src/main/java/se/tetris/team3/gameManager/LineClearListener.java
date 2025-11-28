package se.tetris.team3.gameManager;

/**
 * GameManager가 줄을 삭제했을 때 BattleGameManager에 알려주기 위한 콜백
 */
public interface LineClearListener {

    /**
     * @param attacker    줄을 지운 쪽 GameManager (Player1 또는 Player2)
     * @param clearedRows 실제 삭제된 줄 인덱스들 (0 = 맨 위)
     * @param garbageRows 상대에게 보낼 쓰레기 줄 패턴
     *                    garbageRows[i][x] == true  → 블럭
     *                    garbageRows[i][x] == false → 빈칸
     */
    void onAttack(GameManager attacker, int[] clearedRows, boolean[][] garbageRows);
}
