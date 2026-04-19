package com.example.backend.batch;

import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@StepScope
public class PoiItemWriter implements ItemWriter<Poi> {

    private static final Logger log = LoggerFactory.getLogger(PoiItemWriter.class);
    private final PoiRepository poiRepository;
    private int written = 0, skipped = 0;

    public PoiItemWriter(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends Poi> items) {
        for (Poi poi : items) {
            try {
                poiRepository.save(poi);
                written++;
            } catch (DataIntegrityViolationException e) {
                skipped++;
            }
        }
        log.info("Batch — écrits: {}, skippés: {}", written, skipped);
    }
}