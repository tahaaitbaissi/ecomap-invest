"use client";

import type { ReactNode } from "react";

export default function MapHUD({
  topRight,
  topLeft,
  bottomLeft,
  children,
}: {
  topRight?: ReactNode;
  topLeft?: ReactNode;
  bottomLeft?: ReactNode;
  children?: ReactNode;
}) {
  return (
    <div className="pointer-events-none absolute inset-0 z-[var(--app-shell-z-map-overlay)]">
      {topLeft ? <div className="pointer-events-none absolute left-4 top-4">{topLeft}</div> : null}
      {topRight ? <div className="pointer-events-none absolute right-4 top-4">{topRight}</div> : null}
      {bottomLeft ? <div className="pointer-events-none absolute bottom-4 left-4">{bottomLeft}</div> : null}
      {children}
    </div>
  );
}

