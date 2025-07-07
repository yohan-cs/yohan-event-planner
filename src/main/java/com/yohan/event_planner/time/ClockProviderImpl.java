package com.yohan.event_planner.time;

import com.yohan.event_planner.domain.User;
import java.time.Clock;
import java.time.ZoneId;

public class ClockProviderImpl implements ClockProvider {

    private final Clock baseClock;

    public ClockProviderImpl(Clock baseClock) {
        this.baseClock = baseClock;
    }

    @Override
    public Clock getClockForZone(ZoneId zoneId) {
        return baseClock.withZone(zoneId);
    }

    @Override
    public Clock getClockForUser(User user) {
        ZoneId userZone = ZoneId.of(user.getTimezone());
        return baseClock.withZone(userZone);
    }
}