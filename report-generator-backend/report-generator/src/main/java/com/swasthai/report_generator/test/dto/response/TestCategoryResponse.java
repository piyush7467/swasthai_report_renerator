package com.swasthai.report_generator.test.dto.response;

import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCategoryResponse {

    private String refId;
    private String code;
    private String name;
    private String description;
    private TestCategoryStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}