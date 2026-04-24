package com.example.backend.services;

import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvIngestionServiceTest {

    @Mock
    private PoiRepository poiRepository;

    @InjectMocks
    private CsvIngestionService service;

    @Test
    void ingest_shouldSave5RowsFromMockCsv() {
        when(poiRepository.findByOsmId(anyString())).thenReturn(Optional.empty());
        when(poiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.ingestFromCsv("data/mock_poi.csv");

        verify(poiRepository, times(5)).save(any());
        assertEquals(5, count);
    }

    @Test
    void ingest_shouldSkipDuplicates() {
        when(poiRepository.findByOsmId(anyString())).thenReturn(Optional.of(new Poi()));

        int count = service.ingestFromCsv("data/mock_poi.csv");

        verify(poiRepository, never()).save(any());
        assertEquals(0, count);
    }

    @Test
    void ingest_missingFile_throws() {
        assertThrows(
                RuntimeException.class,
                () -> service.ingestFromCsv("data/does_not_exist.csv"));
    }
}
