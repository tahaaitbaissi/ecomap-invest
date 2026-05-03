package com.example.backend.services;

import com.example.backend.audit.Audited;
import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads enterprise POIs from an Excel workbook on the classpath (first sheet, header row).
 * Expected columns (French labels, case-insensitive): Nom, Categorie, Ville, Adresse, Longitude,
 * Latitude.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseXlsxIngestionService {

    private static final int OSM_ID_MAX = 255;
    private static final int NAME_MAX = 255;
    private static final int ADDRESS_MAX = 255;
    private static final int TYPE_TAG_MAX = 255;

    private final PoiRepository poiRepository;
    private final GeometryFactory geometryFactory;

    @Transactional
    @Audited(action = "INGEST_ENTERPRISE_XLSX")
    public int ingestFromClasspath(String classpathFile) {
        int saved = 0;
        int skipped = 0;
        int skippedInvalid = 0;

        try (InputStream in = new ClassPathResource(classpathFile).getInputStream();
                Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalStateException("Workbook has no header row");
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> col = mapHeaderRow(headerRow, formatter);

            int idxNom = requireColumn(col, "nom");
            int idxCat = requireColumn(col, "categorie");
            int idxLon = requireColumn(col, "longitude");
            int idxLat = requireColumn(col, "latitude");
            Integer idxVille = col.get("ville");
            Integer idxAdr = col.get("adresse");

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String nom = trimToNull(stringCell(row, idxNom, formatter));
                if (nom == null) {
                    continue;
                }
                String categorie = trimToNull(stringCell(row, idxCat, formatter));
                if (categorie == null) {
                    skippedInvalid++;
                    continue;
                }
                Double lon = parseCoordinate(row, idxLon, formatter);
                Double lat = parseCoordinate(row, idxLat, formatter);
                if (lon == null || lat == null || !Double.isFinite(lon) || !Double.isFinite(lat)) {
                    log.debug("Skipping row {}: missing coordinates for {}", r + 1, nom);
                    skippedInvalid++;
                    continue;
                }

                String osmId = buildEnterpriseOsmId(nom, lat, lon);
                if (poiRepository.findByOsmId(osmId).isPresent()) {
                    skipped++;
                    continue;
                }

                String ville = idxVille != null ? trimToNull(stringCell(row, idxVille, formatter)) : null;
                String adresse = idxAdr != null ? trimToNull(stringCell(row, idxAdr, formatter)) : null;
                String fullAddress = mergeAddress(ville, adresse);

                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

                Poi poi = new Poi();
                poi.setOsmId(truncate(osmId, OSM_ID_MAX));
                poi.setName(truncate(nom, NAME_MAX));
                poi.setAddress(truncate(fullAddress, ADDRESS_MAX));
                poi.setTypeTag(truncate("enterprise=" + categorie, TYPE_TAG_MAX));
                poi.setLocation(point);
                poi.setImportedAt(new Timestamp(System.currentTimeMillis()));

                poiRepository.save(poi);
                saved++;
            }
        } catch (Exception e) {
            log.error("Enterprise XLSX ingestion failed", e);
            throw new RuntimeException("Enterprise XLSX ingestion failed: " + e.getMessage(), e);
        }

        log.info(
                "Enterprise XLSX ingestion complete. Saved: {}, Skipped (duplicates): {}, Skipped (invalid): {}",
                saved,
                skipped,
                skippedInvalid);
        return saved;
    }

    private static int requireColumn(Map<String, Integer> col, String key) {
        Integer i = col.get(key);
        if (i == null) {
            throw new IllegalStateException("Missing required column: " + key);
        }
        return i;
    }

    private static Map<String, Integer> mapHeaderRow(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> out = new HashMap<>();
        short last = headerRow.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) {
                continue;
            }
            String raw = formatter.formatCellValue(cell);
            String key = normalizeHeader(raw);
            if (!key.isEmpty()) {
                out.putIfAbsent(key, c);
            }
        }
        return out;
    }

    /** ASCII-ish key for header matching (handles accents). */
    static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        String nfd = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return nfd.toLowerCase(Locale.ROOT).replace('\u00a0', ' ');
    }

    private static String stringCell(Row row, int colIdx, DataFormatter formatter) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        return formatter.formatCellValue(cell);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Double parseCoordinate(Row row, int colIdx, DataFormatter formatter) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        String s = formatter.formatCellValue(cell).trim().replace(',', '.');
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String mergeAddress(String ville, String adresse) {
        if (ville == null && adresse == null) {
            return null;
        }
        if (ville == null) {
            return adresse;
        }
        if (adresse == null) {
            return ville;
        }
        if (adresse.contains(ville)) {
            return adresse;
        }
        return ville + " — " + adresse;
    }

    /** Stable id so re-import skips the same logical row. */
    static String buildEnterpriseOsmId(String nom, double lat, double lon) {
        String key = nom.trim() + "|" + lat + "|" + lon;
        UUID u = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        return "enterprise:" + u;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
