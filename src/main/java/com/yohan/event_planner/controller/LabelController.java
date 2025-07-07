package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.service.LabelService;
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
@RequestMapping("/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    // Get a label by ID
    @GetMapping("/{id}")
    public ResponseEntity<LabelResponseDTO> getLabelById(@PathVariable Long id) {
        LabelResponseDTO labelResponseDTO = labelService.getLabelById(id);
        return labelResponseDTO != null ? ResponseEntity.ok(labelResponseDTO) : ResponseEntity.notFound().build();
    }

    // Create a new label
    @PostMapping
    public ResponseEntity<LabelResponseDTO> createLabel(@Valid @RequestBody LabelCreateDTO labelCreateDTO) {
        LabelResponseDTO createdLabel = labelService.createLabel(labelCreateDTO);
        return new ResponseEntity<>(createdLabel, HttpStatus.CREATED);
    }

    // Update a label by ID
    @PatchMapping("/{id}")
    public ResponseEntity<LabelResponseDTO> updateLabel(@PathVariable Long id, @Valid @RequestBody LabelUpdateDTO labelUpdateDTO) {
        LabelResponseDTO updatedLabel = labelService.updateLabel(id, labelUpdateDTO);
        return ResponseEntity.ok(updatedLabel);
    }


    // Delete a label by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id) {
        labelService.deleteLabel(id);
        return ResponseEntity.noContent().build();
    }
}
