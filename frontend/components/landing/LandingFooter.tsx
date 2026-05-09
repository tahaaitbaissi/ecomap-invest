"use client";

import Link from "next/link";
import Image from "next/image";
import { Container } from "./ui";
import logo from "@/app/logo.png";

export default function LandingFooter() {
  return (
    <footer className="border-t border-[color:var(--color-border)] bg-[color:var(--color-bg-page)]">
      <Container className="py-10">
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="relative h-7 w-7 overflow-hidden rounded-lg border border-[color:var(--color-border)] bg-[color:rgba(234,240,255,0.03)]">
                <Image src={logo} alt="" className="h-full w-full object-contain p-1" />
              </span>
              <div className="text-sm font-semibold text-[color:var(--color-text-primary)]">EcoMap Invest</div>
            </div>
            <div className="mt-1 text-sm text-[color:var(--color-text-muted)]">
              Predictive geomarketing for location decisions.
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-4 text-sm text-[color:var(--color-text-secondary)]">
            <a className="hover:text-[color:var(--color-text-primary)]" href="#features">
              Features
            </a>
            <a className="hover:text-[color:var(--color-text-primary)]" href="#how">
              How It Works
            </a>
            <a className="hover:text-[color:var(--color-text-primary)]" href="#proof">
              Proof
            </a>
            <Link className="hover:text-[color:var(--color-text-primary)]" href="/login">
              Login
            </Link>
            <Link className="hover:text-[color:var(--color-text-primary)]" href="/signup">
              Get Started
            </Link>
          </div>
        </div>
      </Container>
    </footer>
  );
}

