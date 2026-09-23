import { useState, useMemo } from "react";
import {
  AlertCircle,
  Check,
  CheckSquare,
  FlaskConical,
  Loader2,
  Search,
  Square,
  Trash2,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { useTenantTestsQuery } from "@/modules/org-admin/tests/hooks/useTenantTests";

interface Step3SelectTestsProps {
  initialSelectedTestRefIds?: string[];
  alreadyAddedTests?: {
    refId: string;
    testRefId: string;
    testName: string;
    testCode: string;
  }[];
  onRemoveExistingTest?: (reportTestRefId: string) => Promise<void>;
  onConfirm: (selectedTestRefIds: string[]) => Promise<void>;
  onBack: () => void;
  isLoading?: boolean;
}

export function Step3SelectTests({
  initialSelectedTestRefIds = [],
  alreadyAddedTests = [],
  onRemoveExistingTest,
  onConfirm,
  onBack,
  isLoading = false,
}: Step3SelectTestsProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedTypeFilter, setSelectedTypeFilter] = useState<string>("ALL");
  const [selectedTestRefIds, setSelectedTestRefIds] = useState<string[]>(
    initialSelectedTestRefIds
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    data: assignedTestsData,
    isLoading: isTestsLoading,
    isError: isTestsError,
    error: testsError,
  } = useTenantTestsQuery({ status: "ACTIVE", size: 100 });

  const assignedTests = useMemo(
    () => assignedTestsData?.content ?? [],
    [assignedTestsData?.content]
  );

  // Extract unique test types for filter pills
  const testTypes = useMemo(() => {
    const types = new Set<string>();
    assignedTests.forEach((t) => {
      if (t.testType) types.add(t.testType);
    });
    return ["ALL", ...Array.from(types)];
  }, [assignedTests]);

  // Set of tests already saved in draft
  const alreadyAddedSet = useMemo(() => {
    return new Set(alreadyAddedTests.map((t) => t.testRefId));
  }, [alreadyAddedTests]);

  // Filtered assigned tests
  const filteredTests = useMemo(() => {
    return assignedTests.filter((item) => {
      if (
        selectedTypeFilter !== "ALL" &&
        item.testType !== selectedTypeFilter
      ) {
        return false;
      }
      if (!searchTerm.trim()) return true;
      const q = searchTerm.toLowerCase();
      return (
        item.testName.toLowerCase().includes(q) ||
        item.testCode.toLowerCase().includes(q)
      );
    });
  }, [assignedTests, selectedTypeFilter, searchTerm]);

  const toggleTest = (testRefId: string) => {
    if (alreadyAddedSet.has(testRefId)) return;

    setSelectedTestRefIds((prev) =>
      prev.includes(testRefId)
        ? prev.filter((id) => id !== testRefId)
        : [...prev, testRefId]
    );
  };

  const handleSelectAllFiltered = () => {
    const newIds = new Set(selectedTestRefIds);
    filteredTests.forEach((t) => {
      if (!alreadyAddedSet.has(t.testRefId)) {
        newIds.add(t.testRefId);
      }
    });
    setSelectedTestRefIds(Array.from(newIds));
  };

  const handleClearSelection = () => {
    setSelectedTestRefIds([]);
  };

  const totalEffectiveSelected =
    selectedTestRefIds.length + alreadyAddedTests.length;

  // Find objects for the selected tests to display chips in sidebar
  const selectedTestObjects = useMemo(() => {
    const map = new Map<string, string>();
    assignedTests.forEach((t) => map.set(t.testRefId, t.testName));
    return selectedTestRefIds.map((id) => ({
      testRefId: id,
      testName: map.get(id) || "Diagnostic Test",
    }));
  }, [selectedTestRefIds, assignedTests]);

  const handleSubmit = async () => {
    if (totalEffectiveSelected === 0) {
      setErrorMessage("Please select at least one diagnostic test to proceed.");
      return;
    }
    setErrorMessage(null);
    try {
      await onConfirm(selectedTestRefIds);
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Failed to add selected tests.";
      setErrorMessage(msg);
    }
  };

  return (
    <div className="space-y-4">
      {errorMessage && (
        <Alert variant="destructive" className="py-2.5 text-xs">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Selection Error</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      )}

      {/* 2-Column Responsive Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* ======================================================== */}
        {/* LEFT COLUMN: Test Catalog & Search (8 cols)              */}
        {/* ======================================================== */}
        <div className="lg:col-span-8 space-y-4">
          <div>
            <h2 className="text-xl font-bold text-slate-900 tracking-tight">
              Step 3: Select Diagnostic Tests
            </h2>
            <p className="text-sm text-slate-500 mt-1">
              Choose active clinical tests from your laboratory catalog to include in this report.
            </p>
          </div>

          {/* Search Bar & Quick Actions */}
          <div className="space-y-2.5">
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5">
              <div className="relative flex-1">
                <Search className="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
                <Input
                  type="text"
                  placeholder="Search tests by name or code (e.g. CBC, Lipid, Haemoglobin)..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10 pr-8 h-11 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                />
                {searchTerm && (
                  <button
                    type="button"
                    onClick={() => setSearchTerm("")}
                    className="absolute right-3.5 top-3.5 text-slate-400 hover:text-slate-600 cursor-pointer"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleSelectAllFiltered}
                  className="text-xs h-11 border-slate-300 text-slate-700"
                >
                  Select All Shown
                </Button>
                {selectedTestRefIds.length > 0 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleClearSelection}
                    className="text-xs h-11 text-slate-500 hover:text-rose-600"
                  >
                    Clear
                  </Button>
                )}
              </div>
            </div>

            {/* Category Pills */}
            {testTypes.length > 1 && (
              <div className="flex flex-wrap gap-1.5 pt-1">
                {testTypes.map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setSelectedTypeFilter(type)}
                    className={`text-xs px-3 py-1.5 rounded-full font-medium transition-colors cursor-pointer ${
                      selectedTypeFilter === type
                        ? "bg-[#0F766E] text-white shadow-2xs"
                        : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                    }`}
                  >
                    {type === "ALL" ? "All Tests" : type}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Test Catalog List */}
          <div className="rounded-xl border border-slate-200/90 bg-white divide-y divide-slate-100 max-h-[480px] overflow-y-auto shadow-xs">
            {isTestsLoading ? (
              <div className="p-4 space-y-3">
                <Skeleton className="h-14 w-full" />
                <Skeleton className="h-14 w-full" />
                <Skeleton className="h-14 w-full" />
              </div>
            ) : isTestsError ? (
              <div className="p-6 text-center text-xs text-rose-600">
                {testsError?.message || "Failed to load assigned laboratory tests."}
              </div>
            ) : filteredTests.length === 0 ? (
              <div className="p-8 text-center">
                <FlaskConical className="h-8 w-8 text-slate-300 mx-auto mb-2" />
                <p className="text-sm font-semibold text-slate-700">
                  {searchTerm
                    ? `No tests matched "${searchTerm}"`
                    : "No active tests available in your catalog."}
                </p>
                <p className="text-xs text-slate-500 mt-1">
                  Contact your laboratory administrator to assign diagnostic test packages.
                </p>
              </div>
            ) : (
              filteredTests.map((item) => {
                const isAlreadyAdded = alreadyAddedSet.has(item.testRefId);
                const isSelected =
                  selectedTestRefIds.includes(item.testRefId) || isAlreadyAdded;

                return (
                  <div
                    key={item.testRefId}
                    onClick={() => !isAlreadyAdded && toggleTest(item.testRefId)}
                    className={`p-3.5 sm:p-4 transition-colors flex items-center justify-between gap-3 ${
                      isAlreadyAdded
                        ? "bg-purple-50/40 cursor-default"
                        : isSelected
                        ? "bg-teal-50/50 cursor-pointer"
                        : "hover:bg-slate-50 cursor-pointer"
                    }`}
                  >
                    <div className="flex items-start gap-3 min-w-0 flex-1">
                      <div className="pt-0.5 text-[#0F766E]">
                        {isAlreadyAdded ? (
                          <Check className="h-5 w-5 text-purple-600" />
                        ) : isSelected ? (
                          <CheckSquare className="h-5 w-5 text-[#0F766E]" />
                        ) : (
                          <Square className="h-5 w-5 text-slate-300" />
                        )}
                      </div>

                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-bold text-sm text-slate-900">
                            {item.testName}
                          </span>
                          <span className="font-mono text-xs text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded font-semibold border border-slate-200">
                            {item.testCode}
                          </span>
                          {isAlreadyAdded && (
                            <Badge className="bg-purple-100 text-purple-700 border-none text-[10px] font-semibold">
                              Saved in Report
                            </Badge>
                          )}
                        </div>
                        <div className="mt-1 text-xs text-slate-500 capitalize">
                          {item.testType || "Standard Panel"}
                        </div>
                      </div>
                    </div>

                    <Badge
                      variant="outline"
                      className={`text-[10px] shrink-0 font-medium ${
                        isAlreadyAdded
                          ? "bg-purple-50 text-purple-700 border-purple-200"
                          : isSelected
                          ? "bg-[#E6F4EA] text-[#0F766E] border-teal-200"
                          : "text-slate-400 border-slate-200"
                      }`}
                    >
                      {isAlreadyAdded
                        ? "In Report"
                        : isSelected
                        ? "Selected"
                        : "Click to select"}
                    </Badge>
                  </div>
                );
              })
            )}
          </div>

          <div className="flex items-center justify-between pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onBack}
              className="text-xs border-slate-300 text-slate-700"
            >
              ← Back to Doctor
            </Button>
          </div>
        </div>

        {/* ======================================================== */}
        {/* RIGHT COLUMN: Sticky Selected Tests Tray (4 cols)        */}
        {/* ======================================================== */}
        <div className="lg:col-span-4 space-y-4">
          <Card className="border-slate-200/80 bg-white shadow-xs sticky top-4">
            <CardContent className="p-5 space-y-4">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-xl bg-[#E6F4EA] text-[#0F766E] flex items-center justify-center shrink-0">
                  <FlaskConical className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">
                    Selected Tests
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    {totalEffectiveSelected} test{totalEffectiveSelected === 1 ? "" : "s"} chosen
                  </p>
                </div>
              </div>

              {/* Already Added Tests in Report Draft */}
              {alreadyAddedTests.length > 0 && (
                <div className="space-y-1.5 pt-1">
                  <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                    In Current Draft ({alreadyAddedTests.length})
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {alreadyAddedTests.map((t) => (
                      <div
                        key={t.refId}
                        className="bg-purple-50 text-purple-800 border border-purple-200/80 rounded-md px-2 py-1 text-xs flex items-center gap-1.5"
                      >
                        <span className="font-semibold">{t.testName}</span>
                        {onRemoveExistingTest && (
                          <button
                            type="button"
                            onClick={() => onRemoveExistingTest(t.refId)}
                            className="text-purple-400 hover:text-rose-600 transition-colors ml-0.5 p-0.5 cursor-pointer"
                            title="Remove test"
                          >
                            <Trash2 className="h-3 w-3" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Newly Selected Tests */}
              {selectedTestObjects.length > 0 ? (
                <div className="space-y-1.5 pt-1">
                  <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                    Newly Selected ({selectedTestObjects.length})
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {selectedTestObjects.map((t) => (
                      <div
                        key={t.testRefId}
                        className="bg-[#E6F4EA] text-[#0F766E] border border-teal-200/80 rounded-md px-2 py-1 text-xs flex items-center gap-1.5"
                      >
                        <span className="font-semibold">{t.testName}</span>
                        <button
                          type="button"
                          onClick={() => toggleTest(t.testRefId)}
                          className="text-teal-600 hover:text-rose-600 transition-colors ml-0.5 p-0.5 cursor-pointer"
                          title="Unselect"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              ) : alreadyAddedTests.length === 0 ? (
                <div className="py-4 text-center text-xs text-slate-400 border border-dashed border-slate-200 rounded-lg">
                  No tests selected yet. Choose tests from the list on the left.
                </div>
              ) : null}

              {/* Checklist Info */}
              <div className="rounded-lg bg-slate-50 border border-slate-100 p-3 space-y-1.5 text-xs text-slate-600">
                <div className="flex items-center gap-2">
                  <span className="text-[#0F766E] font-bold">✓</span>
                  Multi-parameter automated calculation
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-[#0F766E] font-bold">✓</span>
                  Reference ranges auto-applied by age & gender
                </div>
              </div>

              {/* CTA Continue to Results */}
              <Button
                type="button"
                onClick={handleSubmit}
                disabled={totalEffectiveSelected === 0 || isLoading}
                className="w-full h-10 text-xs font-bold bg-[#0F766E] hover:bg-[#115E59] text-white disabled:bg-slate-200 disabled:text-slate-400 transition-all shadow-xs"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" />
                    Setting Up Report...
                  </>
                ) : (
                  <>
                    Continue to Enter Results ({totalEffectiveSelected}) →
                  </>
                )}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
