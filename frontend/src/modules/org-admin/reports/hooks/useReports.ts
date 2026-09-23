import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";
import { reportApi } from "../api/reportApi";
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

export const REPORT_QUERY_KEYS = {
  all: ["reports"] as const,
  list: (params: ReportQueryParams) => ["reports", "list", params] as const,
  detail: (reportRefId: string) => ["reports", "detail", reportRefId] as const,
};

export function useReportsQuery(
  params: ReportQueryParams = {},
): UseQueryResult<SpringPage<ReportResponse>, Error> {
  return useQuery({
    queryKey: REPORT_QUERY_KEYS.list(params),
    queryFn: () => reportApi.getMyReports(params),
    placeholderData: (previousData) => previousData,
  });
}

export function useReportQuery(
  reportRefId: string | undefined,
): UseQueryResult<ReportResponse, Error> {
  return useQuery({
    queryKey: REPORT_QUERY_KEYS.detail(reportRefId || ""),
    queryFn: () => reportApi.getReport(reportRefId!),
    enabled: Boolean(reportRefId),
  });
}

export function useCreateReportMutation(): UseMutationResult<
  ReportResponse,
  Error,
  CreateReportRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateReportRequest) => reportApi.createReport(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}

export function useAddReportTestMutation(): UseMutationResult<
  ReportResponse,
  Error,
  { reportRefId: string; request: AddReportTestRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ reportRefId, request }) =>
      reportApi.addTest(reportRefId, request),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}

export function useAddReportTestsBulkMutation(): UseMutationResult<
  ReportResponse,
  Error,
  { reportRefId: string; request: AddReportTestsBulkRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ reportRefId, request }) =>
      reportApi.addTestsBulk(reportRefId, request),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}

export function useRecalculateReportMutation(): UseMutationResult<
  ReportResponse,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reportRefId: string) =>
      reportApi.recalculateReport(reportRefId),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}

export function useRemoveReportTestMutation(): UseMutationResult<
  ReportResponse,
  Error,
  { reportRefId: string; reportTestRefId: string }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ reportRefId, reportTestRefId }) =>
      reportApi.removeTest(reportRefId, reportTestRefId),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}

export function useUpdateParametersMutation(): UseMutationResult<
  ReportResponse,
  Error,
  {
    reportRefId: string;
    reportTestRefId: string;
    request: UpdateReportParametersRequest;
  }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ reportRefId, reportTestRefId, request }) =>
      reportApi.updateParameters(reportRefId, reportTestRefId, request),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}

export function useUpdateHeaderOptionMutation(): UseMutationResult<
  ReportResponse,
  Error,
  { reportRefId: string; request: UpdateReportHeaderOptionRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ reportRefId, request }) =>
      reportApi.updateHeaderOption(reportRefId, request),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
    },
  });
}

export function useReorderTestsMutation(): UseMutationResult<
  ReportResponse,
  Error,
  { reportRefId: string; request: ReorderReportTestsRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ reportRefId, request }) =>
      reportApi.reorderTests(reportRefId, request),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
    },
  });
}

export function useFinalizeReportMutation(): UseMutationResult<
  ReportResponse,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reportRefId: string) => reportApi.finalizeReport(reportRefId),
    onSuccess: (data) => {
      queryClient.setQueryData(REPORT_QUERY_KEYS.detail(data.refId), data);
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}

export function useDeleteReportMutation(): UseMutationResult<
  DeleteReportResponse,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reportRefId: string) => reportApi.deleteReport(reportRefId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: REPORT_QUERY_KEYS.all });
    },
  });
}
