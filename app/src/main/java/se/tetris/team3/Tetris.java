package se.tetris.team3;

import javax.swing.SwingUtilities;
import se.tetris.team3.ui.AppFrame;

public class Tetris {

    public static void main(String[] args) {
        check();
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }

     public static void check() {
        // Java version check
        String version = System.getProperty("java.version");
        boolean javaOk = version.startsWith("1.8") || version.compareTo("1.8") > 0;
        if (!javaOk) throw new RuntimeException("Java 1.8+ required");

        // OS check
        String os = System.getProperty("os.name").toLowerCase();
        String osVersion = System.getProperty("os.version");
        boolean osOk = false;

        if (os.contains("windows")) {
            // Windows 10 이상 체크
            String[] winVer = os.replaceAll("[^0-9.]", "").split("\\.");
            int winMajor = winVer.length > 0 && !winVer[0].isEmpty() ? Integer.parseInt(winVer[0]) : 0;
            osOk = winMajor >= 10;
        }
        if (!osOk) throw new RuntimeException("Windows 10 or higher required");

        // Memory check
        long maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        if (maxMemoryMB < 1024) throw new RuntimeException("At least 1GB RAM required");

        // Disk space check
        java.io.File root = new java.io.File(System.getProperty("user.dir"));
        long usableMB = root.getUsableSpace() / (1024 * 1024);
        if (usableMB < 500) throw new RuntimeException("At least 500MB disk space required");

        // CPU check (Windows only, clock speed)
        double cpuGHz = -1;
        try {
            Process process = Runtime.getRuntime().exec(
                new String[] { "powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty MaxClockSpeed" }
            );
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("\\d+")) {
                    int mhz = Integer.parseInt(line);
                    cpuGHz = mhz / 1000.0;
                    System.out.println(cpuGHz);
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            cpuGHz = -1;
        }
        if (cpuGHz < 1.2) throw new RuntimeException("At least 1.2GHz CPU required");
    }
}
