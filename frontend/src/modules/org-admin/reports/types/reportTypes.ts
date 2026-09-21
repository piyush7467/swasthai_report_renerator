export type ReportStatus = "DRAFT" | "CALCULATED" | "FINALIZED";

export type TestParameterDataType =
  | "INTEGER"
  | "DECIMAL"
  | "TEXT"
  | "BOOLEAN"
  | "SELECT";

export type ParameterInputType = "MANUAL" | "CALCULATED";

export type CalculationType = "NONE" | "MCV" | "MCH" | "MCHC";

export type ResultFlag =
  | "NORMAL"
  | "LOW"
  | "HIGH"
  | "CRITICAL_LOW"
  | "CRITICAL_HIGH";

export interface ReportParameterItemResponse {
  refId: string;
  parameterRefId: string;
  parameterCode: string;
  parameterName: string;
  dataType: TestParameterDataType;
  inputType: ParameterInputType;
  calculationType: CalculationType;
  calculationVersion?: string | null;
  unit?: string | null;
  value?: string | null;
  numericValue?: number | null;
  flag?: ResultFlag | null;
  referenceMin?: number | null;
  referenceMax?: number | null;
  criticalLow?: number | null;
  criticalHigh?: number | null;
  displayOrder: number;
}

export interface ReportTestItemResponse {
  refId: string;
  testRefId: string;
  testCode: string;
  testName: string;
  displayOrder: number;
  testVersion: number;
  parameters: ReportParameterItemResponse[];
}

export interface ReportResponse {
  refId: string;
  organizationRefId: string;
  organizationName: string;
  patientRefId: string;
  status: ReportStatus;
  reportVersion: number;
  lockVersion: number;
  createdByEmail?: string | null;
  finalizedByEmail?: string | null;
  finalizedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  tests: ReportTestItemResponse[];
  includeOrganizationHeader: boolean;
}

export interface CreateReportRequest {
  patientRefId: string;
  includeOrganizationHeader?: boolean;
}

export interface AddReportTestRequest {
  testRefId: string;
  lockVersion?: number;
}

export interface TestParameterResultInput {
  parameterRefId?: string;
  parameterCode?: string;
  value?: string | null;
}

export interface UpdateReportParametersRequest {
  lockVersion?: number;
  parameters: TestParameterResultInput[];
}

export interface UpdateReportHeaderOptionRequest {
  includeOrganizationHeader: boolean;
}

export interface TestOrderItemInput {
  reportTestRefId: string;
  displayOrder: number;
}

export interface ReorderReportTestsRequest {
  lockVersion?: number;
  testOrders: TestOrderItemInput[];
}

export interface DeleteReportResponse {
  reportRefId: string;
  deleted: boolean;
  deletedAt: string;
}

export interface ReportQueryParams {
  status?: ReportStatus;
  page?: number;
  size?: number;
  sort?: "createdAt" | "updatedAt" | "status" | "reportVersion";
  direction?: "asc" | "desc";
}
