"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchMe } from "@/services/api/userService";
import { adminBatchStatus, adminTriggerBatch } from "@/services/api/admin/adminBatchService";

export default function AdminBatchPage() {
  const router = useRouter();
  const [guard, setGuard] = useState<"checking" | "ok">("checking");

  const [executionId, setExecutionId] = useState<number | null>(null);
  const [status, setStatus] = useState<any | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pollRef = useRef<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchMe()
      .then((me) => {
        if (cancelled) return;
        if (me.role !== "ROLE_ADMIN") {
          router.replace("/dashboard");
          return;
        }
        setGuard("ok");
      })
      .catch(() => router.replace("/dashboard"));
    return () => {
      cancelled = true;
    };
  }, [router]);

  const canPoll = useMemo(() => executionId != null && executionId > 0, [executionId]);

  async function trigger() {
    setLoading(true);
    setError(null);
    try {
      const r = await adminTriggerBatch();
      setExecutionId(r.executionId);
      setStatus({ status: r.status });
    } catch (e: any) {
      // When app.batch.enabled=false, controller is not registered -> 404.
      const code = e?.response?.status;
      if (code === 404) {
        setError("Batch is disabled (app.batch.enabled=false).");
      } else {
        setError(e?.response?.data ?? e?.message ?? "Trigger failed");
      }
    } finally {
      setLoading(false);
    }
  }

  async function pollOnce() {
    if (!canPoll) return;
    setLoading(true);
    setError(null);
    try {
      const r = await adminBatchStatus(executionId!);
      setStatus(r);
    } catch (e: any) {
      const code = e?.response?.status;
      if (code === 404) {
        setError("Batch endpoint not available (disabled).");
      } else {
        setError(e?.response?.data ?? e?.message ?? "Status check failed");
      }
    } finally {
      setLoading(false);
    }
  }

  function startPolling() {
    if (pollRef.current != null) window.clearInterval(pollRef.current);
    pollRef.current = window.setInterval(() => {
      pollOnce();
    }, 2500);
  }

  function stopPolling() {
    if (pollRef.current != null) window.clearInterval(pollRef.current);
    pollRef.current = null;
  }

  useEffect(() => {
    return () => {
      stopPolling();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (guard !== "ok") {
    return (
      <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-6">
        <div className="text-sm text-[color:var(--color-text-secondary)]">Checking access…</div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
        <div className="text-lg font-extrabold">Batch jobs</div>
        <div className="mt-2 text-sm text-[color:var(--color-text-secondary)]">
          Uses backend endpoint{" "}
          <span className="font-semibold text-[color:var(--color-text-primary)]">/api/v1/admin/batch</span>{" "}
          (only available when{" "}
          <span className="font-semibold text-[color:var(--color-text-primary)]">app.batch.enabled=true</span>).
        </div>
        {error ? <div className="mt-3 text-sm text-red-300">{error}</div> : null}
      </div>

      <div className="rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] p-4">
        <div className="flex flex-wrap items-center gap-2">
          <button
            className="ds-btn ds-btn-primary px-3 py-2 text-sm disabled:opacity-50"
            onClick={trigger}
            disabled={loading}
          >
            Trigger OSM import
          </button>
          <button
            className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
            onClick={pollOnce}
            disabled={loading || !canPoll}
          >
            Poll status
          </button>
          <button
            className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
            onClick={startPolling}
            disabled={!canPoll}
          >
            Start auto-poll
          </button>
          <button
            className="ds-btn ds-btn-secondary px-3 py-2 text-sm"
            onClick={stopPolling}
          >
            Stop
          </button>
        </div>

        <div className="mt-4 flex flex-col gap-2 text-sm text-[color:var(--color-text-secondary)]">
          <div>
            Execution id: <span className="text-[color:var(--color-text-primary)]">{executionId ?? "-"}</span>
          </div>
          <div className="rounded-lg border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)] p-3 font-mono text-xs text-[color:var(--color-text-primary)]">
            {status ? JSON.stringify(status, null, 2) : "No status yet."}
          </div>
        </div>
      </div>
    </div>
  );
}

