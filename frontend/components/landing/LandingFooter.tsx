"use client";

import Link from "next/link";
import { Container } from "./ui";

export default function LandingFooter() {
  return (
    <footer className="border-t border-[color:var(--color-border)] bg-[color:var(--color-bg-page)]">
      <Container className="py-10">
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div>
            <div className="text-sm font-semibold text-white">EcoMap Invest</div>
            <div className="mt-1 text-sm text-[color:var(--color-text-muted)]">
              Predictive geomarketing for location decisions.
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-4 text-sm text-[color:var(--color-text-secondary)]">
            <a className="hover:text-white" href="#features">
              Features
            </a>
            <a className="hover:text-white" href="#how">
              How It Works
            </a>
            <a className="hover:text-white" href="#proof">
              Proof
            </a>
            <Link className="hover:text-white" href="/login">
              Login
            </Link>
            <Link className="hover:text-white" href="/signup">
              Get Started
            </Link>
          </div>
        </div>
      </Container>
    </footer>
  );
}

