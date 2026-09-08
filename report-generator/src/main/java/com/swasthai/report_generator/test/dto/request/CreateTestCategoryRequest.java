package com.swasthai.report_generator.test.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestCategoryRequest {

    @NotBlank(message = "Category code is required.")
    @Size(max = 50, message = "Category code must not exceed 50 characters.")
    private String code;

    @NotBlank(message = "Category name is required.")
    @Size(max = 100, message = "Category name must not exceed 100 characters.")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters.")
    private String description;
}