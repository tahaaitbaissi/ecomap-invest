package com.example.backend.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.overpass.OverpassApiClient;
import com.example.backend.overpass.OsmElement;
import com.example.backend.testsupport.AbstractPostgisRedisIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * Launches {@link BatchConfig#osmImportJob} with Overpass results stubbed (no real HTTP).
 */
@Tag("integration")
@SpringBootTest
@TestPropertySource(
        properties = {
            "app.batch.enabled=true",
            "spring.batch.job.enabled=false"
        })
@Import(OsmBatchJobLauncherIT.StubOverpass.class)
class OsmBatchJobLauncherIT extends AbstractPostgisRedisIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job osmImportJob;

    static class StubOverpass {

        @Bean
        @Primary
        OverpassApiClient overpassApiClientStub() {
            OverpassApiClient m = mock(OverpassApiClient.class);
            when(m.fetchByTagAndBBox(any(), any())).thenReturn(List.<OsmElement>of());
            return m;
        }
    }

    @Test
    void osmImportJob_completesWithEmptyOverpass() throws Exception {
        JobExecution ex =
                jobLauncher.run(
                        osmImportJob,
                        new JobParametersBuilder()
                                .addLong("ts", System.currentTimeMillis(), true)
                                .toJobParameters());

        assertThat(ex.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
