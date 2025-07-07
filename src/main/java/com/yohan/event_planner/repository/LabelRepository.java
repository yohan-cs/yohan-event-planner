package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {

    // Method to fetch labels by creator's userId and sort by name in ascending order
    List<Label> findAllByCreatorIdOrderByNameAsc(Long userId);

    // Check if a label exists by name and creator
    boolean existsByNameAndCreator(String name, User creator);

    // Optional: If you want a method that filters out the "Unlabeled" label directly from DB query,
    // you can add a custom query here (though handling in service is fine):
    List<Label> findAllByCreatorIdAndIdNot(Long userId, Long excludedId);
}
