package com.swasthai.report_generator.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private boolean success;

    private int status;

    private String code;

    private String message;

    private String path;

    private Instant timestamp;

    private Map<String, String> errors;
}