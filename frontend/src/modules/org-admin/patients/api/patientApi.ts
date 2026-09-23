import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  CreatePatientRequest,
  PagedPatientsResponse,
  PatientQueryParams,
  PatientResponse,
} from "../types/patientTypes";

export const patientApi = {
  async createPatient(request: CreatePatientRequest): Promise<PatientResponse> {
    const response = await apiClient.post<ApiResponse<PatientResponse>>(
      "/patients",
      request,
    );
    return response.data.data;
  },

  async getPatients(
    params: PatientQueryParams = {},
  ): Promise<PagedPatientsResponse> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sortBy: params.sortBy ?? "createdAt",
      sortDirection: params.sortDirection ?? "desc",
    };

    if (params.search && params.search.trim()) {
      queryParams.search = params.search.trim();
    }

    const response = await apiClient.get<ApiResponse<PagedPatientsResponse>>(
      "/patients",
      { params: queryParams },
    );
    return response.data.data;
  },

  async getPatient(patientRefId: string): Promise<PatientResponse> {
    const response = await apiClient.get<ApiResponse<PatientResponse>>(
      `/patients/${encodeURIComponent(patientRefId)}`,
    );
    return response.data.data;
  },
};
