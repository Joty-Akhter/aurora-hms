package com.easyops.hospital.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClinicalChartItemServiceBuildPatternTest {

    @Test
    void buildContainsLikePattern_nullOrBlank_returnsNull() {
        assertNull(ClinicalChartItemService.buildContainsLikePattern(null));
        assertNull(ClinicalChartItemService.buildContainsLikePattern(""));
    }

    @Test
    void buildContainsLikePattern_wrapsLowercaseTerm() {
        assertEquals("%cbc%", ClinicalChartItemService.buildContainsLikePattern("CBC"));
        assertEquals("%x-ray%", ClinicalChartItemService.buildContainsLikePattern("x-ray"));
        assertEquals("%abc%", ClinicalChartItemService.buildContainsLikePattern("  abc  "));
    }

    @Test
    void buildContainsLikePattern_escapesLikeMetacharactersUsingBangEscape() {
        assertEquals("%100!%%", ClinicalChartItemService.buildContainsLikePattern("100%"));
        assertEquals("%a!_b%", ClinicalChartItemService.buildContainsLikePattern("a_b"));
        assertEquals("%p!!at%", ClinicalChartItemService.buildContainsLikePattern("p!at"));
    }
}
