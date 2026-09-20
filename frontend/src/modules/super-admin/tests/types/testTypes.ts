export type TestType = "INDIVIDUAL" | "PANEL" | "PROFILE";

export type SampleType =
  | "WHOLE_BLOOD"
  | "SERUM"
  | "PLASMA"
  | "URINE"
  | "STOOL"
  | "CSF"
  | "SWAB"
  | "SEMEN"
  | "SALIVA"
  | "OTHER";

export type TestStatus = "ACTIVE" | "INACTIVE";

export interface TestResponse {
  refId: string;
  categoryRefId: string;
  categoryName: string;
  code: string;
  name: string;
  shortName?: string | null;
  testType: TestType;
  description?: string | null;

  // Sample information
  sampleType: SampleType;
  customSampleType?: string | null;
  specimenContainer?: string | null;
  sampleVolume?: number | null;
  sampleVolumeUnit?: string | null;
  fastingRequired: boolean;
  patientPreparation?: string | null;
  collectionInstructions?: string | null;

  // Processing
  turnaroundTimeHours?: number | null;
  prioritySupported: boolean;
  outsourced: boolean;
  laboratoryInstructions?: string | null;

  // Reporting
  reportSection?: string | null;
  displayOrder?: number | null;
  reportDescription?: string | null;
  interpretationGuidance?: string | null;

  // Commercial
  basePrice?: number | null;
  currency?: string | null;
  billingCode?: string | null;

  // Lifecycle
  status: TestStatus;
  version: number;
  effectiveFrom?: string | null;
  effectiveUntil?: string | null;

  // Audit
  createdAt: string;
  updatedAt: string;
}

export interface CreateTestRequest {
  code: string;
  name: string;
  shortName?: string;
  categoryRefId: string;
  testType: TestType;
  description?: string;
  sampleType: SampleType;
  customSampleType?: string;
  specimenContainer?: string;
  sampleVolume?: number;
  sampleVolumeUnit?: string;
  fastingRequired?: boolean;
  patientPreparation?: string;
  collectionInstructions?: string;
  turnaroundTimeHours?: number;
  prioritySupported?: boolean;
  outsourced?: boolean;
  laboratoryInstructions?: string;
  reportSection?: string;
  displayOrder?: number;
  reportDescription?: string;
  interpretationGuidance?: string;
  basePrice?: number;
  currency?: string;
  billingCode?: string;
  effectiveFrom?: string;
  effectiveUntil?: string;
}

export interface UpdateTestRequest {
  code?: string;
  name?: string;
  shortName?: string;
  categoryRefId?: string;
  testType?: TestType;
  description?: string;
  sampleType?: SampleType;
  customSampleType?: string;
  specimenContainer?: string;
  sampleVolume?: number;
  sampleVolumeUnit?: string;
  fastingRequired?: boolean;
  patientPreparation?: string;
  collectionInstructions?: string;
  turnaroundTimeHours?: number;
  prioritySupported?: boolean;
  outsourced?: boolean;
  laboratoryInstructions?: string;
  reportSection?: string;
  displayOrder?: number;
  reportDescription?: string;
  interpretationGuidance?: string;
  basePrice?: number;
  currency?: string;
  billingCode?: string;
  status?: TestStatus;
  effectiveFrom?: string;
  effectiveUntil?: string;
}

export interface TestQueryParams {
  search?: string;
  categoryRefId?: string;
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
  numberOfElements?: number;
  empty?: boolean;
}
