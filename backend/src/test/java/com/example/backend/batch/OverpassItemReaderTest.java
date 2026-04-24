package com.example.backend.batch;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.overpass.BoundingBox;
import com.example.backend.overpass.OverpassApiClient;
import com.example.backend.overpass.OsmElement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverpassItemReaderTest {

    @Mock
    private OverpassApiClient overpassApiClient;

    @Test
    @Timeout(60)
    void read_drainsAllCategoriesAndReturnsNull() throws Exception {
        when(overpassApiClient.fetchByTagAndBBox(any(String.class), any(BoundingBox.class)))
                .thenReturn(List.of());
        var reader = new OverpassItemReader(overpassApiClient);
        assertNull(reader.read());
        verify(overpassApiClient, atLeast(11))
                .fetchByTagAndBBox(any(String.class), any(BoundingBox.class));
    }
}
