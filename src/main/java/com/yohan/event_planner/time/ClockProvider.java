package com.yohan.event_planner.time;

import com.yohan.event_planner.domain.User;

import java.time.Clock;
import java.time.ZoneId;

public interface ClockProvider {
    Clock getClockForZone(ZoneId zoneId);
    Clock getClockForUser(User user);
}