"use client";

import { Activity, Brain, Hexagon, Layers, Radar, SlidersHorizontal } from "lucide-react";
import { Container, Card, Section } from "./ui";
import { MotionDiv } from "./motion";

const features = [
  {
    title: "Interactive Geospatial Map",
    description: "Explore urban zones through an interactive map interface.",
    icon: Layers,
    tone: "cyan",
  },
  {
    title: "H3 Opportunity Scoring",
    description: "Hexagonal scoring model highlights high-potential and saturated areas.",
    icon: Hexagon,
    tone: "emerald",
  },
  {
    title: "Competition Intelligence",
    description: "Analyze nearby competitors and commercial density.",
    icon: Radar,
    tone: "amber",
  },
  {
    title: "What-If Simulation",
    description: "Simulate opening a café, gym, pharmacy, etc. and evaluate opportunity.",
    icon: SlidersHorizontal,
    tone: "cyan",
  },
  {
    title: "AI Explanations",
    description: "Receive contextual explanations for zone scores.",
    icon: Brain,
    tone: "emerald",
  },
  {
    title: "Analytics Dashboard",
    description: "Zone statistics, trends, and business insights.",
    icon: Activity,
    tone: "cyan",
  },
] as const;

export default function FeaturesGrid() {
  return (
    <Section id="features">
      <Container>
        <MotionDiv
          initial={{ opacity: 0, y: 10 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.5, ease: "easeOut" }}
        >
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-balance text-3xl font-black tracking-tight text-white md:text-4xl">
              Built for decision-grade location intelligence
            </h2>
            <p className="mt-3 text-pretty text-slate-300">
              A focused toolchain for exploring zones, quantifying opportunity, and explaining why
              a location scores the way it does.
            </p>
          </div>

          <div className="mt-10 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => {
              const Icon = f.icon;
              const iconTone =
                f.tone === "emerald"
                  ? "text-[color:var(--color-accent)]"
                  : f.tone === "amber"
                    ? "text-[color:rgba(234,179,8,0.95)]"
                    : "text-[color:var(--color-accent-2)]";
              return (
                <Card
                  key={f.title}
                  className={[
                    "p-6 transition",
                    "hover:-translate-y-0.5 hover:bg-[color:rgba(234,240,255,0.03)]",
                  ].join(" ")}
                >
                  <div className="flex items-start gap-3">
                    <div className="rounded-xl border border-[color:var(--color-border)] bg-transparent p-2">
                      <Icon className={["h-5 w-5", iconTone].join(" ")} />
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-white">{f.title}</div>
                      <div className="mt-1 text-sm text-[color:var(--color-text-secondary)]">
                        {f.description}
                      </div>
                    </div>
                  </div>
                </Card>
              );
            })}
          </div>
        </MotionDiv>
      </Container>
    </Section>
  );
}

