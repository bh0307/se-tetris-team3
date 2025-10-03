package se.tetris.team3.store;

import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.core.Settings.SizePreset;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class SettingsStore {
    private static final String DIR  = System.getProperty("user.home") + File.separator + ".tetris";
    private static final String FILE = DIR + File.separator + "settings.properties";

    // 현재 설정값을 사용자 홈 폴더의 파일에 저장
    public static void save(Settings s) {
        try {
            Files.createDirectories(Paths.get(DIR));
            Properties p = new Properties();
            p.setProperty("sizePreset", s.getSizePreset().name());
            p.setProperty("colorBlind", Boolean.toString(s.isColorBlindMode()));
            for (var e : s.getKeymap().entrySet()) {
                p.setProperty("key." + e.getKey().name(), Integer.toString(e.getValue()));
            }
            try (FileOutputStream fos = new FileOutputStream(FILE)) {
                p.store(fos, "Tetris Settings");
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // 저장된 파일이 있으면 읽어서 Settings 인스턴스에 반영
    public static void load(Settings s) {
        File f = new File(FILE);
        if (!f.exists()) return;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(f)) {
            p.load(fis);
            s.setSizePreset(SizePreset.valueOf(p.getProperty("sizePreset", s.getSizePreset().name())));
            s.setColorBlindMode(Boolean.parseBoolean(p.getProperty("colorBlind", "false")));
            for (Action a : Action.values()) {
                String v = p.getProperty("key." + a.name());
                if (v != null) s.getKeymap().put(a, Integer.parseInt(v));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // PR5에서 사용, 스코어 저장 파일을 초기화
    public static void resetScores() {
        try {
            Files.createDirectories(Paths.get(DIR));
            Path scorePath = Paths.get(DIR, "scores.dat");
            Files.deleteIfExists(scorePath);
            Files.createFile(scorePath);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
