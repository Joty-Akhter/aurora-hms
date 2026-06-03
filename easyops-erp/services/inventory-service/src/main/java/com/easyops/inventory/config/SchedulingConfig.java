package com.easyops.inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enable Spring's @Scheduled annotation support
    // This allows the ReorderMonitoringService to run periodic checks
}

