package ru.z3r0ing.gitlabnotificator.validation.scheduling;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class IssueValidationStateStoreTest {

    private final IssueValidationStateStore store = new IssueValidationStateStore();
    private final IssueKey key = new IssueKey(1L, 42L);

    @Test
    void mutate_NoExistingState_ShouldCreateOne() {
        IssueValidationState created = store.mutate(key, existing -> {
            assertThat(existing).isNull();
            return new IssueValidationState();
        });

        assertThat(created).isNotNull();
        assertThat(store.find(key)).isPresent();
    }

    @Test
    void mutate_ReturningNull_ShouldRemoveState() {
        store.mutate(key, existing -> new IssueValidationState());

        store.mutate(key, existing -> null);

        assertThat(store.find(key)).isEmpty();
    }

    @Test
    void find_UnknownKey_ShouldReturnEmpty() {
        assertThat(store.find(new IssueKey(9L, 9L))).isEmpty();
    }

    @Test
    void remove_ShouldDeleteState() {
        store.mutate(key, existing -> new IssueValidationState());

        store.remove(key);

        assertThat(store.find(key)).isEmpty();
    }

    @Test
    void mutate_ConcurrentCallsOnSameKey_ShouldBeAtomic() throws InterruptedException {
        int threads = 4;
        int iterationsPerThread = 250;
        // deliberately not atomic: lost updates here would expose a non-atomic mutate()
        int[] plainCounter = {0};
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                startLatch.await();
                for (int j = 0; j < iterationsPerThread; j++) {
                    store.mutate(key, existing -> {
                        plainCounter[0]++;
                        return existing != null ? existing : new IssueValidationState();
                    });
                }
                return null;
            });
        }
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(plainCounter[0]).isEqualTo(threads * iterationsPerThread);
        assertThat(store.find(key)).isPresent();
    }
}
