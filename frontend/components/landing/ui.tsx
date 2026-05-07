import Link from "next/link";
import { cn } from "@/lib/cn";

export function Container({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return <div className={cn("mx-auto w-full max-w-6xl px-4", className)}>{children}</div>;
}

export function Section({
  children,
  id,
  className,
}: {
  children: React.ReactNode;
  id?: string;
  className?: string;
}) {
  return (
    <section id={id} className={cn("py-16 md:py-24", className)}>
      {children}
    </section>
  );
}

export function Card({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "ds-card",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function Badge({
  children,
  tone = "neutral",
}: {
  children: React.ReactNode;
  tone?: "neutral" | "emerald" | "cyan" | "amber" | "red";
}) {
  const toneClass =
    tone === "emerald"
      ? "text-[color:var(--color-text-secondary)] border-[color:var(--color-border)] bg-[color:rgba(47,107,255,0.10)]"
      : tone === "cyan"
        ? "text-[color:var(--color-text-secondary)] border-[color:var(--color-border)] bg-[color:rgba(51,211,255,0.08)]"
        : tone === "amber"
          ? "text-[color:var(--color-text-secondary)] border-[color:var(--color-border)] bg-[color:rgba(234,179,8,0.06)]"
          : tone === "red"
            ? "text-[color:var(--color-text-secondary)] border-[color:var(--color-border)] bg-[color:rgba(239,68,68,0.06)]"
            : "text-[color:var(--color-text-secondary)] border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)]";
  return (
    <span className={cn("inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs", toneClass)}>
      {children}
    </span>
  );
}

export function PrimaryLinkButton({
  href,
  children,
  className,
}: {
  href: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "ds-btn ds-btn-primary",
        className,
      )}
    >
      {children}
    </Link>
  );
}

export function SecondaryLinkButton({
  href,
  children,
  className,
}: {
  href: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "ds-btn ds-btn-secondary",
        className,
      )}
    >
      {children}
    </Link>
  );
}

export function GhostLinkButton({
  href,
  children,
  className,
}: {
  href: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <Link href={href} className={cn("ds-btn ds-btn-ghost", className)}>
      {children}
    </Link>
  );
}

