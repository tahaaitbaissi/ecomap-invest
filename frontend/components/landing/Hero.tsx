"use client";

import { ArrowRight, Sparkles, MapPinned, Hexagon, SlidersHorizontal } from "lucide-react";
import { Badge, Card, Container, PrimaryLinkButton, SecondaryLinkButton, Section } from "./ui";
import { MotionDiv } from "./motion";

function ProductPreview() {
  return (
    <Card className="overflow-hidden">
      <div className="grid grid-cols-1 md:grid-cols-5">
        {/* Map / heatmap frame */}
        <div className="md:col-span-3 p-4">
          <div className="flex items-center justify-between">
            <div className="text-xs font-semibold text-white">Casablanca</div>
            <div className="text-[11px] text-[color:var(--color-text-muted)]">H3 res-9 • Opportunity heatmap</div>
          </div>

          <div className="mt-4 aspect-[16/11] w-full overflow-hidden rounded-[14px] border border-[color:var(--color-border)] bg-[color:var(--color-bg-page)]">
            {/* Refined static grid */}
            <div className="h-full w-full opacity-[0.28] [background-image:linear-gradient(rgba(234,240,255,0.10)_1px,transparent_1px),linear-gradient(90deg,rgba(234,240,255,0.10)_1px,transparent_1px)] [background-size:44px_44px]" />

            {/* Minimal “heat” layer (no glow blobs) */}
            <div className="pointer-events-none absolute inset-0 opacity-100">
              <div className="absolute left-[18%] top-[24%] h-14 w-16 rounded-[10px] bg-[color:rgba(47,107,255,0.22)]" />
              <div className="absolute left-[38%] top-[36%] h-14 w-16 rounded-[10px] bg-[color:rgba(47,107,255,0.16)]" />
              <div className="absolute left-[56%] top-[28%] h-14 w-16 rounded-[10px] bg-[color:rgba(51,211,255,0.12)]" />
              <div className="absolute left-[60%] top-[52%] h-14 w-16 rounded-[10px] bg-[color:rgba(234,179,8,0.10)]" />
              <div className="absolute left-[42%] top-[62%] h-14 w-16 rounded-[10px] bg-[color:rgba(47,107,255,0.18)]" />
              <div className="absolute left-[22%] top-[58%] h-14 w-16 rounded-[10px] bg-[color:rgba(239,68,68,0.08)]" />
            </div>

            {/* Annotation callouts (data-first, minimal) */}
            <div className="absolute left-3 top-3 rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] px-3 py-2">
              <div className="text-[10px] text-[color:var(--color-text-muted)]">Opportunity score</div>
              <div className="text-sm font-semibold text-white">82/100</div>
            </div>
            <div className="absolute right-3 top-3 rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] px-3 py-2">
              <div className="text-[10px] text-[color:var(--color-text-muted)]">Competition</div>
              <div className="text-sm font-semibold text-white">Low</div>
            </div>
            <div className="absolute bottom-3 right-3 rounded-xl border border-[color:var(--color-border)] bg-[color:var(--color-bg-card)] px-3 py-2">
              <div className="text-[10px] text-[color:var(--color-text-muted)]">Scenario</div>
              <div className="text-sm font-semibold text-white">Café</div>
            </div>
          </div>
        </div>

        {/* Data panel */}
        <div className="md:col-span-2 border-t border-[color:var(--color-border)] md:border-l md:border-t-0 p-4">
          <div className="text-xs font-semibold text-white">Signal breakdown</div>
          <div className="mt-4 grid grid-cols-1 gap-3">
            <div className="rounded-[14px] border border-[color:var(--color-border)] bg-transparent p-4">
              <div className="flex items-center justify-between">
                <div className="text-xs text-[color:var(--color-text-muted)]">Drivers (weighted)</div>
                <div className="text-sm font-semibold text-white">68%</div>
              </div>
              <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-[color:rgba(234,240,255,0.05)]">
                <div className="h-full w-[68%] rounded-full bg-[color:var(--color-accent)]" />
              </div>
            </div>
            <div className="rounded-[14px] border border-[color:var(--color-border)] bg-transparent p-4">
              <div className="flex items-center justify-between">
                <div className="text-xs text-[color:var(--color-text-muted)]">Saturation risk</div>
                <div className="text-sm font-semibold text-white">32%</div>
              </div>
              <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-[color:rgba(234,240,255,0.05)]">
                <div className="h-full w-[32%] rounded-full bg-[color:rgba(234,179,8,0.85)]" />
              </div>
            </div>
            <div className="rounded-[14px] border border-[color:var(--color-border)] bg-transparent p-4">
              <div className="text-xs text-[color:var(--color-text-muted)]">AI explanation</div>
              <div className="mt-2 text-sm text-[color:var(--color-text-secondary)]">
                “High score driven by strong foot traffic indicators and low direct competition within 1km.”
              </div>
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
}

export default function Hero() {
  return (
    <Section className="pt-10 md:pt-16">
      <Container>
        <div className="grid grid-cols-1 items-center gap-10 lg:grid-cols-2">
          <MotionDiv
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, ease: "easeOut" }}
          >
            <span className="ds-kicker">
              <MapPinned className="h-4 w-4" />
              Predictive geomarketing
            </span>
            <h1 className="mt-5 text-balance text-4xl font-black tracking-tight text-white md:text-6xl">
              Make smarter location decisions with predictive geomarketing
            </h1>
            <p className="mt-4 text-pretty text-lg leading-relaxed text-[color:var(--color-text-secondary)]">
              Analyze competition, simulate business scenarios, and identify high-opportunity zones
              using geospatial intelligence.
            </p>

            <div className="mt-6 flex flex-col gap-3 sm:flex-row">
              <PrimaryLinkButton href="/signup">
                Get Started <ArrowRight className="ml-2 h-4 w-4" />
              </PrimaryLinkButton>
              <SecondaryLinkButton href="/map">
                View Demo <ArrowRight className="ml-2 h-4 w-4" />
              </SecondaryLinkButton>
            </div>

            <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <Card className="p-5 hover:bg-[color:rgba(234,240,255,0.03)] transition">
                <div className="flex items-center gap-2 text-sm font-semibold text-white">
                  <Hexagon className="h-4 w-4 text-[color:var(--color-accent-2)]" />
                  H3 scoring
                </div>
                <div className="mt-1 text-xs text-[color:var(--color-text-muted)]">
                  Hexagonal opportunity surfaces with consistent normalization.
                </div>
              </Card>
              <Card className="p-5 hover:bg-[color:rgba(234,240,255,0.03)] transition">
                <div className="flex items-center gap-2 text-sm font-semibold text-white">
                  <SlidersHorizontal className="h-4 w-4 text-[color:var(--color-accent)]" />
                  What-If simulation
                </div>
                <div className="mt-1 text-xs text-[color:var(--color-text-muted)]">
                  Compare scenarios across business types in seconds.
                </div>
              </Card>
              <Card className="p-5 hover:bg-[color:rgba(234,240,255,0.03)] transition">
                <div className="flex items-center gap-2 text-sm font-semibold text-white">
                  <Sparkles className="h-4 w-4 text-[color:var(--color-accent-2)]" />
                  AI explanations
                </div>
                <div className="mt-1 text-xs text-[color:var(--color-text-muted)]">
                  Clear, contextual reasoning behind every score.
                </div>
              </Card>
            </div>
          </MotionDiv>

          <MotionDiv
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut", delay: 0.08 }}
          >
            <ProductPreview />
          </MotionDiv>
        </div>
      </Container>
    </Section>
  );
}

