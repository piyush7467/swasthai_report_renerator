export type LicenseStatus = "ACTIVE" | "DEACTIVATED" | "EXPIRED";

export type UpgradeRequestStatus =
  | "PENDING"
  | "CONTACTED"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED";

export interface LicenseResponse {
  refId: string;
  organizationRefId: string;
  planRefId: string;
  planCode: string;
  planName: string;
  maxLabStaff: number;
  maxReportsPerMonth?: number;
  maxReportsPerDay?: number;
  status: LicenseStatus;
  startedAt: string;
  expiresAt: string;
  currentlyUsable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StaffUsageSummary {
  activeStaff: number;
  maxLabStaff: number;
  remainingSlots: number;
  totalStaff: number;
  limitReached: boolean;
  overLimit: boolean;
}

export interface PatientUsageSummary {
  totalPatients: number;
}

export interface ReportUsageSummary {
  totalReports: number;
  finalizedReports: number;
  monthlyReportsCreated?: number;
  maxReportsPerMonth?: number;
  dailyReportsCreated?: number;
  maxReportsPerDay?: number;
  remainingMonthlyReports?: number;
  monthlyLimitReached?: boolean;
  dailyLimitReached?: boolean;
}

export interface OrganizationLicenseOverviewResponse {
  license: LicenseResponse;
  staffUsage: StaffUsageSummary;
  patientUsage: PatientUsageSummary;
  reportUsage: ReportUsageSummary;
  daysRemaining: number;
  expiryStatus: "ACTIVE" | "EXPIRING_SOON" | "EXPIRED" | "DEACTIVATED";
}

export interface AvailablePlanResponse {
  refId: string;
  code: string;
  name: string;
  description?: string | null;
  annualPrice: number;
  currency: string;
  maxLabStaff: number;
  maxReportsPerMonth?: number;
  maxReportsPerDay?: number;
  currentPlan: boolean;
  upgrade: boolean;
}

export interface CreateUpgradeRequest {
  requestedPlanRefId: string;
  reason?: string;
  contactName: string;
  contactEmail: string;
  contactPhone?: string;
  additionalMessage?: string;
}

export interface PlanUpgradeRequestResponse {
  refId: string;
  organizationRefId: string;
  organizationName: string;
  requestedByEmail: string;
  currentPlanRefId: string;
  currentPlanName: string;
  requestedPlanRefId: string;
  requestedPlanName: string;
  currentActiveStaffCount: number;
  requestedStaffCapacity: number;
  reason?: string | null;
  contactName: string;
  contactEmail: string;
  contactPhone?: string | null;
  additionalMessage?: string | null;
  status: UpgradeRequestStatus;
  reviewedByEmail?: string | null;
  reviewedAt?: string | null;
  adminNotes?: string | null;
  rejectionReason?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
