package com.swasthai.report_generator.report.controller;

import com.swasthai.report_generator.report.dto.response.PublicReportVerificationResponse;
import com.swasthai.report_generator.report.service.PublicReportVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/reports")
@RequiredArgsConstructor
public class PublicReportVerificationController {

    private final PublicReportVerificationService verificationService;

    @GetMapping("/{reportRefId}/verify")
    public ResponseEntity<PublicReportVerificationResponse> verifyReport(
            @PathVariable String reportRefId,
            HttpServletRequest request
    ) {
        String clientIp = request != null ? request.getRemoteAddr() : null;

        PublicReportVerificationResponse response =
                verificationService.verifyReport(reportRefId, clientIp);

        if (response.valid()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
