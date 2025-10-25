package se.tetris.team3.ui.score;

import java.io.*;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;

import se.tetris.team3.core.GameMode;

// 모드별 파일 분리: classic(.txt), item(.txt.item)
public class ScoreManager {

    private final String scoreFileBase;
    private static final int MAX_HIGH_SCORES = 10;

    // 기본 생성자
    public ScoreManager() {
        this(System.getProperty("user.dir")
                + File.separator + "app"
                + File.separator + "build"
                + File.separator + "scores"
                + File.separator + "scoreFile.txt");
    }
    
    // 테스트용 경로 지정 생성자
    public ScoreManager(String fileBasePath) {
        this.scoreFileBase = fileBasePath;
    }

    // 데이터 클래스: 한 개의 점수 기록을 담는 DTO
    public static class ScoreEntry implements Comparable<ScoreEntry> {
        private String playerName;
        private int score;
        private Date date;

        public ScoreEntry(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
            this.date = new Date();
        }

        public ScoreEntry(String playerName, int score, Date date) {
            this.playerName = playerName;
            this.score = score;
            this.date = date;
        }

        public String getPlayerName() { return playerName; }
        public int getScore() { return score; }
        public Date getDate() { return date; }

        @Override
        public int compareTo(ScoreEntry other) {
            return Integer.compare(other.score, this.score);
        }

        @Override
        public String toString() {
            return playerName + "," + score + "," + date.getTime();
        }

        public static ScoreEntry fromString(String str) {
            String[] parts = str.split(",");
            if (parts.length >= 2) {
                try {
                    String name = parts[0];
                    int score = Integer.parseInt(parts[1]);
                    Date date = parts.length > 2 ? new Date(Long.parseLong(parts[2])) : new Date();
                    return new ScoreEntry(name, score, date);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(date);
        }
    }

    // 파일 경로 유틸: 점수 파일 경로를 모드별로 분리해서 만들어주는 헬퍼
    // classic(.txt), item(.txt.item)
    private String filePathFor(GameMode mode) {
        return (mode == GameMode.ITEM)
                ? (scoreFileBase + ".item")
                : scoreFileBase;
    }

    // 모드 인식 API
    public void addScore(GameMode mode, String playerName, int score) {
        // 모드별 점수 목록을 독립적으로 관리하기 위해 매번 로드
        List<ScoreEntry> list = loadForMode(mode);
        list.add(new ScoreEntry(playerName, score));
        Collections.sort(list);
        if (list.size() > MAX_HIGH_SCORES) {
            list = new ArrayList<>(list.subList(0, MAX_HIGH_SCORES));
        }
        saveForMode(mode, list);
    }

    public boolean isHighScore(GameMode mode, int score) {
        List<ScoreEntry> list = loadForMode(mode);
        if (list.size() < MAX_HIGH_SCORES) return true;
        // 동점은 하이스코어 아님
        return score > list.get(MAX_HIGH_SCORES - 1).getScore();
    }

    public List<ScoreEntry> getHighScores(GameMode mode) {
    return new ArrayList<>(loadForMode(mode));
}

    // 파일 IO: 점수 리스트를 디스크 파일과 동기화
    // 설정에서 초기화 하기 전에는 프로그램을 종료하더라도 스코어 보드 기록 유지
    private List<ScoreEntry> loadForMode(GameMode mode) {
        List<ScoreEntry> list = new ArrayList<>();
        String path = filePathFor(mode);
        File f = new File(path);

        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        if (!f.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ScoreEntry entry = ScoreEntry.fromString(line);
                if (entry != null) list.add(entry);
            }
            Collections.sort(list);
        } catch (IOException e) {
            // ignore
        }
        return list;
    }

    private void saveForMode(GameMode mode, List<ScoreEntry> list) {
        String path = filePathFor(mode);
        File f = new File(path);

        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(f, false))) {
            for (ScoreEntry entry : list) writer.println(entry.toString());
        } catch (IOException e) {
            System.err.println("Cannot save scores(" + mode + "): " + e.getMessage());
        }
    }

    // Settings에서 호출될 스코어 초기화(모드 구분 없이 전부 삭제)
    public void clearScores() {
        File base = new File(scoreFileBase);
        File dir = base.getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();

        try (PrintWriter w = new PrintWriter(new FileWriter(scoreFileBase, false))) {}
        catch (IOException e) { System.err.println("Cannot clear scores: " + e.getMessage()); }
        
        try (PrintWriter w = new PrintWriter(new FileWriter(scoreFileBase + ".item", false))) {}
        catch (IOException e) { System.err.println("Cannot clear item scores: " + e.getMessage()); }
        
        System.out.println("[ScoreManager] All scores cleared (classic + item).");
    }

        // ===== 구 시그니처 호환 =====
    public boolean isHighScore(int score) {
        return isHighScore(GameMode.CLASSIC, score);
    }

    public void addScore(String playerName, int score) {
        addScore(GameMode.CLASSIC, playerName, score);
    }

    public List<ScoreEntry> getHighScores() {
        return new ArrayList<>(loadForMode(GameMode.CLASSIC));
    }
}
