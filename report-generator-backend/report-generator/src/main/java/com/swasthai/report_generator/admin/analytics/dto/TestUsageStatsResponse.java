package com.swasthai.report_generator.admin.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestUsageStatsResponse {

    private String testRefId;
    private String testCode;
    private String testName;
    private String testShortName;
    private String categoryName;
    private long usageCount;
}
