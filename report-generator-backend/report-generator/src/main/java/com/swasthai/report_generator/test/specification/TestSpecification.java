package com.swasthai.report_generator.test.specification;

import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class TestSpecification {

    private TestSpecification() {
    }

    public static Specification<Test> search(String search) {

        if (search == null || search.isBlank()) {
            return null;
        }

        String value = "%" + search.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("code")),
                                value
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                value
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("shortName")),
                                value
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")),
                                value
                        )
                );
    }

    public static Specification<Test> hasStatus(
            TestStatus status
    ) {

        if (status == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<Test> belongsToCategory(
            String categoryRefId
    ) {

        if (categoryRefId == null || categoryRefId.isBlank()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {

            Join<Test, TestCategory> category =
                    root.join("category", JoinType.INNER);

            return criteriaBuilder.equal(
                    category.get("refId"),
                    categoryRefId.trim()
                );
        };
    }
}
