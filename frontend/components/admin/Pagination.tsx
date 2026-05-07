"use client";

export default function Pagination({
  page,
  size,
  totalElements,
  onPage,
}: {
  page: number;
  size: number;
  totalElements: number;
  onPage: (p: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="text-sm text-[color:var(--color-text-secondary)]">
        Page <span className="text-[color:var(--color-text-primary)]">{page + 1}</span> / {totalPages} — Total{" "}
        <span className="text-[color:var(--color-text-primary)]">{totalElements}</span>
      </div>
      <div className="flex gap-2">
        <button
          className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
          onClick={() => onPage(0)}
          disabled={page <= 0}
        >
          First
        </button>
        <button
          className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
          onClick={() => onPage(Math.max(page - 1, 0))}
          disabled={page <= 0}
        >
          Prev
        </button>
        <button
          className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
          onClick={() => onPage(page + 1)}
          disabled={page + 1 >= totalPages}
        >
          Next
        </button>
        <button
          className="ds-btn ds-btn-secondary px-3 py-2 text-sm disabled:opacity-50"
          onClick={() => onPage(totalPages - 1)}
          disabled={page + 1 >= totalPages}
        >
          Last
        </button>
      </div>
    </div>
  );
}

