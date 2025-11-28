package se.tetris.team3.items;

import se.tetris.team3.gameManager.GameManager;

// 아이템 공통 인터페이스
// 필요 Hook만 default로 제공: onFalling(낙하 중), onLock(고정 시)
public interface Item {
    // 낙하 중 효과가 필요할 때 (무게추)
    default void onFalling(GameManager gm) {}
    
    // 고정 시 효과가 필요할 때 (줄삭제)
    default void onLock(GameManager gm) {}

    // 각 아이템 구별하는 화면용 문자
    // ex) LineClear: 'L'
    char symbol();
}
