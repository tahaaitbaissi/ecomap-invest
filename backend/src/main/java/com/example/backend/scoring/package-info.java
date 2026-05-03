/**
 * Saturation scoring strategy: {@link com.example.backend.scoring.ScoringStrategy} is bound from
 * {@code app.scoring.strategy} (see {@link com.example.backend.scoring.ScoringProperties} and
 * {@link com.example.backend.scoring.ScoringStrategyConfiguration}).
 *
 * <p>{@code local} uses {@link com.example.backend.scoring.SaturationFormula} in
 * {@link com.example.backend.scoring.LocalScoringStrategy}. {@code distributed} delegates to
 * {@link com.example.backend.scoring.DistributedScoringStrategy} and
 * {@link com.example.backend.services.rmi.RmiScoringClient#computeSaturationScoreForPipeline} (RMI
 * + Resilience4j, formula fallback on failure). The active implementation is injected into
 * {@link com.example.backend.services.PoiService}.
 */
package com.example.backend.scoring;
