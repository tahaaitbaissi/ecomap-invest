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

/** Pool for blocking SOAP calls wrapped in {@link java.util.concurrent.CompletableFuture}. */
@Configuration
public class FootTrafficSoapExecutorConfiguration {

    public static final String FOOT_TRAFFIC_SOAP_EXECUTOR = "footTrafficSoapExecutor";

    @Bean(name = FOOT_TRAFFIC_SOAP_EXECUTOR)
    public Executor footTrafficSoapExecutor(
            @Value("${app.foot-traffic.soap.pool-size:${APP_FOOT_TRAFFIC_SOAP_POOL:24}}") int poolSize) {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory factory =
                r -> {
                    Thread t = new Thread(r, "foot-traffic-soap-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                };
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2048),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
