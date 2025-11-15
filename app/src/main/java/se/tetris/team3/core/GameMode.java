package se.tetris.team3.core;

// ScoreManager, Settings, MenuScreen, GameManager 등 전역으로 참조.
// 대전 모드: BATTLE_NORMAL(일반 대전), BATTLE_ITEM(아이템 대전), BATTLE_TIME(시간제한 대전)
public enum GameMode {
    CLASSIC, ITEM, BATTLE_NORMAL, BATTLE_ITEM, BATTLE_TIME
}
