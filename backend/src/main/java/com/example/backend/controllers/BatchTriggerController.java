package com.example.backend.controllers;

import com.example.backend.controllers.dto.BatchJobStatusResponse;
import com.example.backend.controllers.dto.BatchTriggerResponse;
import java.util.Collection;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/batch")
public class BatchTriggerController {

    private final JobLauncher jobLauncher;
    private final Job osmImportJob;
    private final JobExplorer jobExplorer;

    public BatchTriggerController(
            JobLauncher jobLauncher, Job osmImportJob, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.osmImportJob = osmImportJob;
        this.jobExplorer = jobExplorer;
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchTriggerResponse> trigger() throws Exception {
        JobParameters params =
                new JobParametersBuilder().addLong("timestamp", System.currentTimeMillis()).toJobParameters();
        JobExecution execution = jobLauncher.run(osmImportJob, params);
        JobInstance inst = execution.getJobInstance();
        return ResponseEntity.ok(
                new BatchTriggerResponse(
                        inst.getId(),
                        execution.getId(),
                        execution.getStatus() != null ? execution.getStatus().name() : "UNKNOWN"));
    }

    @GetMapping("/status/{executionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchJobStatusResponse> status(@PathVariable long executionId) {
        JobExecution ex = jobExplorer.getJobExecution(executionId);
        if (ex == null) {
            return ResponseEntity.notFound().build();
        }
        long read = 0, write = 0, skip = 0;
        Collection<StepExecution> steps = ex.getStepExecutions();
        if (steps != null) {
            for (StepExecution se : steps) {
                read += se.getReadCount();
                write += se.getWriteCount();
                skip += se.getSkipCount();
            }
        }
        return ResponseEntity.ok(
                new BatchJobStatusResponse(
                        ex.getStatus() != null ? ex.getStatus().name() : "UNKNOWN",
                        ex.getStartTime(),
                        ex.getEndTime(),
                        read,
                        write,
                        skip));
    }
}