"use client";

import { Container, Section, Card } from "./ui";
import { MotionDiv } from "./motion";

const stats = [
  { label: "Cells scored", value: "1.8k", note: "H3 res-9 grid (Casablanca)" },
  { label: "POIs indexed", value: "4k+", note: "Viewport analytics ready" },
  { label: "Profiles", value: "Dynamic", note: "Drivers vs competitors tags" },
  { label: "Simulations", value: "Instant", note: "What-If deltas & comparisons" },
] as const;

export default function Credibility() {
  return (
    <Section id="proof" className="border-y border-[color:var(--color-border)]">
      <Container>
        <MotionDiv
          initial={{ opacity: 0, y: 10 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.5, ease: "easeOut" }}
        >
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-balance text-3xl font-black tracking-tight text-[color:var(--color-text-primary)] md:text-4xl">
              Built for credibility
            </h2>
            <p className="mt-3 text-pretty text-[color:var(--color-text-secondary)]">
              EcoMap Invest combines geospatial scoring, simulation, and explainable insights in a
              focused workflow—so decisions can be defended, not guessed.
            </p>
          </div>

          <div className="mt-10 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {stats.map((s) => (
              <Card key={s.label} className="p-6">
                <div className="text-xs uppercase tracking-wide text-[color:var(--color-text-muted)]">
                  {s.label}
                </div>
                <div className="mt-3 text-3xl font-black text-[color:var(--color-text-primary)]">{s.value}</div>
                <div className="mt-2 text-sm text-[color:var(--color-text-secondary)]">{s.note}</div>
              </Card>
            ))}
          </div>

          <div className="mt-10 grid grid-cols-2 gap-3 md:grid-cols-6">
            {["Urban analytics", "Opportunity scoring", "Competition", "Simulation", "XAI", "Dashboards"].map(
              (t) => (
                <div
                  key={t}
                  className="rounded-xl border border-[color:var(--color-border)] bg-transparent px-4 py-3 text-center text-sm font-semibold text-[color:var(--color-text-secondary)]"
                >
                  {t}
                </div>
              ),
            )}
          </div>
        </MotionDiv>
      </Container>
    </Section>
  );
}

