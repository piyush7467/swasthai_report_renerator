package com.swasthai.report_generator.organization.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUsageStatsResponse {
    private String categoryName;
    private long count;
}
