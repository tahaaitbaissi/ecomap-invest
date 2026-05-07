"use client";

import { Container, Card, Section } from "./ui";
import { MotionDiv } from "./motion";
import { CheckCircle2 } from "lucide-react";

const steps = [
  { title: "Explore Zones", description: "Pan/zoom the map and inspect H3 hex scores." },
  { title: "Select Business Type", description: "Choose a profile (café, gym, pharmacy, ...)." },
  { title: "Run Simulation", description: "Test scenarios and compare outcomes instantly." },
  { title: "Receive Opportunity Score", description: "Get a normalized score plus context and drivers." },
] as const;

export default function HowItWorks() {
  return (
    <Section id="how">
      <Container>
        <MotionDiv
          initial={{ opacity: 0, y: 10 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.5, ease: "easeOut" }}
        >
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-balance text-3xl font-black tracking-tight text-white md:text-4xl">
              How it works
            </h2>
            <p className="mt-3 text-pretty text-slate-300">
              A simple workflow designed for real-world location decisions.
            </p>
          </div>

          <div className="mt-10">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              {steps.map((s, idx) => (
                <div key={s.title} className="relative">
                  <Card className="p-5 hover:bg-[color:rgba(234,240,255,0.03)] transition">
                    <div className="flex items-center gap-2 text-sm font-semibold text-white">
                      <span className="inline-flex h-6 w-6 items-center justify-center rounded-lg border border-[color:var(--color-border)] bg-transparent text-xs text-[color:var(--color-text-secondary)]">
                        {idx + 1}
                      </span>
                      {s.title}
                    </div>
                    <p className="mt-2 text-sm text-[color:var(--color-text-secondary)]">{s.description}</p>
                    <div className="mt-3 flex items-center gap-2 text-xs text-[color:var(--color-text-muted)]">
                      <CheckCircle2 className="h-4 w-4 text-[color:var(--color-accent)]" />
                      Ready for analysis
                    </div>
                  </Card>
                  {idx < steps.length - 1 ? (
                    <div className="hidden md:block absolute -right-2 top-1/2 h-px w-4 bg-white/10" />
                  ) : null}
                </div>
              ))}
            </div>
          </div>
        </MotionDiv>
      </Container>
    </Section>
  );
}

