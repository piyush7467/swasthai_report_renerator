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
} from "../types/patientTypes";

export const PATIENT_QUERY_KEYS = {
  all: ["patients"] as const,
  list: (params: PatientQueryParams) => ["patients", "list", params] as const,
  detail: (refId: string) => ["patients", "detail", refId] as const,
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

