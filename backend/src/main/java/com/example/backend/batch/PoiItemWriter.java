package com.example.backend.batch;

import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
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
        try {
            List<Poi> list = List.copyOf(items.getItems());
            poiRepository.saveAll(list);
            written += list.size();
        } catch (DataIntegrityViolationException e) {
            // Unique constraint on osm_id can trip on concurrent/import duplicates.
            // Fall back to per-item saves to count skips precisely.
            for (Poi poi : items) {
                try {
                    poiRepository.save(poi);
                    written++;
                } catch (DataIntegrityViolationException ex) {
                    skipped++;
                }
            }
        }
        log.info("Batch — écrits: {}, skippés: {}", written, skipped);
    }
}