package com.swasthai.report_generator.test.repository;

import com.swasthai.report_generator.test.entity.TestParameterResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestParameterResultRepository
        extends JpaRepository<TestParameterResult, UUID> {

    Optional<TestParameterResult> findByRefId(String refId);

    boolean existsByRefId(String refId);

    Optional<TestParameterResult> findByPatientTestResult_IdAndTestParameter_Id(
            UUID patientTestResultId,
            UUID testParameterId
    );

    List<TestParameterResult> findAllByPatientTestResult_IdOrderByDisplayOrderAsc(
            UUID patientTestResultId
    );

    void deleteAllByPatientTestResult_Id(UUID patientTestResultId);

    // ReportTestResult queries for multi-test report workflow
    Optional<TestParameterResult> findByReportTestResult_IdAndTestParameter_Id(
            UUID reportTestResultId,
            UUID testParameterId
    );

    List<TestParameterResult> findAllByReportTestResult_IdOrderByDisplayOrderAsc(
            UUID reportTestResultId
    );

    void deleteAllByReportTestResult_Id(UUID reportTestResultId);
}