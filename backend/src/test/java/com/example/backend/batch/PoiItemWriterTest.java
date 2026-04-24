package com.example.backend.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PoiItemWriterTest {

    @Mock
    private PoiRepository poiRepository;

    @InjectMocks
    private PoiItemWriter writer;

    @Test
    void write_savesAll() {
        Poi a = new Poi();
        Poi b = new Poi();
        writer.write(Chunk.of(a, b));
        verify(poiRepository, times(2)).save(any(Poi.class));
    }

    @Test
    void write_dataIntegrity_continues() {
        when(poiRepository.save(any(Poi.class)))
                .thenThrow(new DataIntegrityViolationException("dup"))
                .thenAnswer(inv -> inv.getArgument(0, Poi.class));
        writer.write(Chunk.of(new Poi(), new Poi()));
        verify(poiRepository, times(2)).save(any(Poi.class));
    }
}
