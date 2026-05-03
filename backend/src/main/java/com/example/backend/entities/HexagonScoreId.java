package com.example.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HexagonScoreId implements Serializable {

    @Column(name = "h3_index", nullable = false)
    private String h3Index;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HexagonScoreId that)) {
            return false;
        }
        return Objects.equals(h3Index, that.h3Index) && Objects.equals(profileId, that.profileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(h3Index, profileId);
    }
}
