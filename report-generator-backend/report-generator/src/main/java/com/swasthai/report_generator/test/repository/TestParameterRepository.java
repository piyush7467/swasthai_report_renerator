package com.swasthai.report_generator.test.repository;

import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.entity.TestParameterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TestParameterRepository
        extends JpaRepository<TestParameter, UUID> {

    Optional<TestParameter> findByRefId(String refId);

    boolean existsByRefId(String refId);

    Optional<TestParameter> findByTest_IdAndCode(
            UUID testId,
            String code);

    boolean existsByTest_IdAndCode(
            UUID testId,
            String code);

    boolean existsByTest_IdAndCodeAndIdNot(
            UUID testId,
            String code,
            UUID id);

    Page<TestParameter> findAllByTest_Id(
            UUID testId,
            Pageable pageable);

    Page<TestParameter> findAllByTest_IdAndStatus(
            UUID testId,
            TestParameterStatus status,
            Pageable pageable);

    Page<TestParameter> findAllByTest_RefId(
            String testRefId,
            Pageable pageable);

    Page<TestParameter> findAllByTest_RefIdAndStatus(
            String testRefId,
            TestParameterStatus status,
            Pageable pageable);

    boolean existsByTest_IdAndDisplayOrder(
            UUID testId,
            Integer displayOrder);
}