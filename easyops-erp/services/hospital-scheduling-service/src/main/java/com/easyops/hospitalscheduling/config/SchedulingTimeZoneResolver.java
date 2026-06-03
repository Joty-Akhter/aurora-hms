package com.easyops.hospitalscheduling.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class SchedulingTimeZoneResolver {

    private final ZoneId defaultZone;

    public SchedulingTimeZoneResolver(
            @Value("${hospital.scheduling.default-timezone:UTC}") String defaultTimezone) {
        this.defaultZone = ZoneId.of(defaultTimezone);
    }

    public ZoneId getDefaultZone() {
        return defaultZone;
    }
}
