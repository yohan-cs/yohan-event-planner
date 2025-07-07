package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.EventRecap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRecapRepository extends JpaRepository<EventRecap, Long> {
}
