export type TestParameterDataType =
  | "INTEGER"
  | "DECIMAL"
  | "TEXT"
  | "BOOLEAN"
  | "DATE"
  | "DATETIME"
  | "ENUM";

export type ParameterInputType = "MANUAL" | "CALCULATED";

export type CalculationType = "NONE" | "MCV" | "MCH" | "MCHC";

export type TestParameterStatus = "ACTIVE" | "INACTIVE";

export interface TestParameterResponse {
  refId: string;
  testRefId: string;
  testCode: string;
  testName: string;
  code: string;
  name: string;
  description?: string | null;
  dataType: TestParameterDataType;
  inputType: ParameterInputType;
  calculationType: CalculationType;
  unit?: string | null;
  required: boolean;
  displayOrder: number;
  referenceMin?: number | null;
  referenceMax?: number | null;
  criticalLow?: number | null;
  criticalHigh?: number | null;
  reportDescription?: string | null;
  interpretationGuidance?: string | null;
  status: TestParameterStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTestParameterRequest {
  code: string;
  name: string;
  description?: string;
  dataType: TestParameterDataType;
  inputType: ParameterInputType;
  calculationType?: CalculationType;
  unit?: string;
  required?: boolean;
  displayOrder: number;
  referenceMin?: number;
  referenceMax?: number;
  criticalLow?: number;
  criticalHigh?: number;
  reportDescription?: string;
  interpretationGuidance?: string;
}

export interface UpdateTestParameterRequest {
  code?: string;
  name?: string;
  description?: string;
  dataType?: TestParameterDataType;
  inputType?: ParameterInputType;
  calculationType?: CalculationType;
  unit?: string;
  required?: boolean;
  displayOrder?: number;
  referenceMin?: number;
  referenceMax?: number;
  criticalLow?: number;
  criticalHigh?: number;
  reportDescription?: string;
  interpretationGuidance?: string;
  status?: TestParameterStatus;
}

export interface ParameterQueryParams {
  status?: string;
  page?: number;
  size?: number;
  sort?:
    | "displayOrder"
    | "code"
    | "name"
    | "dataType"
    | "status"
    | "version"
    | "createdAt"
    | "updatedAt";
  direction?: "asc" | "desc";
}
