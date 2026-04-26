package com.example.backend.services;

import com.example.backend.controllers.dto.PoiCsvRecord;
import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvIngestionService {

    private final PoiRepository poiRepository;
    private final GeometryFactory geometryFactory;

    @Transactional
    public int ingestFromCsv(String classpathFile) {
        int saved = 0;
        int skipped = 0;

        try (Reader reader = new InputStreamReader(
                new ClassPathResource(classpathFile).getInputStream(),
                StandardCharsets.UTF_8)) {

            List<PoiCsvRecord> records = new CsvToBeanBuilder<PoiCsvRecord>(reader)
                    .withType(PoiCsvRecord.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();

            for (int i = 0; i < records.size(); i++) {
                PoiCsvRecord rec = records.get(i);
                String safeName = rec.getName() == null ? "unknown" : rec.getName().trim().replaceAll("\\s+", "_");
                String osmId = "csv:" + i + ":" + safeName;

                if (poiRepository.findByOsmId(osmId).isPresent()) {
                    log.info("Skipping duplicate: {}", osmId);
                    skipped++;
                    continue;
                }

                Point point = geometryFactory.createPoint(
                        new Coordinate(rec.getLongitude(), rec.getLatitude())
                );

                Poi poi = new Poi();
                poi.setOsmId(osmId);
                poi.setName(rec.getName());
                poi.setAddress(rec.getAddress());
                poi.setTypeTag("category=" + rec.getCategory());
                poi.setLocation(point);

                poiRepository.save(poi);
                saved++;
            }
        } catch (Exception e) {
            log.error("CSV ingestion failed", e);
            throw new RuntimeException("CSV ingestion failed: " + e.getMessage(), e);
        }

        log.info("CSV ingestion complete. Saved: {}, Skipped (duplicates): {}", saved, skipped);
        return saved;
    }
}
