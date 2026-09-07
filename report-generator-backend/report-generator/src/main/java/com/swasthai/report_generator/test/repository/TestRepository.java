package com.swasthai.report_generator.test.repository;

import com.swasthai.report_generator.test.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TestRepository
        extends JpaRepository<Test, UUID>,
                JpaSpecificationExecutor<Test> {

    Optional<Test> findByRefId(String refId);

    Optional<Test> findByCode(String code);

    boolean existsByRefId(String refId);

    boolean existsByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeAndIdNot(String code, UUID id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}