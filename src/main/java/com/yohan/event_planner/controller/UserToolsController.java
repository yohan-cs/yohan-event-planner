package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.UserToolsResponseDTO;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.service.BadgeService;
import com.yohan.event_planner.service.LabelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for user tools such as badges and labels.
 */
@RestController
@RequestMapping("/usertools")
public class UserToolsController {

    private final BadgeService badgeService;
    private final LabelService labelService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserToolsController(BadgeService badgeService,
                               LabelService labelService,
                               AuthenticatedUserProvider authenticatedUserProvider) {
        this.badgeService = badgeService;
        this.labelService = labelService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Get all badges and labels for the authenticated user.
     *
     * @return the user's badges and labels
     */
    @GetMapping
    public ResponseEntity<UserToolsResponseDTO> getUserTools() {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();

        List<BadgeResponseDTO> badges = badgeService.getBadgesByUser(userId);
        List<LabelResponseDTO> labels = labelService.getLabelsByUser(userId);

        return ResponseEntity.ok(new UserToolsResponseDTO(badges, labels));
    }
}