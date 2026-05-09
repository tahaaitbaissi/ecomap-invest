"use client";

import { AlertTriangle, BarChart3, Search, ShieldAlert } from "lucide-react";
import { Container, Card, Section } from "./ui";
import { MotionDiv } from "./motion";

export default function ValueProp() {
  return (
    <Section className="border-y border-[color:var(--color-border)]">
      <Container>
        <MotionDiv
          initial={{ opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.5, ease: "easeOut" }}
        >
          <div className="mx-auto max-w-2xl text-center">
            <div className="inline-flex items-center gap-2 rounded-full border border-amber-500/20 bg-amber-500/10 px-3 py-1 text-xs text-amber-200">
              <AlertTriangle className="h-4 w-4" />
              Risk
            </div>
            <h2 className="mt-4 text-balance text-3xl font-black tracking-tight text-[color:var(--color-text-primary)] md:text-4xl">
              Choosing the wrong location is expensive
            </h2>
            <p className="mt-3 text-pretty text-[color:var(--color-text-secondary)]">
              Location strategy often relies on fragmented data and manual analysis—leading to
              missed opportunities and costly investments.
            </p>
          </div>

          <div className="mt-10 grid grid-cols-1 gap-4 md:grid-cols-3">
            <Card className="p-5 hover:bg-[color:rgba(234,240,255,0.03)] transition">
              <div className="flex items-center gap-2 text-sm font-semibold text-[color:var(--color-text-primary)]">
                <Search className="h-4 w-4 text-[color:var(--color-accent-2)]" />
                Fragmented market data
              </div>
              <p className="mt-2 text-sm text-[color:var(--color-text-secondary)]">
                Relevant signals live across maps, listings, and spreadsheets with inconsistent
                coverage and quality.
              </p>
            </Card>
            <Card className="p-5 hover:bg-[color:rgba(234,240,255,0.03)] transition">
              <div className="flex items-center gap-2 text-sm font-semibold text-[color:var(--color-text-primary)]">
                <BarChart3 className="h-4 w-4 text-[color:var(--color-accent)]" />
                Manual competitor analysis
              </div>
              <p className="mt-2 text-sm text-[color:var(--color-text-secondary)]">
                Understanding saturation by business type takes time—and quickly becomes outdated.
              </p>
            </Card>
            <Card className="p-5 hover:bg-[color:rgba(234,240,255,0.03)] transition">
              <div className="flex items-center gap-2 text-sm font-semibold text-[color:var(--color-text-primary)]">
                <ShieldAlert className="h-4 w-4 text-[color:rgba(239,68,68,0.85)]" />
                Poor investment decisions
              </div>
              <p className="mt-2 text-sm text-[color:var(--color-text-secondary)]">
                Without a unified view, location choices can be driven by bias instead of evidence.
              </p>
            </Card>
          </div>

          <div className="mt-10 rounded-2xl border border-[color:var(--color-border)] bg-transparent p-6 text-center">
            <div className="text-sm font-semibold text-[color:var(--color-text-primary)]">EcoMap centralizes everything.</div>
            <p className="mt-2 text-sm text-[color:var(--color-text-secondary)]">
              A single workspace to explore zones, simulate scenarios, and justify decisions with
              explainable analytics.
            </p>
          </div>
        </MotionDiv>
      </Container>
    </Section>
  );
}

