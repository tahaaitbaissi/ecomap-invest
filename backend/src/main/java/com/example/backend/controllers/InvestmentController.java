package com.example.backend.controllers;

import com.example.backend.controllers.dto.CreateInvestmentRequest;
import com.example.backend.controllers.dto.InvestmentResponse;
import com.example.backend.services.InvestmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Investments", description = "User investment rows (same DB; optional Atomikos JTA profile)")
@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class InvestmentController {

    private final InvestmentService investmentService;

    @Operation(summary = "Record an investment for the current user")
    @PostMapping
    public ResponseEntity<InvestmentResponse> create(
            @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody CreateInvestmentRequest request) {
        InvestmentResponse out = investmentService.create(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    @Operation(summary = "List investments for the current user")
    @GetMapping
    public ResponseEntity<List<InvestmentResponse>> list(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(investmentService.listMine(principal.getUsername()));
    }
}
