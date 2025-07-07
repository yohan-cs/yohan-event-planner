package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.service.BadgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    // Get a badge by ID
    @GetMapping("/{id}")
    public ResponseEntity<BadgeResponseDTO> getBadgeById(@PathVariable Long id) {
        BadgeResponseDTO badgeResponseDTO = badgeService.getBadgeById(id);
        return badgeResponseDTO != null ? ResponseEntity.ok(badgeResponseDTO) : ResponseEntity.notFound().build();
    }

    // Create a new badge
    @PostMapping
    public ResponseEntity<BadgeResponseDTO> createBadge(@Valid @RequestBody BadgeCreateDTO badgeCreateDTO) {
        BadgeResponseDTO createdBadge = badgeService.createBadge(badgeCreateDTO);
        return new ResponseEntity<>(createdBadge, HttpStatus.CREATED);
    }

    // Update a badge by ID
    @PatchMapping("/{id}")
    public ResponseEntity<BadgeResponseDTO> updateBadge(@PathVariable Long id, @Valid @RequestBody BadgeUpdateDTO badgeUpdateDTO) {
        BadgeResponseDTO updatedBadge = badgeService.updateBadge(id, badgeUpdateDTO);
        return ResponseEntity.ok(updatedBadge);
    }

    // Delete a badge by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBadge(@PathVariable Long id) {
        badgeService.deleteBadge(id);
        return ResponseEntity.noContent().build();
    }
}
