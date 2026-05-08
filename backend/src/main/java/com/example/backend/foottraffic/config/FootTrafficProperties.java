package com.example.backend.foottraffic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.foot-traffic")
public class FootTrafficProperties {

    private boolean enabled = true;
    private double termWeight = 0.25;
    private double blendAlpha = 0.5;
    private double trafficCap = 500;
    private double densityCapFallback = 50;
    private long jitterSalt = 893451263L;
    private Cache cache = new Cache();
    private Recompute recompute = new Recompute();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getTermWeight() {
        return termWeight;
    }

    public void setTermWeight(double termWeight) {
        this.termWeight = termWeight;
    }

    public double getBlendAlpha() {
        return blendAlpha;
    }

    public void setBlendAlpha(double blendAlpha) {
        this.blendAlpha = blendAlpha;
    }

    public double getTrafficCap() {
        return trafficCap;
    }

    public void setTrafficCap(double trafficCap) {
        this.trafficCap = trafficCap;
    }

    public double getDensityCapFallback() {
        return densityCapFallback;
    }

    public void setDensityCapFallback(double densityCapFallback) {
        this.densityCapFallback = densityCapFallback;
    }

    public long getJitterSalt() {
        return jitterSalt;
    }

    public void setJitterSalt(long jitterSalt) {
        this.jitterSalt = jitterSalt;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Recompute getRecompute() {
        return recompute;
    }

    public void setRecompute(Recompute recompute) {
        this.recompute = recompute;
    }

    public static class Cache {
        private int ttlSeconds = 172800;

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Recompute {
        private String scheduledCron = "0 0 3 * * *";
        private int batchSize = 1000;

        public String getScheduledCron() {
            return scheduledCron;
        }

        public void setScheduledCron(String scheduledCron) {
            this.scheduledCron = scheduledCron;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
