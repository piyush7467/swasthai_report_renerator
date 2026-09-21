package com.swasthai.report_generator.admin.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTrendPointResponse {

    private String date;
    private long totalCount;
    private long finalizedCount;
    private long draftCount;
}
