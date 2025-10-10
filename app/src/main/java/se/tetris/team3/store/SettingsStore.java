package se.tetris.team3.store;

import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.core.Settings.SizePreset;
import se.tetris.team3.ui.score.ScoreManager;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class SettingsStore {

    private static final String DIR  = System.getProperty("user.home") + File.separator + ".tetris";
    private static final String FILE = DIR + File.separator + "settings.properties";

    // 설정 파일 저장 
    public static void save(Settings s) {
        try {
            Files.createDirectories(Paths.get(DIR));
            Properties p = new Properties();

            p.setProperty("sizePreset", s.getSizePreset().name());
            p.setProperty("colorBlind", Boolean.toString(s.isColorBlindMode()));

            // 키맵(settings.properties에 저장된 키 설정 다시 불러오기) 저장
            for (var e : s.getKeymap().entrySet()) {
                p.setProperty("key." + e.getKey().name(), Integer.toString(e.getValue()));
            }

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

            // 기본값과 비교해서 존재하는 값만 덮어쓰기
            s.setSizePreset(SizePreset.valueOf(
                    p.getProperty("sizePreset", s.getSizePreset().name())
            ));
            s.setColorBlindMode(Boolean.parseBoolean(
                    p.getProperty("colorBlind", Boolean.toString(s.isColorBlindMode()))
            ));

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

    // ScoreManager API를 통해 스코어 초기화
    public static void resetScores() {
        try {
            ScoreManager manager = new ScoreManager();
            manager.clearScores();
            System.out.println("[SettingsStore] Scores cleared via ScoreManager.");
        } catch (Exception e) {
            System.err.println("[SettingsStore] resetScores failed: " + e.getMessage());
        }
    }

    /*

    어쩌다보니 GPT가 작성해줘서 일단은 놔둠

    public static void loadSettings(Settings settings) {
        // TODO: 파일에서 설정을 불러오는 로직 구현 예정
        // 지금은 비워둬도 됨

        // 예시 구현 (JSON 파싱 라이브러리 없이 간단히 구현)
        // 나중에 프로그램 꺼도 색맹모드 유지하려면 이 부분 구현 필요

        File file = new File(SETTINGS_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String json = reader.readLine();
            if (json != null && json.contains("\"colorBlindMode\":true")) {
                settings.setColorBlindMode(true);
            } else {
                settings.setColorBlindMode(false);
            }

        } catch (IOException e) {
            System.err.println("[SettingsStore] 설정 파일 로드 실패: " + e.getMessage());
        }
    }

    public static void saveSettings(Settings settings) {
        // TODO: 설정 저장 로직 구현 예정

        // 예시 구현 (JSON 파싱 라이브러리 없이 간단히 구현)
        // 나중에 프로그램 꺼도 색맹모드 유지하려면 이 부분 구현 필요

        File file = new File(SETTINGS_FILE);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            String json = String.format("{\"colorBlindMode\":%s}", settings.isColorBlindMode());
            writer.write(json);

        } catch (IOException e) {
            System.err.println("[SettingsStore] 설정 파일 저장 실패: " + e.getMessage());
        }
    }
    */

}
