package se.tetris.team3.store;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.core.Settings.SizePreset;
import se.tetris.team3.ui.score.ScoreManager;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Settings를 프로그램 재실행 후에도 유지하려고 쓰는 영속 저장 레이어
 */
public class SettingsStore {

    private static final String DIR  = System.getProperty("user.home") + File.separator + ".tetris";
    private static final String FILE = DIR + File.separator + "settings.properties";

    // P2P 최근 IP 저장용 키
    private static final String KEY_RECENT_IPS = "p2p.recentIPs"; // "ip1,ip2,ip3" 형태

    // --------------------- Settings 저장 ---------------------
    // 설정 파일 저장 
    public static void save(Settings s) {
        try {
            Files.createDirectories(Paths.get(DIR));
            Properties p = new Properties();

            // 기존 파일에 있던 값(최근 IP 등)을 먼저 읽어서 유지
            File f = new File(FILE);
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    p.load(fis);
                } catch (IOException ignore) {}
            }

            // Settings 값 덮어쓰기
            p.setProperty("sizePreset", s.getSizePreset().name());
            p.setProperty("colorBlind", Boolean.toString(s.isColorBlindMode()));
            p.setProperty("gameMode", s.getGameMode().name());

            // 키맵 저장
            for (Map.Entry<Settings.Action, Integer> e : s.getKeymap().entrySet()) {
                p.setProperty("key." + e.getKey().name(), Integer.toString(e.getValue()));
            }

            // 저장
            try (FileOutputStream fos = new FileOutputStream(FILE)) {
                p.store(fos, "Tetris Settings");
            }
        } catch (IOException e) {
            System.err.println("[SettingsStore] save failed: " + e.getMessage());
        }
    }

    // 설정 파일 로드
    public static void load(Settings s) {
        File f = new File(FILE);
        if (!f.exists()) return;

        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(f)) {
            p.load(fis);

            s.setSizePreset(SizePreset.valueOf(
                    p.getProperty("sizePreset", s.getSizePreset().name())
            ));
            s.setColorBlindMode(Boolean.parseBoolean(
                    p.getProperty("colorBlind", Boolean.toString(s.isColorBlindMode()))
            ));

            String gm = p.getProperty("gameMode", GameMode.CLASSIC.name());
            try {
                s.setGameMode(GameMode.valueOf(gm));
            } catch (IllegalArgumentException ignore) {}

            // 키맵 복원
            for (Action a : Action.values()) {
                String v = p.getProperty("key." + a.name());
                if (v != null) {
                    try {
                        s.getKeymap().put(a, Integer.parseInt(v));
                    } catch (NumberFormatException ignore) {
                        System.err.println("[SettingsStore] invalid key value for " + a.name());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[SettingsStore] load failed: " + e.getMessage());
        }
    }

    // --------------------- P2P 최근 IP 관련 ---------------------

    /** settings.properties 에서 최근 P2P IP 리스트를 읽어서 돌려줌 */
    public static List<String> getRecentP2PIPs() {
        File f = new File(FILE);
        if (!f.exists()) return Collections.emptyList();

        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(f)) {
            p.load(fis);
            String v = p.getProperty(KEY_RECENT_IPS, "");
            List<String> list = new ArrayList<>();
            for (String s : v.split(",")) {
                s = s.trim();
                if (!s.isEmpty()) list.add(s);
            }
            return list;
        } catch (IOException e) {
            System.err.println("[SettingsStore] getRecentP2PIPs failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 최근 접속한 P2P IP 를 맨 앞에 추가하고 최대 5개까지만 유지 */
    public static void addRecentP2PIP(String ip) {
        if (ip == null || ip.isBlank()) return;
        ip = ip.trim();

        Properties p = new Properties();
        File f = new File(FILE);
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                p.load(fis);
            } catch (IOException ignore) {}
        }

        // 기존 값 파싱
        List<String> list = new ArrayList<>();
        String v = p.getProperty(KEY_RECENT_IPS, "");
        for (String s : v.split(",")) {
            s = s.trim();
            if (!s.isEmpty() && !s.equals(ip)) list.add(s);
        }

        // 새 IP 맨 앞에
        list.add(0, ip);
        while (list.size() > 5) list.remove(list.size() - 1);

        // 다시 문자열로 합치기
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        p.setProperty(KEY_RECENT_IPS, sb.toString());

        // 저장
        try {
            Files.createDirectories(Paths.get(DIR));
            try (FileOutputStream fos = new FileOutputStream(FILE)) {
                p.store(fos, "Tetris Settings");
            }
        } catch (IOException e) {
            System.err.println("[SettingsStore] addRecentP2PIP failed: " + e.getMessage());
        }
    }

    // --------------------- 스코어 초기화 ---------------------
    public static void resetScores() {
        try {
            ScoreManager manager = new ScoreManager();
            manager.clearScores();
            System.out.println("[SettingsStore] Scores cleared via ScoreManager.");
        } catch (Exception e) {
            System.err.println("[SettingsStore] resetScores failed: " + e.getMessage());
        }
    }
}
