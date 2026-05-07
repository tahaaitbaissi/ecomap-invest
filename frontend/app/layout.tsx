import type { Metadata } from "next";
import { Fraunces, Plus_Jakarta_Sans } from "next/font/google";
import "leaflet/dist/leaflet.css";
import "./globals.css";
import ThemeScript from "@/components/theme/ThemeScript";

export const metadata: Metadata = {
  title: "EcoMap Invest",
  description:
    "Predictive geomarketing platform: explore zones, score opportunity, simulate scenarios, and get explainable insights.",
};

const heading = Fraunces({
  subsets: ["latin"],
  variable: "--font-heading",
  weight: ["600", "700", "800", "900"],
});

const body = Plus_Jakarta_Sans({
  subsets: ["latin"],
  variable: "--font-body",
  weight: ["400", "500", "600", "700"],
});

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={[heading.variable, body.variable, "antialiased"].join(" ")}>
        <ThemeScript />
        {children}
      </body>
    </html>
  );
}
