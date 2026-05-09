"use client";

import Link from "next/link";
import Image from "next/image";
import { useEffect, useMemo, useState } from "react";
import { Menu, X } from "lucide-react";
import { getToken } from "@/lib/auth";
import { cn } from "@/lib/cn";
import ThemeToggle from "@/components/theme/ThemeToggle";
import logo from "@/app/logo.png";

const links = [
  { label: "Features", href: "#features" },
  { label: "How It Works", href: "#how" },
  { label: "Tech Stack", href: "#stack" },
] as const;

export default function LandingNavbar() {
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const isAuthed = useMemo(() => Boolean(getToken()), []);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 4);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  return (
    <div
      className={cn(
        "sticky top-0 z-50 border-b",
        scrolled
          ? "border-[color:var(--color-border)] bg-[color:color-mix(in_srgb,var(--color-bg-page)_75%,transparent)] backdrop-blur"
          : "border-transparent bg-transparent",
      )}
    >
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
        <Link href="/" className="flex items-center gap-2">
          <span className="relative h-8 w-8 overflow-hidden rounded-xl border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)]">
            <Image src={logo} alt="" className="h-full w-full object-contain p-1" priority />
          </span>
          <span className="text-sm font-semibold tracking-wide text-[color:var(--color-text-primary)]">
            EcoMap Invest
          </span>
        </Link>

        <nav className="hidden items-center gap-6 text-sm text-[color:var(--color-text-secondary)] md:flex">
          {links.map((l) => (
            <a key={l.href} href={l.href} className="hover:text-[color:var(--color-text-primary)]">
              {l.label}
            </a>
          ))}
        </nav>

        <div className="hidden items-center gap-2 md:flex">
          <ThemeToggle />
          {isAuthed ? (
            <Link
              href="/dashboard"
              className="ds-btn ds-btn-secondary"
            >
              Open Dashboard
            </Link>
          ) : null}
          <Link
            href="/login"
            className="ds-btn ds-btn-secondary"
          >
            Login
          </Link>
          <Link
            href="/signup"
            className="ds-btn ds-btn-primary"
          >
            Get Started
          </Link>
        </div>

        <button
          type="button"
          className="inline-flex items-center justify-center rounded-xl border border-[color:var(--color-border)] bg-transparent p-2 text-[color:var(--color-text-primary)] md:hidden"
          aria-label="Open menu"
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {open ? (
        <div className="border-t border-[color:var(--color-border)] bg-[color:color-mix(in_srgb,var(--color-bg-page)_92%,transparent)] backdrop-blur md:hidden">
          <div className="mx-auto max-w-6xl px-4 py-4">
            <div className="flex flex-col gap-2">
              <div className="pb-2">
                <ThemeToggle />
              </div>
              {links.map((l) => (
                <a
                  key={l.href}
                  href={l.href}
                  className="rounded-lg px-3 py-2 text-sm text-[color:var(--color-text-secondary)] hover:bg-[color:rgba(234,240,255,0.04)] hover:text-[color:var(--color-text-primary)]"
                  onClick={() => setOpen(false)}
                >
                  {l.label}
                </a>
              ))}
              <div className="mt-2 grid grid-cols-1 gap-2">
                {isAuthed ? (
                  <Link
                    href="/dashboard"
                    className="ds-btn ds-btn-secondary"
                    onClick={() => setOpen(false)}
                  >
                    Open Dashboard
                  </Link>
                ) : null}
                <Link
                  href="/login"
                  className="ds-btn ds-btn-secondary"
                  onClick={() => setOpen(false)}
                >
                  Login
                </Link>
                <Link
                  href="/signup"
                  className="ds-btn ds-btn-primary"
                  onClick={() => setOpen(false)}
                >
                  Get Started
                </Link>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

