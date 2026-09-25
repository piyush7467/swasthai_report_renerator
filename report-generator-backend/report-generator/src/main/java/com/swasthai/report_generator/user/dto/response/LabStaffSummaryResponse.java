package com.swasthai.report_generator.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabStaffSummaryResponse {

    private long totalStaff;
    private long activeStaff;
    private int maxLabStaff;
    private int remainingSlots;
    private boolean limitReached;
    private String planCode;
    private String planName;
}
