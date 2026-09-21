package com.swasthai.report_generator.admin.analytics.repository;

public interface TestUsageProjection {

    String getTestRefId();

    String getTestCode();

    String getTestName();

    String getTestShortName();

    String getCategoryName();

    Long getUsageCount();
}
