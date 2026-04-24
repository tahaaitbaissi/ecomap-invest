package com.example.backend.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "dynamic_profile")
public class DynamicProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_query", nullable = false, columnDefinition = "TEXT")
    private String userQuery;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drivers_config", nullable = false, columnDefinition = "jsonb")
    private JsonNode driversConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "competitors_config", nullable = false, columnDefinition = "jsonb")
    private JsonNode competitorsConfig;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
