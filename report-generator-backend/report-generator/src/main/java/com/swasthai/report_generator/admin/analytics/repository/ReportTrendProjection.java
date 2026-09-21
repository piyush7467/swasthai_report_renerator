package com.swasthai.report_generator.admin.analytics.repository;

import java.sql.Date;

public interface ReportTrendProjection {

    Date getReportDate();

    Long getTotalCount();

    Long getFinalizedCount();

    Long getDraftCount();
}
