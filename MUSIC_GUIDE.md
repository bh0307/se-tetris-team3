# 테트리스 배경음악 추가 가이드

## 🎵 음악 파일 준비

다음 위치에 음악 파일(WAV 형식)을 넣어주세요:

```
app/src/main/resources/audio/
├── menu_theme.wav     (메인 메뉴 BGM)
├── game_theme.wav     (게임 플레이 BGM - 테트리스 클래식 음악)
└── battle_theme.wav   (대전 모드 BGM)
```

## 📁 디렉토리 생성

1. `app/src/main/resources/` 폴더에 `audio` 폴더 생성
2. 위 음악 파일들을 해당 폴더에 복사

## 🎼 추천 음악

**클래식 테트리스 음악 (Korobeiniki)**
- 게임: `game_theme.wav`, `battle_theme.wav`
- 유튜브에서 "Tetris Theme A (Korobeiniki)" 검색
- 무료 음원 사이트에서 다운로드 가능

**메뉴 음악**
- 메뉴: `menu_theme.wav`
- 차분한 8-bit 음악 또는 테트리스 메뉴 음악 사용

## 🔧 음악 변환 (MP3 → WAV)

음악 파일이 MP3 형식이라면 WAV로 변환이 필요합니다:

### 온라인 변환 도구
- https://online-audio-converter.com/
- https://convertio.co/kr/mp3-wav/

### 설정
- 출력 형식: WAV
- 샘플 레이트: 44100 Hz (권장)
- 비트레이트: 16 bit
- 채널: Stereo

## 🎮 게임 내 음악 기능

### 음소거 토글
- **M 키**: 음소거 ON/OFF
- 메인 메뉴에서 언제든지 사용 가능

### 자동 재생
- 메인 메뉴 진입 시: `menu_theme.wav` 자동 재생
- 게임 시작 시: `game_theme.wav` 자동 재생 (무한 반복)
- 대전 모드 시작 시: `battle_theme.wav` 자동 재생

## ⚠️ 주의사항

1. **파일 형식**: 반드시 WAV 형식이어야 합니다
2. **파일 크기**: 너무 큰 파일은 로딩이 느릴 수 있습니다 (5MB 이하 권장)
3. **파일명**: 위에 명시된 이름과 정확히 일치해야 합니다
4. **없어도 실행**: 음악 파일이 없어도 게임은 정상 작동합니다

## 🎵 무료 음원 사이트

- **Freesound**: https://freesound.org/
- **Free Music Archive**: https://freemusicarchive.org/
- **Incompetech**: https://incompetech.com/
- **YouTube Audio Library**: 무료 음원 제공

## 🔊 볼륨 조절 (코드 수정)

볼륨을 조절하려면 `AudioManager.java`에서:

```java
AudioManager.getInstance().setVolume(0.5f); // 0.0 ~ 1.0
```

기본값은 0.5 (50%)입니다.
