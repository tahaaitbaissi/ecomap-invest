"use client";

import { Container, Card, Section } from "./ui";
import { MotionDiv } from "./motion";
import { ArrowUpRight, FlaskConical } from "lucide-react";

const sims = [
  { name: "Café", score: 82, tone: "emerald" },
  { name: "Gym", score: 65, tone: "amber" },
  { name: "Pharmacy", score: 91, tone: "cyan" },
] as const;

export default function SimulationShowcase() {
  return (
    <Section>
      <Container>
        <MotionDiv
          initial={{ opacity: 0, y: 10 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.5, ease: "easeOut" }}
        >
          <div className="grid grid-cols-1 items-center gap-8 lg:grid-cols-2">
            <div>
              <div className="ds-kicker">
                <FlaskConical className="h-4 w-4" />
                Simulation
              </div>
              <h2 className="mt-4 text-balance text-3xl font-black tracking-tight text-white md:text-4xl">
                Test before you invest
              </h2>
              <p className="mt-3 text-pretty text-[color:var(--color-text-secondary)]">
                Simulate opening different businesses and compare outcomes for the same zone—fast,
                consistent, and explainable.
              </p>
              <div className="mt-6 flex items-center gap-2 text-sm text-[color:var(--color-text-secondary)]">
                <ArrowUpRight className="h-4 w-4 text-[color:var(--color-accent)]" />
                Compare scenarios side-by-side
              </div>
            </div>

            <Card className="p-6">
              <div className="text-sm font-semibold text-white">Scenario comparison</div>
              <div className="mt-4 grid grid-cols-1 gap-3">
                {sims.map((s) => {
                  const bar =
                    s.tone === "emerald"
                      ? "from-emerald-400 to-cyan-400"
                      : s.tone === "amber"
                        ? "from-amber-400 to-red-400"
                        : "from-cyan-400 to-emerald-400";
                  return (
                    <div
                      key={s.name}
                      className="rounded-2xl border border-[color:var(--color-border)] bg-transparent p-4 hover:bg-[color:rgba(234,240,255,0.03)] transition"
                    >
                      <div className="flex items-center justify-between">
                        <div className="text-sm font-semibold text-white">{s.name}</div>
                        <div className="text-sm font-semibold text-white">{s.score}/100</div>
                      </div>
                      <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-[color:rgba(234,240,255,0.05)]">
                        <div
                          className={`h-full rounded-full bg-gradient-to-r ${bar}`}
                          style={{ width: `${s.score}%` }}
                        />
                      </div>
                      <div className="mt-2 text-xs text-[color:var(--color-text-muted)]">
                        Score reflects opportunity vs saturation for this business type.
                      </div>
                    </div>
                  );
                })}
              </div>
            </Card>
          </div>
        </MotionDiv>
      </Container>
    </Section>
  );
}

