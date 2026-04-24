package com.example.backend.repositories;

import com.example.backend.entities.DynamicProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicProfileRepository extends JpaRepository<DynamicProfile, UUID> {}
