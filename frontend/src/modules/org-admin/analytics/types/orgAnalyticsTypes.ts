export interface OrgOverviewStats {
  totalReports: number;
  reportsToday: number;
  finalizedReports: number;
  totalPatients: number;
  activeTests: number;
  finalizedRate: number;
  reportsGrowthRate: number;
  patientsGrowthRate: number;
}

export interface OrgReportTrendPoint {
  date: string;
  totalReports: number;
  finalizedReports: number;
  draftReports: number;
}

export interface OrgCategoryUsageStats {
  categoryName: string;
  reportCount: number;
  percentage: number;
}

export interface OrgPatientTrendPoint {
  date: string;
  newPatients: number;
}

export interface OrgActivityItem {
  id: string;
  type: "REPORT_FINALIZED" | "REPORT_CREATED" | "PATIENT_REGISTERED" | string;
  description: string;
  userName: string;
  time: string;
  createdAt: string;
}
