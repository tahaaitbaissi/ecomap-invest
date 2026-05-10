package com.example.backend.foottraffic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.foot-traffic")
public class FootTrafficProperties {

    private boolean enabled = true;
    private double termWeight = 0.25;
    private double blendAlpha = 0.5;
    private double trafficCap = 120;
    /** If true, normalize with sqrt(peak/cap) so low-but-nonzero peaks contribute visibly in hex scoring. */
    private boolean sqrtNormalization = true;
    private double densityCapFallback = 50;
    private long jitterSalt = 893451263L;
    private Cache cache = new Cache();
    private Recompute recompute = new Recompute();
    private Soap soap = new Soap();

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

    public boolean isSqrtNormalization() {
        return sqrtNormalization;
    }

    public void setSqrtNormalization(boolean sqrtNormalization) {
        this.sqrtNormalization = sqrtNormalization;
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

    public Soap getSoap() {
        return soap;
    }

    public void setSoap(Soap soap) {
        this.soap = soap;
    }

    public static class Soap {
        private boolean enabled = true;
        private String url = "http://soap-foot-traffic:8092/ws";
        private int poolSize = 24;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }
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
        /**
         * If true, run a one-time recompute on app boot (useful for Docker demo so the UI isn't
         * "empty" until an admin clicks recompute).
         */
        private boolean runOnBoot = true;
        /** If true, only run boot recompute when {@code foot_traffic_cell_profile} is empty. */
        private boolean runOnBootOnlyIfEmpty = true;
        /** If true, enable the scheduled recompute job (cron). */
        private boolean scheduledEnabled = false;

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

        public boolean isRunOnBoot() {
            return runOnBoot;
        }

        public void setRunOnBoot(boolean runOnBoot) {
            this.runOnBoot = runOnBoot;
        }

        public boolean isRunOnBootOnlyIfEmpty() {
            return runOnBootOnlyIfEmpty;
        }

        public void setRunOnBootOnlyIfEmpty(boolean runOnBootOnlyIfEmpty) {
            this.runOnBootOnlyIfEmpty = runOnBootOnlyIfEmpty;
        }

        public boolean isScheduledEnabled() {
            return scheduledEnabled;
        }

        public void setScheduledEnabled(boolean scheduledEnabled) {
            this.scheduledEnabled = scheduledEnabled;
        }
    }
}
