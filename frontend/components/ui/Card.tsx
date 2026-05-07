import { cn } from "@/lib/cn";

export default function Card({
  children,
  className,
  elevated = false,
}: {
  children: React.ReactNode;
  className?: string;
  elevated?: boolean;
}) {
  return (
    <div className={cn(elevated ? "ds-card-elev" : "ds-card", className)}>{children}</div>
  );
}

