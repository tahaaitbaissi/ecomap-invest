"use client";

import LandingNavbar from "@/components/landing/LandingNavbar";
import Hero from "@/components/landing/Hero";
import ValueProp from "@/components/landing/ValueProp";
import FeaturesGrid from "@/components/landing/FeaturesGrid";
import HowItWorks from "@/components/landing/HowItWorks";
import SimulationShowcase from "@/components/landing/SimulationShowcase";
import Credibility from "@/components/landing/Credibility";
import LandingCTA from "@/components/landing/LandingCTA";
import LandingFooter from "@/components/landing/LandingFooter";

export default function Home() {
  return (
    <div className="min-h-screen bg-[color:var(--color-bg-page)] text-white">
      <div className="absolute inset-0 -z-10 opacity-70 [background-image:linear-gradient(rgba(234,240,255,0.05)_1px,transparent_1px),linear-gradient(90deg,rgba(234,240,255,0.05)_1px,transparent_1px)] [background-size:72px_72px]" />
      <header>
        <LandingNavbar />
      </header>
      <main>
        <Hero />
        <ValueProp />
        <FeaturesGrid />
        <HowItWorks />
        <SimulationShowcase />
        <Credibility />
        <LandingCTA />
      </main>
      <LandingFooter />
    </div>
  );
}
