package se.tetris.team3.uiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.ui.GameManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 파티클 시스템의 멀티스레드 안정성 테스트
 */
public class ParticleThreadSafetyTest {

    private GameManager manager;

    @BeforeEach
    public void setUp() {
        manager = new GameManager(GameMode.CLASSIC);
    }

    /**
     * particles 리스트 접근
     */
    @SuppressWarnings("unchecked")
    private List<Object> getParticles() throws Exception {
        Field particlesField = GameManager.class.getDeclaredField("particles");
        particlesField.setAccessible(true);
        return (List<Object>) particlesField.get(manager);
    }

    /**
     * addBreakEffect 메서드 호출 (private 메서드)
     */
    private void addBreakEffect(int gridX, int gridY) throws Exception {
        Method addBreakEffectMethod = GameManager.class.getDeclaredMethod("addBreakEffect", int.class, int.class);
        addBreakEffectMethod.setAccessible(true);
        addBreakEffectMethod.invoke(manager, gridX, gridY);
    }

    /**
     * field 가져오기
     */
    private int[][] getField() throws Exception {
        Field fieldField = GameManager.class.getDeclaredField("field");
        fieldField.setAccessible(true);
        return (int[][]) fieldField.get(manager);
    }

    @Test
    @DisplayName("동시에 여러 스레드가 파티클을 추가해도 ConcurrentModificationException이 발생하지 않는다")
    public void testConcurrentParticleAddition() throws Exception {
        final int THREAD_COUNT = 10;
        final int ITERATIONS_PER_THREAD = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        // 여러 스레드에서 동시에 파티클 추가
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        addBreakEffect(threadId % 10, j % 20);
                    }
                } catch (Exception e) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(exceptionOccurred.get(), "파티클 추가 중 예외가 발생하지 않아야 함");
        
        List<Object> particles = getParticles();
        // 각 addBreakEffect() 호출은 8~12개의 파티클을 생성
        int totalCalls = THREAD_COUNT * ITERATIONS_PER_THREAD;
        int minExpected = totalCalls * 8;  // 500 × 8 = 4,000
        int maxExpected = totalCalls * 12; // 500 × 12 = 6,000
        
        assertTrue(particles.size() >= minExpected && particles.size() <= maxExpected, 
                   String.format("파티클 수가 예상 범위(%d ~ %d)에 있어야 함, 실제: %d", 
                                 minExpected, maxExpected, particles.size()));
    }

    @Test
    @DisplayName("updateParticles와 파티클 추가가 동시에 실행되어도 안전하다")
    public void testConcurrentUpdateAndAdd() throws Exception {
        final int ADD_THREAD_COUNT = 5;
        final int UPDATE_THREAD_COUNT = 3;
        final int ITERATIONS = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(ADD_THREAD_COUNT + UPDATE_THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(ADD_THREAD_COUNT + UPDATE_THREAD_COUNT);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        // 파티클 추가 스레드
        for (int i = 0; i < ADD_THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        addBreakEffect(j % 10, j % 20);
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        exceptionOccurred.set(true);
                        e.printStackTrace();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 파티클 업데이트 스레드
        for (int i = 0; i < UPDATE_THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        manager.updateParticles();
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        exceptionOccurred.set(true);
                        e.printStackTrace();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(exceptionOccurred.get(), "동시 접근 중 예외가 발생하지 않아야 함");
    }

    @Test
    @DisplayName("renderParticles와 updateParticles가 동시에 실행되어도 안전하다")
    public void testConcurrentRenderAndUpdate() throws Exception {
        // 먼저 파티클 생성
        for (int i = 0; i < 50; i++) {
            addBreakEffect(i % 10, i % 20);
        }

        final int RENDER_THREADS = 3;
        final int UPDATE_THREADS = 3;
        final int ITERATIONS = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(RENDER_THREADS + UPDATE_THREADS);
        CountDownLatch latch = new CountDownLatch(RENDER_THREADS + UPDATE_THREADS);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        // 렌더링 스레드 (실제 Graphics2D 없이 호출만 테스트)
        for (int i = 0; i < RENDER_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        // renderParticles는 Graphics2D가 필요하므로 particles 리스트만 순회
                        List<Object> particles = getParticles();
                        synchronized(particles) {
                            for (Object particle : particles) {
                                // 단순 순회만 수행
                                assertNotNull(particle);
                            }
                        }
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        exceptionOccurred.set(true);
                        e.printStackTrace();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 업데이트 스레드
        for (int i = 0; i < UPDATE_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        manager.updateParticles();
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        exceptionOccurred.set(true);
                        e.printStackTrace();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(exceptionOccurred.get(), "렌더링과 업데이트 동시 실행 중 예외가 발생하지 않아야 함");
    }

    @RepeatedTest(5)
    @DisplayName("clearLines와 updateParticles가 동시에 실행되어도 안전하다 (반복 테스트)")
    public void testClearLinesWithUpdateParticles() throws Exception {
        int[][] field = getField();
        
        // 여러 줄을 가득 채움
        for (int row = 15; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                field[row][col] = 1;
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        // clearLines 스레드
        executor.submit(() -> {
            try {
                manager.clearLines(true);
                Thread.sleep(200); // 플래시 완료까지 대기
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                }
            } finally {
                latch.countDown();
            }
        });

        // updateParticles 스레드
        executor.submit(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    manager.updateParticles();
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                }
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(exceptionOccurred.get(), "clearLines와 updateParticles 동시 실행 중 예외가 발생하지 않아야 함");
    }

    @Test
    @DisplayName("particles 리스트가 동기화된 리스트로 생성되었는지 확인")
    public void testParticlesIsSynchronized() throws Exception {
        List<Object> particles = getParticles();
        
        // Collections.synchronizedList로 감싸진 리스트인지 확인
        String className = particles.getClass().getName();
        assertTrue(className.contains("SynchronizedList") || className.contains("SynchronizedRandomAccessList"),
                   "particles는 동기화된 리스트여야 함");
    }

    @Test
    @DisplayName("대량의 파티클 생성과 업데이트가 안정적으로 동작한다")
    public void testHighVolumeParticleOperations() throws Exception {
        final int PARTICLE_COUNT = 1000;
        final int UPDATE_COUNT = 100;
        
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(2);

        // 대량 파티클 생성
        executor.submit(() -> {
            try {
                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    addBreakEffect(i % 10, i % 20);
                }
            } catch (Exception e) {
                exceptionOccurred.set(true);
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 동시에 업데이트
        executor.submit(() -> {
            try {
                for (int i = 0; i < UPDATE_COUNT; i++) {
                    manager.updateParticles();
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                }
            } finally {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(exceptionOccurred.get(), "대량 파티클 처리 중 예외가 발생하지 않아야 함");
        
        List<Object> particles = getParticles();
        assertTrue(particles.size() <= PARTICLE_COUNT, "파티클 수가 예상 범위 내에 있어야 함");
    }
}
