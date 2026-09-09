package com.swasthai.report_generator.test.repository;

import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestCategoryRepository
        extends JpaRepository<TestCategory, UUID> {

    Optional<TestCategory> findByRefId(String refId);

    Optional<TestCategory> findByCode(String code);

    Optional<TestCategory> findByNameIgnoreCase(String name);

    boolean existsByRefId(String refId);

    boolean existsByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    List<TestCategory> findAllByStatusOrderByNameAsc(
            TestCategoryStatus status
    );

    @Query(value = "SELECT c FROM TestCategory c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT count(c) FROM TestCategory c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TestCategory> findWithFilters(
            @Param("status") TestCategoryStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}