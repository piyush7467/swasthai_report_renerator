export type Salutation = "MR" | "MRS" | "MS" | "MASTER" | "BABY" | "DR" | "OTHER";
export type Gender = "MALE" | "FEMALE" | "OTHER";
export type AgeUnit = "YEARS" | "MONTHS" | "WEEKS" | "DAYS";

export interface PatientResponse {
  refId: string;
  patientCode: string;
  salutation: string;
  name: string;
  dateOfBirthKnown: boolean;
  dateOfBirth?: string | null;
  ageValue?: number | null;
  ageUnit?: AgeUnit | null;
  gender: Gender;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  weightKg?: number | null;
  organizationRefId: string;
  totalReports?: number;
  lastReportDate?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PagedPatientsResponse {
  content: PatientResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PatientQueryParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: "asc" | "desc";
  search?: string;
}

export interface CreatePatientRequest {
  salutation: Salutation;
  name: string;
  dateOfBirthKnown: boolean;
  dateOfBirth?: string | null;
  ageValue?: number | null;
  ageUnit?: AgeUnit | null;
  gender: Gender;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  weightKg?: number | null;
}

export interface UpdatePatientRequest {
  salutation: Salutation;
  name: string;
  dateOfBirthKnown: boolean;
  dateOfBirth?: string | null;
  ageValue?: number | null;
  ageUnit?: AgeUnit | null;
  gender: Gender;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  weightKg?: number | null;
}
