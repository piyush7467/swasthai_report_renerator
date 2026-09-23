import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  CreatePatientRequest,
  PagedPatientsResponse,
  PatientQueryParams,
  PatientResponse,
  UpdatePatientRequest,
} from "../types/patientTypes";
import type { PagedReportsResponse } from "@/modules/org-admin/reports/types/reportTypes";

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

  async updatePatient(
    patientRefId: string,
    request: UpdatePatientRequest,
  ): Promise<PatientResponse> {
    const response = await apiClient.put<ApiResponse<PatientResponse>>(
      `/patients/${encodeURIComponent(patientRefId)}`,
      request,
    );
    return response.data.data;
  },

  async getPatientReports(
    patientRefId: string,
    params: { page?: number; size?: number; sortBy?: string; sortDirection?: "asc" | "desc" } = {},
  ): Promise<PagedReportsResponse> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 15,
      sortBy: params.sortBy ?? "createdAt",
      sortDirection: params.sortDirection ?? "desc",
    };

    const response = await apiClient.get<ApiResponse<PagedReportsResponse>>(
      `/patients/${encodeURIComponent(patientRefId)}/reports`,
      { params: queryParams },
    );
    return response.data.data;
  },
};
