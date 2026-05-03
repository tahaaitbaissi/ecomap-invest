package com.example.backend.repositories;

import com.example.backend.entities.Investment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentRepository extends JpaRepository<Investment, UUID> {

    List<Investment> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
