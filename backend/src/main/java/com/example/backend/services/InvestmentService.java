package com.example.backend.services;

import com.example.backend.controllers.dto.CreateInvestmentRequest;
import com.example.backend.controllers.dto.InvestmentResponse;
import com.example.backend.entities.Investment;
import com.example.backend.entities.User;
import com.example.backend.repositories.InvestmentRepository;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final UserService userService;

    @Transactional
    public InvestmentResponse create(String userEmail, CreateInvestmentRequest req) {
        User user = userService.getUserByEmail(userEmail);
        Investment row = new Investment();
        row.setUserId(user.getId());
        row.setAmount(req.amount());
        row.setCurrency(req.currency().trim().toUpperCase());
        row.setNote(req.note() != null ? req.note().trim() : null);
        row.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        Investment saved = investmentRepository.save(row);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InvestmentResponse> listMine(String userEmail) {
        User user = userService.getUserByEmail(userEmail);
        return investmentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    private InvestmentResponse toDto(Investment i) {
        return new InvestmentResponse(
                i.getId(), i.getUserId(), i.getAmount(), i.getCurrency(), i.getNote(), i.getCreatedAt());
    }
}
