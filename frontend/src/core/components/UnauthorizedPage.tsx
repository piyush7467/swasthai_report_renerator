export function UnauthorizedPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <section className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <div className="mb-4 text-4xl">
          403
        </div>

        <h1 className="text-xl font-semibold text-slate-900">
          Access not available
        </h1>

        <p className="mt-2 text-sm leading-6 text-slate-600">
          Your current account does not have access to this
          section.
        </p>
      </section>
    </main>
  );
}