package se.tetris.team3.ui.score;

import java.io.*;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;

public class ScoreManager {
    
    private static final String SCORE_FILE = "app\\src\\main\\java\\se\\tetris\\team3\\ui\\score\\scoreFile.txt";
    private static final int MAX_HIGH_SCORES = 10;
    
    private List<ScoreEntry> highScores;
    
    public ScoreManager() {
        highScores = new ArrayList<>();
        loadHighScores();
    }
    
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
    
    public void addScore(String playerName, int score) {
        ScoreEntry entry = new ScoreEntry(playerName, score);
        highScores.add(entry);
        Collections.sort(highScores);
        
        if (highScores.size() > MAX_HIGH_SCORES) {
            highScores = highScores.subList(0, MAX_HIGH_SCORES);
        }
        
        saveHighScores();
    }
    
    public boolean isHighScore(int score) {
        if (highScores.size() < MAX_HIGH_SCORES) {
            return true;
        }
        return score > highScores.get(MAX_HIGH_SCORES - 1).getScore();
    }
    
    public List<ScoreEntry> getHighScores() {
        return new ArrayList<>(highScores);
    }
    
    private void loadHighScores() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SCORE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ScoreEntry entry = ScoreEntry.fromString(line);
                if (entry != null) {
                    highScores.add(entry);
                }
            }
            Collections.sort(highScores);
        } catch (IOException e) {
            highScores = new ArrayList<>();
        }
    }
    
    private void saveHighScores() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SCORE_FILE))) {
            for (ScoreEntry entry : highScores) {
                writer.println(entry.toString());
            }
        } catch (IOException e) {
            System.err.println("Cannot save scores: " + e.getMessage());
        }
    }
}