package se.tetris.team3.p2pTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.net.P2PMessage;
import se.tetris.team3.core.GameMode;

/**
 * P2P 메시지 팩토리 메서드와 toString 동작을 검증하는 단위 테스트 모음.
 * 각 팩토리(HELLO, HELLO_OK, MODE_INFO 등)가 올바른 Type과 필드를 세팅하는지 확인합니다.
 */
public class P2PMessageTest {

    @Test
    void testFactoryMethodsAndToString() {
        // HELLO 메시지 생성 검증: 타입 및 버전 텍스트 포함 여부
        P2PMessage m1 = P2PMessage.hello();
        assertEquals(P2PMessage.Type.HELLO, m1.type);
        assertNotNull(m1.text);
        assertTrue(m1.text.contains("SE-TETRIS"));

        // HELLO_OK
        P2PMessage m2 = P2PMessage.helloOk();
        assertEquals(P2PMessage.Type.HELLO_OK, m2.type);

        // MODE_INFO: 전달된 게임모드와 시간 제한이 보존되는지 확인
        P2PMessage m3 = P2PMessage.modeInfo(GameMode.BATTLE_TIME, 120);
        assertEquals(P2PMessage.Type.MODE_INFO, m3.type);
        assertEquals(GameMode.BATTLE_TIME, m3.gameMode);
        assertEquals(120, m3.timeLimitSeconds);

        // READY_STATE
        P2PMessage m4 = P2PMessage.ready(true);
        assertEquals(P2PMessage.Type.READY_STATE, m4.type);
        assertTrue(m4.ready);

        // ATTACK: 쓰레기 줄 배열이 메시지에 포함되는지
        P2PMessage m5 = P2PMessage.attack(new boolean[][]{{true,true}});
        assertEquals(P2PMessage.Type.ATTACK, m5.type);
        assertNotNull(m5.garbageRows);

        // LAG_WARNING
        P2PMessage m7 = P2PMessage.lagWarning("lag");
        assertEquals(P2PMessage.Type.LAG_WARNING, m7.type);

        // ERROR 및 toString 포함 여부 확인
        P2PMessage err = P2PMessage.error("boom");
        assertEquals(P2PMessage.Type.ERROR, err.type);
        assertTrue(err.toString().toLowerCase().contains("error"));
    }
}
