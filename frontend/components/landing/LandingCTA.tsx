"use client";

import { ArrowRight } from "lucide-react";
import { Container, Card, PrimaryLinkButton, SecondaryLinkButton, Section } from "./ui";
import { MotionDiv } from "./motion";

export default function LandingCTA() {
  return (
    <Section className="pb-20 md:pb-28">
      <Container>
        <MotionDiv
          initial={{ opacity: 0, y: 10 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-120px" }}
          transition={{ duration: 0.5, ease: "easeOut" }}
        >
          <div className="relative">
            <Card className="relative p-8 md:p-10">
              <div className="mx-auto max-w-2xl text-center">
                <h3 className="text-balance text-3xl font-black tracking-tight text-white md:text-4xl">
                  Start exploring smarter investment zones
                </h3>
                <p className="mt-3 text-pretty text-[color:var(--color-text-secondary)]">
                  Create an account and jump into the map, scoring, simulations, and AI explanations.
                </p>
                <div className="mt-6 flex flex-col justify-center gap-3 sm:flex-row">
                  <PrimaryLinkButton href="/signup">
                    Create Account <ArrowRight className="ml-2 h-4 w-4" />
                  </PrimaryLinkButton>
                  <SecondaryLinkButton href="/login">Login</SecondaryLinkButton>
                </div>
              </div>
            </Card>
          </div>
        </MotionDiv>
      </Container>
    </Section>
  );
}

