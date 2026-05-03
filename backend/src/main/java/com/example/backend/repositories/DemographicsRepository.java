package com.example.backend.repositories;

import com.example.backend.entities.Demographics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemographicsRepository extends JpaRepository<Demographics, Long> {

    /**
     * Permet de récupérer les données démographiques d'un hexagone spécifique.
     * Très utilisé par l'algorithme de scoring (F6.1.4).
     */
    Optional<Demographics> findByH3Index(String h3Index);

    // Vérifie si des données existent déjà pour éviter les doublons lors du seeding
    boolean existsByH3Index(String h3Index);
}