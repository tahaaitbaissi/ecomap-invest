package com.example.backend.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class OsmImportSchedulerTest {

    @Test
    void runNightlyImport_runsJob() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        when(launcher.run(eq(job), any(JobParameters.class)))
                .thenReturn(mock(JobExecution.class));
        var scheduler = new OsmImportScheduler(launcher, job);
        scheduler.runNightlyImport();
        verify(launcher).run(eq(job), any(JobParameters.class));
    }
}
