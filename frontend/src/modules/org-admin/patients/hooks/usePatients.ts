import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";
import { patientApi } from "../api/patientApi";
import type {
  CreatePatientRequest,
  PagedPatientsResponse,
  PatientQueryParams,
  PatientResponse,
  UpdatePatientRequest,
} from "../types/patientTypes";
import type { PagedReportsResponse } from "../../reports/types/reportTypes";

export const PATIENT_QUERY_KEYS = {
  all: ["patients"] as const,
  list: (params: PatientQueryParams) => ["patients", "list", params] as const,
  detail: (refId: string) => ["patients", "detail", refId] as const,
  reports: (refId: string, params?: unknown) =>
    ["patients", "reports", refId, params] as const,
};

export function usePatientsQuery(
  params: PatientQueryParams = {},
): UseQueryResult<PagedPatientsResponse, Error> {
  return useQuery({
    queryKey: PATIENT_QUERY_KEYS.list(params),
    queryFn: () => patientApi.getPatients(params),
    placeholderData: (previousData) => previousData,
  });
}

export function usePatientQuery(
  patientRefId: string | undefined,
): UseQueryResult<PatientResponse, Error> {
  return useQuery({
    queryKey: PATIENT_QUERY_KEYS.detail(patientRefId || ""),
    queryFn: () => patientApi.getPatient(patientRefId!),
    enabled: Boolean(patientRefId),
  });
}

export function usePatientReportsQuery(
  patientRefId: string | undefined,
  params: { page?: number; size?: number; sortBy?: string; sortDirection?: "asc" | "desc" } = {},
): UseQueryResult<PagedReportsResponse, Error> {
  return useQuery({
    queryKey: PATIENT_QUERY_KEYS.reports(patientRefId || "", params),
    queryFn: () => patientApi.getPatientReports(patientRefId!, params),
    enabled: Boolean(patientRefId),
    placeholderData: (previousData) => previousData,
  });
}

export function useCreatePatientMutation(): UseMutationResult<
  PatientResponse,
  Error,
  CreatePatientRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreatePatientRequest) =>
      patientApi.createPatient(request),
    onSuccess: (newPatient) => {
      queryClient.setQueryData(
        PATIENT_QUERY_KEYS.detail(newPatient.refId),
        newPatient,
      );
      void queryClient.invalidateQueries({ queryKey: PATIENT_QUERY_KEYS.all });
    },
  });
}

export function useUpdatePatientMutation(
  patientRefId: string,
): UseMutationResult<PatientResponse, Error, UpdatePatientRequest> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdatePatientRequest) =>
      patientApi.updatePatient(patientRefId, request),
    onSuccess: (updated) => {
      queryClient.setQueryData(
        PATIENT_QUERY_KEYS.detail(updated.refId),
        updated,
      );
      void queryClient.invalidateQueries({ queryKey: PATIENT_QUERY_KEYS.all });
    },
  });
}

