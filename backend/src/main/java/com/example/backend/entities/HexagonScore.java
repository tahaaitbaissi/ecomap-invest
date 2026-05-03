package com.example.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hexagon_score")
@Getter
@Setter
@NoArgsConstructor
public class HexagonScore {

    @EmbeddedId
    private HexagonScoreId id;

    @Column(nullable = false)
    private Double score;

    @Column(name = "last_calculated", nullable = false)
    private Instant lastCalculated;

    public HexagonScore(String h3Index, java.util.UUID profileId, double score, Instant lastCalculated) {
        this.id = new HexagonScoreId(h3Index, profileId);
        this.score = score;
        this.lastCalculated = lastCalculated;
    }
}
