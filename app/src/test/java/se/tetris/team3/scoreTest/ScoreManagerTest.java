package se.tetris.team3.scoreTest;

import org.junit.jupiter.api.Test;

import se.tetris.team3.ui.score.ScoreManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.Date;

public class ScoreManagerTest {
    
    private ScoreManager scoreManager;
    
    @BeforeEach
    void setUp() {
        // 테스트용 파일명으로 변경 (원본 파일 보호)
        deleteTestFile();
        scoreManager = new ScoreManager("scoreFile.txt");
    }
    
    @AfterEach
    void tearDown() {
        // 테스트 후 테스트 파일 정리
        deleteTestFile();
    }
    
    private void deleteTestFile() {
        File classicFile = new File("scoreFile.txt");
        File itemFile = new File("scoreFile.txt.item");
        if (classicFile.exists()) classicFile.delete();
        if (itemFile.exists()) itemFile.delete();
    }
    
    @Test
    @DisplayName("새로운 ScoreManager 생성 시 빈 점수 목록")
    void newScoreManagerShouldHaveEmptyScores() {
        List<ScoreManager.ScoreEntry> scores = scoreManager.getHighScores();
        assertTrue(scores.isEmpty(), "새 ScoreManager는 빈 점수 목록을 가져야 합니다");
    }
    
    @Test
    @DisplayName("점수 추가 및 정렬 테스트")
    void addScoreShouldAddAndSort() {
        // Given
        scoreManager.addScore("Alice", 1000);
        scoreManager.addScore("Bob", 2000);
        scoreManager.addScore("Charlie", 1500);
        
        // When
        List<ScoreManager.ScoreEntry> scores = scoreManager.getHighScores();
        
        // Then
        assertEquals(3, scores.size());
        assertEquals("Bob", scores.get(0).getPlayerName()); // 가장 높은 점수
        assertEquals(2000, scores.get(0).getScore());
        assertEquals("Charlie", scores.get(1).getPlayerName()); // 두 번째
        assertEquals(1500, scores.get(1).getScore());
        assertEquals("Alice", scores.get(2).getPlayerName()); // 가장 낮은 점수
        assertEquals(1000, scores.get(2).getScore());
    }
    
    @Test
    @DisplayName("최대 10개 점수만 유지")
    void shouldKeepOnlyTop10Scores() {
        // Given - 11개 점수 추가
        for (int i = 1; i <= 11; i++) {
            scoreManager.addScore("Player" + i, i * 100);
        }
        
        // When
        List<ScoreManager.ScoreEntry> scores = scoreManager.getHighScores();
        
        // Then
        assertEquals(10, scores.size(), "최대 10개 점수만 유지해야 합니다");
        assertEquals(1100, scores.get(0).getScore(), "가장 높은 점수는 1100이어야 합니다");
        assertEquals(200, scores.get(9).getScore(), "10번째 점수는 200이어야 합니다");
    }
    
    @Test
    @DisplayName("하이스코어 판별 테스트 - 점수가 10개 미만일 때")
    void isHighScoreWhenLessThan10Scores() {
        // Given
        scoreManager.addScore("Player1", 500);
        scoreManager.addScore("Player2", 300);
        
        // When & Then
        assertTrue(scoreManager.isHighScore(100), "점수가 10개 미만이면 모든 점수가 하이스코어여야 합니다");
        assertTrue(scoreManager.isHighScore(1000), "높은 점수는 당연히 하이스코어여야 합니다");
    }
    
    @Test
    @DisplayName("하이스코어 판별 테스트 - 점수가 10개일 때")
    void isHighScoreWhenExactly10Scores() {
        // Given - 10개 점수 추가 (100, 200, ..., 1000)
        for (int i = 1; i <= 10; i++) {
            scoreManager.addScore("Player" + i, i * 100);
        }
        
        // When & Then
        assertFalse(scoreManager.isHighScore(50), "최저 점수(100)보다 낮으면 하이스코어가 아니어야 합니다");
        assertFalse(scoreManager.isHighScore(100), "최저 점수와 같으면 하이스코어가 아니어야 합니다");
        assertTrue(scoreManager.isHighScore(150), "최저 점수보다 높으면 하이스코어여야 합니다");
        assertTrue(scoreManager.isHighScore(2000), "높은 점수는 당연히 하이스코어여야 합니다");
    }
    
