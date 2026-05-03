package com.example.backend.repositories;

import com.example.backend.entities.DynamicProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicProfileRepository extends JpaRepository<DynamicProfile, UUID> {
    List<DynamicProfile> findByUserIdOrderByGeneratedAtDesc(UUID userId);

    Page<DynamicProfile> findByUserId(UUID userId, Pageable pageable);
}
