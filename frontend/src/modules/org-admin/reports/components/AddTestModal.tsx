import { useState, useMemo } from "react";
import {
  AlertCircle,
  Check,
  FlaskConical,
  Loader2,
  Plus,
  Search,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";

import { useTenantTestsQuery } from "../../tests/hooks/useTenantTests";
import { useAddReportTestMutation } from "../hooks/useReports";
import type { OrganizationTestResponse } from "@/modules/super-admin/tests/types/assignmentTypes";

interface AddTestModalProps {
  reportRefId: string;
  lockVersion: number;
  existingTestRefIds: string[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function AddTestModal({
  reportRefId,
  lockVersion,
  existingTestRefIds,
  open,
  onOpenChange,
}: AddTestModalProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedTest, setSelectedTest] = useState<OrganizationTestResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    data: assignedTestsData,
    isLoading: isTestsLoading,
    isError: isTestsError,
    error: testsError,
  } = useTenantTestsQuery({ status: "ACTIVE", size: 100 });

  const assignedTests = assignedTestsData?.content ?? [];

  // Filter out already added tests and search query
  const availableTests = useMemo(() => {
    const existingSet = new Set(existingTestRefIds);
    return assignedTests.filter((item) => {
      if (existingSet.has(item.testRefId)) return false;
      if (!searchTerm.trim()) return true;
      const q = searchTerm.toLowerCase();
      return (
        item.testName.toLowerCase().includes(q) ||
        item.testCode.toLowerCase().includes(q)
      );
    });
  }, [assignedTests, existingTestRefIds, searchTerm]);

  const addTestMutation = useAddReportTestMutation();

  const handleAddTest = async () => {
    if (!selectedTest) return;
    setErrorMessage(null);

    try {
      await addTestMutation.mutateAsync({
        reportRefId,
        request: {
          testRefId: selectedTest.testRefId,
          lockVersion,
        },
      });
      setSelectedTest(null);
      setSearchTerm("");
      onOpenChange(false);
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to add test to report. Check optimistic lock status.";
      setErrorMessage(msg);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-purple-50 text-purple-600">
              <FlaskConical className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Add Diagnostic Test
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Select an active test from your laboratory's assigned test catalog.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-3 py-1">
          {errorMessage && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription className="text-xs">{errorMessage}</AlertDescription>
            </Alert>
          )}

          {/* Search Input */}
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
            <Input
              type="search"
              placeholder="Search assigned tests by code or name..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8 text-xs h-8"
              autoFocus
            />
          </div>

          {/* Tests List */}
          <div className="rounded-lg border border-slate-200 max-h-60 overflow-y-auto divide-y divide-slate-100 bg-white">
            {isTestsLoading ? (
              <div className="p-3 space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : isTestsError ? (
              <div className="p-4 text-center text-xs text-rose-600">
                {testsError?.message || "Failed to retrieve assigned tests."}
              </div>
            ) : availableTests.length === 0 ? (
              <div className="p-6 text-center text-xs text-slate-500">
                {searchTerm
                  ? `No available tests matched "${searchTerm}".`
                  : assignedTests.length > 0
                  ? "All assigned tests have already been added to this report."
                  : "No active tests are currently assigned to your organization."}
              </div>
            ) : (
              availableTests.map((item) => {
                const isSelected = selectedTest?.testRefId === item.testRefId;
                return (
                  <button
                    key={item.testRefId}
                    type="button"
                    onClick={() => setSelectedTest(item)}
                    className={`w-full text-left p-2.5 transition-colors flex items-center justify-between ${
                      isSelected ? "bg-purple-50/80 border-l-4 border-purple-600" : "hover:bg-slate-50"
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-xs text-slate-900 truncate">
                          {item.testName}
                        </span>
                        <span className="font-mono text-[10px] text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">
                          {item.testCode}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        Type: {item.testType || "Standard"}
                      </p>
                    </div>

                    {isSelected && (
                      <div className="h-5 w-5 rounded-full bg-purple-600 text-white flex items-center justify-center shrink-0">
                        <Check className="h-3 w-3" />
                      </div>
                    )}
                  </button>
                );
              })
            )}
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onOpenChange(false)}
            disabled={addTestMutation.isPending}
            className="text-xs"
          >
            Cancel
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handleAddTest}
            disabled={!selectedTest || addTestMutation.isPending}
            className="text-xs bg-purple-600 hover:bg-purple-700 text-white font-medium"
          >
            {addTestMutation.isPending ? (
              <>
                <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                Adding Test...
              </>
            ) : (
              <>
                <Plus className="mr-1.5 h-3.5 w-3.5" />
                Add Test to Report
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
