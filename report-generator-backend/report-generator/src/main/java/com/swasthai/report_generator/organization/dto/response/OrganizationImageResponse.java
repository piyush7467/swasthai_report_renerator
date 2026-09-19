package com.swasthai.report_generator.organization.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrganizationImageResponse {

    private final byte[] bytes;
    private final String contentType;
}
