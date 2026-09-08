package com.swasthai.report_generator.test.dto.request;

import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTestCategoryRequest {

    @Size(max = 50, message = "Category code must not exceed 50 characters.")
    private String code;

    @Size(max = 100, message = "Category name must not exceed 100 characters.")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters.")
    private String description;

    private TestCategoryStatus status;
}