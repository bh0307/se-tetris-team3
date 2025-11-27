package se.tetris.team3.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * 게임 오디오 관리 클래스
 * - 배경음악(BGM) 재생
 * - 효과음(SFX) 재생
 */
public class AudioManager {
    
    private static AudioManager instance;
    private Clip bgmClip;
    private boolean isMuted = false;
    private float volume = 0.5f; // 0.0 ~ 1.0
    
    private AudioManager() {
        // 싱글톤
    }
    
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }
    
    /**
     * 배경음악 재생 (무한 반복)
     * @param resourcePath 리소스 경로 (예: "/audio/tetris_theme.wav")
     */
    public void playBGM(String resourcePath) {
        stopBGM(); // 기존 BGM 정지
        
        if (isMuted) return;
        
        try {
            InputStream audioSrc = getClass().getResourceAsStream(resourcePath);
            if (audioSrc == null) {
                System.err.println("Audio file not found: " + resourcePath);
                return;
            }
            
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            
            // 볼륨 설정
            setVolume(volume);
            
            // 무한 반복
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing BGM: " + e.getMessage());
        }
    }
    
    /**
     * 배경음악 정지
     */
    public void stopBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }
    
    /**
     * 배경음악 일시정지
     */
    public void pauseBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
        }
    }
    
    /**
     * 배경음악 재개
     */
    public void resumeBGM() {
        if (bgmClip != null && !bgmClip.isRunning() && !isMuted) {
            bgmClip.start();
        }
    }
    
    /**
     * 효과음 재생 (한 번만)
     * @param resourcePath 리소스 경로
     */
    public void playSFX(String resourcePath) {
        if (isMuted) return;
        
        new Thread(() -> {
            try {
                InputStream audioSrc = getClass().getResourceAsStream(resourcePath);
                if (audioSrc == null) return;
                
                InputStream bufferedIn = new BufferedInputStream(audioSrc);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                
                // 볼륨 설정
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
                
                clip.start();
                
                // 재생이 끝나면 자원 해제
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                
            } catch (Exception e) {
                // 효과음 재생 실패는 조용히 무시
            }
        }).start();
    }
    
    /**
     * 음소거 토글
     */
    public void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            pauseBGM();
        } else {
            resumeBGM();
        }
    }
    
    /**
     * 음소거 설정
     */
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        if (muted) {
            pauseBGM();
        } else {
            resumeBGM();
        }
    }
    
    /**
     * 볼륨 설정 (0.0 ~ 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        
        if (bgmClip != null && bgmClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(this.volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }
    
    /**
     * 현재 볼륨 가져오기
     */
    public float getVolume() {
        return volume;
    }
    
    /**
     * 음소거 상태 확인
     */
    public boolean isMuted() {
        return isMuted;
    }
    
    /**
     * 모든 오디오 정리
     */
    public void cleanup() {
        stopBGM();
    }
}
