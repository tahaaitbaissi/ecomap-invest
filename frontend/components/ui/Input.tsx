"use client";

import { cn } from "@/lib/cn";

export default function Input({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={cn(
        "w-full rounded-[12px] border border-[color:var(--color-border)] bg-transparent px-4 py-3",
        "text-sm text-[color:var(--color-text-primary)] placeholder:text-[color:var(--color-text-muted)]",
        "outline-none focus:ring-2 focus:ring-[color:rgba(47,107,255,0.35)]",
        className,
      )}
    />
  );
}

