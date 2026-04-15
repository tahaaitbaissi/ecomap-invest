package com.example.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Isolated pool for blocking RMI work wrapped in {@link java.util.concurrent.CompletableFuture}, so hung scoring
 * nodes cannot exhaust the JVM common ForkJoinPool.
 */
@Configuration
public class RmiScoringExecutorConfiguration {

    public static final String RMI_SCORING_EXECUTOR = "rmiScoringExecutor";

    @Bean(name = RMI_SCORING_EXECUTOR)
    public Executor rmiScoringExecutor(@Value("${app.rmi.scoring.pipeline-pool-size:32}") int poolSize) {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "rmi-scoring-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(4096),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
