package se.tetris.team3.audio;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * 게임 배경 음악 관리 클래스
 * - MIDI를 사용하여 테트리스 스타일의 배경음악 생성 및 재생
 */
public class AudioManager {
    private Sequencer sequencer;
    private Sequence sequence;
    private boolean isPlaying = false;
    private int currentTrack = 0; // 0: 메뉴, 1: 게임, 2: 배틀
    
    public AudioManager() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
        } catch (MidiUnavailableException e) {
            System.err.println("MIDI 시스템을 사용할 수 없습니다: " + e.getMessage());
        }
    }
    
    /**
     * 메뉴 화면 음악 재생 (밝고 경쾌한 느낌)
     */
    public void playMenuMusic() {
        if (currentTrack == 0 && isPlaying) return;
        stopMusic();
        currentTrack = 0;
        createMenuMusic();
        startPlayback();
    }
    
    /**
     * 게임 플레이 음악 재생 (집중할 수 있는 리듬감)
     */
    public void playGameMusic() {
        if (currentTrack == 1 && isPlaying) return;
        stopMusic();
        currentTrack = 1;
        createGameMusic();
        startPlayback();
    }
    
    /**
     * 배틀 모드 음악 재생 (긴장감 있는 빠른 템포)
     */
    public void playBattleMusic() {
        if (currentTrack == 2 && isPlaying) return;
        stopMusic();
        currentTrack = 2;
        createBattleMusic();
        startPlayback();
    }
    
    /**
     * 음악 정지
     */
    public void stopMusic() {
        if (sequencer != null && sequencer.isRunning()) {
            sequencer.stop();
            isPlaying = false;
        }
    }
    
    /**
     * 볼륨 조절 (0.0 ~ 1.0)
     */
    public void setVolume(float volume) {
        if (sequencer != null && sequencer.isOpen()) {
            try {
                // MIDI 볼륨은 0-127 범위
                int midiVolume = (int) (volume * 127);
                ShortMessage volumeMessage = new ShortMessage();
                volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, midiVolume);
                sequencer.getReceiver().send(volumeMessage, -1);
            } catch (Exception e) {
                System.err.println("볼륨 조절 실패: " + e.getMessage());
            }
        }
    }
    
    /**
     * 리소스 정리
     */
    public void dispose() {
        stopMusic();
        if (sequencer != null && sequencer.isOpen()) {
            sequencer.close();
        }
    }
    
    private void startPlayback() {
        if (sequencer != null && sequence != null) {
            try {
                sequencer.setSequence(sequence);
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                sequencer.start();
                isPlaying = true;
            } catch (InvalidMidiDataException e) {
                System.err.println("MIDI 재생 실패: " + e.getMessage());
            }
        }
    }
    
    /**
     * 메뉴 음악 생성 (Korobeiniki 테트리스 테마 스타일)
     */
    private void createMenuMusic() {
        try {
            sequence = new Sequence(Sequence.PPQ, 4);
            Track track = sequence.createTrack();
            
            // 악기 설정 (피아노)
            setInstrument(track, 0, 0);
            
            int tempo = 140; // BPM
            int beat = 60000000 / tempo;
            
            // 테트리스 테마 멜로디 (간소화 버전)
            int[][] melody = {
                {64, 4}, {59, 2}, {60, 2}, {62, 4}, {60, 2}, {59, 2},
                {57, 4}, {57, 2}, {60, 2}, {64, 4}, {62, 2}, {60, 2},
                {59, 6}, {60, 2}, {62, 4}, {64, 4},
                {60, 4}, {57, 4}, {57, 4}, {0, 4},
                
                {62, 4}, {65, 2}, {69, 4}, {67, 2}, {65, 2},
                {64, 6}, {60, 2}, {64, 4}, {62, 2}, {60, 2},
                {59, 4}, {59, 2}, {60, 2}, {62, 4}, {64, 4},
                {60, 4}, {57, 4}, {57, 4}, {0, 4}
            };
            
            int tick = 0;
            for (int[] note : melody) {
                if (note[0] > 0) {
                    addNote(track, 0, note[0], 80, tick, note[1]);
                }
                tick += note[1];
            }
            
            // 베이스 라인
            addSimpleBass(track, 1, new int[]{45, 40, 43, 38}, 32);
            
        } catch (InvalidMidiDataException e) {
            System.err.println("메뉴 음악 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 게임 음악 생성 (집중력을 위한 차분한 리듬)
     */
    private void createGameMusic() {
        try {
            sequence = new Sequence(Sequence.PPQ, 4);
            Track track = sequence.createTrack();
            
            setInstrument(track, 0, 0); // 피아노
            
            // 간단한 아르페지오 패턴
            int[][] pattern = {
                {60, 2}, {64, 2}, {67, 2}, {64, 2},
                {62, 2}, {65, 2}, {69, 2}, {65, 2},
                {59, 2}, {62, 2}, {67, 2}, {62, 2},
                {57, 2}, {60, 2}, {64, 2}, {60, 2}
            };
            
            int tick = 0;
            for (int i = 0; i < 4; i++) { // 4번 반복
                for (int[] note : pattern) {
                    addNote(track, 0, note[0], 70, tick, note[1]);
                    tick += note[1];
                }
            }
            
            addSimpleBass(track, 1, new int[]{48, 50, 47, 45}, 16);
            
        } catch (InvalidMidiDataException e) {
            System.err.println("게임 음악 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 배틀 음악 생성 (긴장감 있는 빠른 템포)
     */
    private void createBattleMusic() {
        try {
            sequence = new Sequence(Sequence.PPQ, 4);
            Track track = sequence.createTrack();
            
            setInstrument(track, 0, 0);
            
            // 긴박한 멜로디
            int[][] battle = {
                {69, 2}, {67, 2}, {65, 2}, {64, 2},
                {69, 2}, {67, 2}, {65, 2}, {62, 2},
                {72, 2}, {71, 2}, {69, 2}, {67, 2},
                {65, 2}, {64, 2}, {62, 2}, {60, 2},
                
                {67, 2}, {65, 2}, {64, 2}, {62, 2},
                {67, 2}, {65, 2}, {64, 2}, {60, 2},
                {69, 2}, {67, 2}, {65, 2}, {64, 2},
                {62, 2}, {60, 2}, {59, 2}, {57, 2}
            };
            
            int tick = 0;
            for (int[] note : battle) {
                addNote(track, 0, note[0], 90, tick, note[1]);
                tick += note[1];
            }
            
            // 드라마틱한 베이스
            addSimpleBass(track, 1, new int[]{45, 43, 48, 41}, 16);
            
        } catch (InvalidMidiDataException e) {
            System.err.println("배틀 음악 생성 실패: " + e.getMessage());
        }
    }
    
    private void setInstrument(Track track, int channel, int instrument) throws InvalidMidiDataException {
        ShortMessage prog = new ShortMessage();
        prog.setMessage(ShortMessage.PROGRAM_CHANGE, channel, instrument, 0);
        track.add(new MidiEvent(prog, 0));
    }
    
    private void addNote(Track track, int channel, int note, int velocity, int tick, int duration) throws InvalidMidiDataException {
        // Note ON
        ShortMessage on = new ShortMessage();
        on.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
        track.add(new MidiEvent(on, tick));
        
        // Note OFF
        ShortMessage off = new ShortMessage();
        off.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
        track.add(new MidiEvent(off, tick + duration));
    }
    
    private void addSimpleBass(Track track, int channel, int[] notes, int totalBeats) throws InvalidMidiDataException {
        setInstrument(track, channel, 32); // 베이스
        
        int beatsPer = totalBeats / notes.length;
        int tick = 0;
        
        for (int i = 0; i < 4; i++) { // 4번 반복
            for (int note : notes) {
                addNote(track, channel, note, 60, tick, beatsPer);
                tick += beatsPer;
            }
        }
    }
}
