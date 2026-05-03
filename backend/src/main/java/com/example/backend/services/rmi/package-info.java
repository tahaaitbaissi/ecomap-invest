/**
 * RMI client for the remote scoring JVM: {@link com.example.backend.services.rmi.RmiScoringClient}
 * wraps {@link com.example.backend.services.rmi.RmiSaturationInvoker} with Resilience4j (see
 * {@code resilience4j.*.instances.rmiScoring} and {@code rmiScoringPipeline} in {@code
 * application.yml}). Registry location is {@code app.rmi.scoring.host},
 * {@code app.rmi.scoring.port}, {@code app.rmi.scoring.service-name}. REST demos live under {@code
 * /api/v1/rmi} ({@link com.example.backend.controllers.RmiScoringController}); application scoring
 * uses {@link com.example.backend.scoring.DistributedScoringStrategy}.
 */
package com.example.backend.services.rmi;
