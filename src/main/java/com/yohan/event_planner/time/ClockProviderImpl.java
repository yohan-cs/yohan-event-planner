package com.yohan.event_planner.time;

import com.yohan.event_planner.domain.User;
import java.time.Clock;
import java.time.ZoneId;

public class ClockProviderImpl implements ClockProvider {

    public ClockProviderImpl() {
    }

    @Override
    public Clock getClockForZone(ZoneId zoneId) {
        return Clock.system(zoneId);
    }

    @Override
    public Clock getClockForUser(User user) {
        ZoneId userZone = ZoneId.of(user.getTimezone());
        return Clock.system(userZone);
    }
}