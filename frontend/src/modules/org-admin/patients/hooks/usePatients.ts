import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { patientApi } from "../api/patientApi";
import type {
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
