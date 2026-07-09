package ru.z3r0ing.gitlabnotificator.validation.scheduling;

import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * TaskScheduler stub: records scheduled tasks so tests fire them manually.
 */
class ManualTaskScheduler implements TaskScheduler {

    final List<ManualTask> tasks = new ArrayList<>();

    static final class ManualTask {
        final Runnable runnable;
        final Instant startTime;
        final ManualScheduledFuture future = new ManualScheduledFuture();
        private boolean executed;

        ManualTask(Runnable runnable, Instant startTime) {
            this.runnable = runnable;
            this.startTime = startTime;
        }

        /** Fires the task unless its future was cancelled. */
        void run() {
            if (!future.isCancelled()) {
                executed = true;
                runnable.run();
            }
        }

        boolean isCancelled() {
            return future.isCancelled();
        }

        /** Pending = neither cancelled nor already fired. */
        boolean isPending() {
            return !future.isCancelled() && !executed;
        }
    }

    static final class ManualScheduledFuture implements ScheduledFuture<Object> {
        private boolean cancelled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Object get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(long timeout, @NonNull TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDelay(@NonNull TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(@NonNull Delayed other) {
            return 0;
        }
    }

    List<ManualTask> activeTasks() {
        return tasks.stream().filter(ManualTask::isPending).toList();
    }

    ManualTask lastTask() {
        return tasks.get(tasks.size() - 1);
    }

    @Override
    @NonNull
    public ScheduledFuture<?> schedule(@NonNull Runnable task, @NonNull Instant startTime) {
        ManualTask manualTask = new ManualTask(task, startTime);
        tasks.add(manualTask);
        return manualTask.future;
    }

    @Override
    public ScheduledFuture<?> schedule(@NonNull Runnable task, @NonNull Trigger trigger) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable task, @NonNull Instant startTime,
                                                  @NonNull Duration period) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable task, @NonNull Duration period) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable task, @NonNull Instant startTime,
                                                     @NonNull Duration delay) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable task, @NonNull Duration delay) {
        throw new UnsupportedOperationException();
    }
}
