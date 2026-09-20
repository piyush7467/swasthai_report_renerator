export type TestCategoryStatus = "ACTIVE" | "INACTIVE";

export interface TestCategoryResponse {
  refId: string;
  code: string;
  name: string;
  description?: string | null;
  status: TestCategoryStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTestCategoryRequest {
  code: string;
  name: string;
  description?: string;
}

export interface UpdateTestCategoryRequest {
  code?: string;
  name?: string;
  description?: string;
  status?: TestCategoryStatus;
}

export interface TestCategoryQueryParams {
  status?: TestCategoryStatus;
  search?: string;
  page?: number;
  size?: number;
  sortBy?: "name" | "code" | "status" | "createdAt" | "updatedAt";
  sortDirection?: "asc" | "desc";
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