    @Test
    @DisplayName("ScoreEntry 생성 및 비교 테스트")
    void scoreEntryCreationAndComparison() {
        // Given
        ScoreManager.ScoreEntry entry1 = new ScoreManager.ScoreEntry("Alice", 1000);
        ScoreManager.ScoreEntry entry2 = new ScoreManager.ScoreEntry("Bob", 2000);
        
        // When & Then
        assertEquals("Alice", entry1.getPlayerName());
        assertEquals(1000, entry1.getScore());
        assertNotNull(entry1.getDate());
        assertNotNull(entry1.getFormattedDate());
        
        // 점수 비교 (높은 점수가 먼저)
        assertTrue(entry1.compareTo(entry2) > 0, "낮은 점수는 높은 점수보다 뒤에 와야 합니다");
        assertTrue(entry2.compareTo(entry1) < 0, "높은 점수는 낮은 점수보다 앞에 와야 합니다");
    }
    
    @Test
    @DisplayName("ScoreEntry 문자열 변환 테스트")
    void scoreEntryStringConversion() {
        // Given
        Date testDate = new Date();
        ScoreManager.ScoreEntry entry = new ScoreManager.ScoreEntry("TestPlayer", 1500, testDate);
        
        // When
        String entryString = entry.toString();
        ScoreManager.ScoreEntry reconstructed = ScoreManager.ScoreEntry.fromString(entryString);
        
        // Then
        assertNotNull(reconstructed, "문자열에서 ScoreEntry 복원이 가능해야 합니다");
        assertEquals("TestPlayer", reconstructed.getPlayerName());
        assertEquals(1500, reconstructed.getScore());
        assertEquals(testDate.getTime(), reconstructed.getDate().getTime());
    }
    
    @Test
    @DisplayName("잘못된 문자열에서 ScoreEntry 생성 실패 테스트")
    void scoreEntryFromInvalidString() {
        // When & Then
        assertNull(ScoreManager.ScoreEntry.fromString("invalid"), "잘못된 문자열은 null을 반환해야 합니다");
        assertNull(ScoreManager.ScoreEntry.fromString("name,invalid_score"), "잘못된 점수 형식은 null을 반환해야 합니다");
        assertNull(ScoreManager.ScoreEntry.fromString(""), "빈 문자열은 null을 반환해야 합니다");
    }
    
    @Test
    @DisplayName("날짜 포맷팅 테스트")
    void dateFormattingTest() {
        // Given
        ScoreManager.ScoreEntry entry = new ScoreManager.ScoreEntry("Player", 1000);
        
        // When
        String formattedDate = entry.getFormattedDate();
        
        // Then
        assertNotNull(formattedDate);
        assertTrue(formattedDate.matches("\\d{2}.\\d{2}.\\d{2}"), "날짜는 yy.MM.dd 형식이어야 합니다");
    }
    
    @Test
    @DisplayName("점수 목록 불변성 테스트")
    void highScoresListImmutability() {
        // Given
        scoreManager.addScore("Player1", 1000);
        scoreManager.addScore("Player2", 2000);
        
        // When
        List<ScoreManager.ScoreEntry> scores1 = scoreManager.getHighScores();
        List<ScoreManager.ScoreEntry> scores2 = scoreManager.getHighScores();
        
        // Then
        assertNotSame(scores1, scores2, "매번 새로운 리스트 인스턴스를 반환해야 합니다");
        assertEquals(scores1.size(), scores2.size(), "내용은 같아야 합니다");
        
        // 원본 리스트 수정 시도
        scores1.clear();
        assertEquals(2, scoreManager.getHighScores().size(), "외부에서 리스트를 수정해도 원본에 영향이 없어야 합니다");
    }
}