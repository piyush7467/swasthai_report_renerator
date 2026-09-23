import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type { SpringPage } from "@/modules/super-admin/tests/types/testTypes";
import type {
  AddReportTestRequest,
  AddReportTestsBulkRequest,
  CreateReportRequest,
  DeleteReportResponse,
  ReorderReportTestsRequest,
  ReportQueryParams,
  ReportResponse,
  UpdateReportHeaderOptionRequest,
  UpdateReportParametersRequest,
} from "../types/reportTypes";

export const reportApi = {
  async createReport(request: CreateReportRequest): Promise<ReportResponse> {
    const response = await apiClient.post<ApiResponse<ReportResponse>>(
      "/reports",
      request,
    );
    return response.data.data;
  },

  async getMyReports(
    params: ReportQueryParams = {},
  ): Promise<SpringPage<ReportResponse>> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: params.sort ?? "createdAt",
      direction: params.direction ?? "desc",
    };

    if (params.status) {
      queryParams.status = params.status;
    }

    const response = await apiClient.get<
      ApiResponse<SpringPage<ReportResponse>>
    >("/reports/my", {
      params: queryParams,
    });
    return response.data.data;
  },

  async getReport(reportRefId: string): Promise<ReportResponse> {
    const response = await apiClient.get<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}`,
    );
    return response.data.data;
  },

  async addTest(
    reportRefId: string,
    request: AddReportTestRequest,
  ): Promise<ReportResponse> {
    const response = await apiClient.post<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/tests`,
      request,
    );
    return response.data.data;
  },

  async addTestsBulk(
    reportRefId: string,
    request: AddReportTestsBulkRequest,
  ): Promise<ReportResponse> {
    const response = await apiClient.post<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/tests/bulk`,
      request,
    );
    return response.data.data;
  },

  async recalculateReport(reportRefId: string): Promise<ReportResponse> {
    const response = await apiClient.post<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/recalculate`,
    );
    return response.data.data;
  },

  async removeTest(
    reportRefId: string,
    reportTestRefId: string,
  ): Promise<ReportResponse> {
    const response = await apiClient.delete<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/tests/${encodeURIComponent(
        reportTestRefId,
      )}`,
    );
    return response.data.data;
  },

  async updateParameters(
    reportRefId: string,
    reportTestRefId: string,
    request: UpdateReportParametersRequest,
  ): Promise<ReportResponse> {
    const response = await apiClient.patch<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/tests/${encodeURIComponent(
        reportTestRefId,
      )}/parameters`,
      request,
    );
    return response.data.data;
  },

  async reorderTests(
    reportRefId: string,
    request: ReorderReportTestsRequest,
  ): Promise<ReportResponse> {
    const response = await apiClient.patch<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/tests/reorder`,
      request,
    );
    return response.data.data;
  },

  async updateHeaderOption(
    reportRefId: string,
    request: UpdateReportHeaderOptionRequest,
  ): Promise<ReportResponse> {
    const response = await apiClient.patch<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/header-option`,
      request,
    );
    return response.data.data;
  },

  async finalizeReport(reportRefId: string): Promise<ReportResponse> {
    const response = await apiClient.post<ApiResponse<ReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}/finalize`,
    );
    return response.data.data;
  },

  async deleteReport(reportRefId: string): Promise<DeleteReportResponse> {
    const response = await apiClient.delete<ApiResponse<DeleteReportResponse>>(
      `/reports/${encodeURIComponent(reportRefId)}`,
    );
    return response.data.data;
  },

  async downloadReportPdf(reportRefId: string): Promise<Blob> {
    const response = await apiClient.get<Blob>(
      `/reports/${encodeURIComponent(reportRefId)}/pdf`,
      {
        responseType: "blob",
      },
    );
    return response.data;
  },

  async getReportQrBlob(reportRefId: string): Promise<Blob> {
    const response = await apiClient.get<Blob>(
      `/reports/${encodeURIComponent(reportRefId)}/qr`,
      {
        responseType: "blob",
      },
    );
    return response.data;
  },

  async createReportShare(
    reportRefId: string,
    request: import("../types/reportTypes").CreateReportShareRequest,
  ): Promise<import("../types/reportTypes").ReportShareResponse> {
    const response = await apiClient.post<
      ApiResponse<import("../types/reportTypes").ReportShareResponse>
    >(`/reports/${encodeURIComponent(reportRefId)}/shares`, request);
    return response.data.data;
  },

  async getSharedReport(
    shareToken: string,
  ): Promise<import("../types/reportTypes").SharedReportResponse> {
    const response = await apiClient.get<
      ApiResponse<import("../types/reportTypes").SharedReportResponse>
    >(`/shared/reports/${encodeURIComponent(shareToken)}`);
    return response.data.data;
  },

  async downloadSharedReportPdf(shareToken: string): Promise<Blob> {
    const response = await apiClient.get<Blob>(
      `/shared/reports/${encodeURIComponent(shareToken)}/pdf`,
      {
        responseType: "blob",
      },
    );
    return response.data;
  },
};
