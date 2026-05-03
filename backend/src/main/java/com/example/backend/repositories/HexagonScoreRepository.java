package com.example.backend.repositories;

import com.example.backend.entities.HexagonScore;
import com.example.backend.entities.HexagonScoreId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HexagonScoreRepository extends JpaRepository<HexagonScore, HexagonScoreId> {

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO hexagon_score (h3_index, profile_id, score, last_calculated)
                    VALUES (:h3Index, :profileId, :score, CURRENT_TIMESTAMP)
                    ON CONFLICT (h3_index, profile_id)
                    DO UPDATE SET score = EXCLUDED.score, last_calculated = EXCLUDED.last_calculated
                    """,
            nativeQuery = true)
    void upsert(
            @Param("h3Index") String h3Index,
            @Param("profileId") UUID profileId,
            @Param("score") double score);
}
