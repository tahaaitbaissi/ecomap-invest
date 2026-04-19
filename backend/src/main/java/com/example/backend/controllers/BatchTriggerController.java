package com.example.backend.controllers;

import com.example.backend.batch.OsmImportScheduler;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/batch")
public class BatchTriggerController {

    private final JobLauncher jobLauncher;
    private final Job osmImportJob;

    public BatchTriggerController(JobLauncher jobLauncher, Job osmImportJob) {
        this.jobLauncher = jobLauncher;
        this.osmImportJob = osmImportJob;
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> trigger() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        JobExecution execution = jobLauncher.run(osmImportJob, params);
        return ResponseEntity.ok("Job lancé : " + execution.getJobInstance().getInstanceId());
    }
}