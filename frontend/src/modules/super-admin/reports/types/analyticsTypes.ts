export interface AdminOverviewStats {
  totalReports: number;
  reportsToday: number;
  reportsThisWeek: number;
  reportsThisMonth: number;
  draftReports: number;
  calculatedReports: number;
  finalizedReports: number;
  deletedReportsToday: number;
  deletedReportsThisMonth: number;
  totalDeletedReports: number;
  activeOrganizationsCount: number;
  organizationsWithReportsCount: number;
  breakGlassAccessCount30Days: number;
}

export interface ReportTrendPoint {
  date: string;
  totalCount: number;
  finalizedCount: number;
  draftCount: number;
}

export interface OrganizationReportActivity {
  organizationRefId: string;
  organizationName: string;
  organizationCode: string;
  organizationStatus: "ACTIVE" | "INACTIVE" | "SUSPENDED";
  totalReports: number;
  reportsToday: number;
  reportsThisWeek: number;
  reportsThisMonth: number;
  draftReports: number;
  calculatedReports: number;
  finalizedReports: number;
  lastReportCreatedAt?: string | null;
}

export interface TestUsageStats {
  testRefId: string;
  testCode: string;
  testName: string;
  testShortName?: string | null;
  categoryName: string;
  usageCount: number;
}

export interface SecurityAuditLog {
  refId: string;
  actorEmail: string;
  action: string;
  targetOrganizationRefId?: string | null;
  targetOrganizationName?: string | null;
  targetReportRefId?: string | null;
  justification: string;
  success: boolean;
  failureReason?: string | null;
  ipAddress?: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TrendFilterParams {
  days?: number;
  from?: string;
  to?: string;
  organizationRefId?: string;
}

export interface OrgActivityFilterParams {
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
}

export interface SecurityAuditFilterParams {
  action?: string;
  targetOrganizationRefId?: string;
  success?: boolean;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
}
