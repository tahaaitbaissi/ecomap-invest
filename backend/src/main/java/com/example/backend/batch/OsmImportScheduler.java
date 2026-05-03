package com.example.backend.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.batch", name = "enabled", havingValue = "true")
public class OsmImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(OsmImportScheduler.class);
    private final JobLauncher jobLauncher;
    private final Job osmImportJob;

    public OsmImportScheduler(JobLauncher jobLauncher, Job osmImportJob) {
        this.jobLauncher = jobLauncher;
        this.osmImportJob = osmImportJob;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runNightlyImport() throws Exception {
        log.info("Démarrage import OSM nocturne...");
        JobParameters params = new JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        jobLauncher.run(osmImportJob, params);
    }
}