"use client";

import Link from "next/link";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost";
type Size = "sm" | "md";

const variantClass: Record<Variant, string> = {
  primary: "ds-btn ds-btn-primary",
  secondary: "ds-btn ds-btn-secondary",
  ghost: "ds-btn ds-btn-ghost",
};

const sizeClass: Record<Size, string> = {
  sm: "px-3 py-2 text-xs",
  md: "",
};

export function Button({
  variant = "secondary",
  size = "md",
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
}) {
  return (
    <button
      {...props}
      className={cn(variantClass[variant], sizeClass[size], className)}
    />
  );
}

export function ButtonLink({
  href,
  variant = "secondary",
  size = "md",
  className,
  children,
}: {
  href: string;
  variant?: Variant;
  size?: Size;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <Link href={href} className={cn(variantClass[variant], sizeClass[size], className)}>
      {children}
    </Link>
  );
}

