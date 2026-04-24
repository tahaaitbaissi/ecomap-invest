package com.example.backend.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.entities.Poi;
import com.example.backend.overpass.OsmElement;
import com.example.backend.overpass.OverpassApiClient;
import com.example.backend.repositories.PoiRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Same step/job wiring as {@link BatchConfig}, in-memory job repository, mocked Overpass (no HTTP).
 * First reader pass is slow due to {@link OverpassItemReader} throttling between tags.
 */
@SuppressWarnings("removal")
class BatchConfigJobLauncherTest {

    @Test
    @Timeout(90)
    void osmImportJob_runsToCompletion() throws Exception {
        OverpassApiClient overpass = mock(OverpassApiClient.class);
        when(overpass.fetchByTagAndBBox(any(String.class), any()))
                .thenReturn(List.of());
        PoiRepository poiRepo = mock(PoiRepository.class);
        when(poiRepo.existsByOsmId(any())).thenReturn(false);

        var jobRepository = new ResourcelessJobRepository();
        var tx = new ResourcelessTransactionManager();
        var reader = new OverpassItemReader(overpass);
        var processor = new PoiItemProcessor(poiRepo);
        var writer = new PoiItemWriter(poiRepo);

        var step =
                new StepBuilder("importPoisStep", jobRepository)
                        .<OsmElement, Poi>chunk(100, tx)
                        .reader(reader)
                        .processor(processor)
                        .writer(writer)
                        .faultTolerant()
                        .skipLimit(500)
                        .skip(DataIntegrityViolationException.class)
                        .build();

        Job job = new JobBuilder("osmImportJob", jobRepository).start(step).build();

        var launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SyncTaskExecutor());
        launcher.afterPropertiesSet();

        var execution =
                launcher.run(
                        job,
                        new JobParametersBuilder()
                                .addLong("timestamp", System.currentTimeMillis(), true)
                                .toJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }
}
