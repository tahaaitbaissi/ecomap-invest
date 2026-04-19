package com.example.backend.batch;

import com.example.backend.entities.Poi;
import com.example.backend.overpass.OsmElement;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public Step importPoisStep(JobRepository jobRepository,
                               PlatformTransactionManager txManager,
                               OverpassItemReader reader,
                               PoiItemProcessor processor,
                               PoiItemWriter writer) {
        return new StepBuilder("importPoisStep", jobRepository)
            .<OsmElement, Poi>chunk(100, txManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(500)
            .skip(DataIntegrityViolationException.class)
            .build();
    }

    @Bean
    public Job osmImportJob(JobRepository jobRepository, Step importPoisStep) {
        return new JobBuilder("osmImportJob", jobRepository)
            .start(importPoisStep)
            .build();
    }
}