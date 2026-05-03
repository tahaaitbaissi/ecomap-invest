import type { NextConfig } from "next";

/**
 * API traffic is proxied by `app/api/[...path]/route.ts` (reliable in standalone).
 * Do not use next.config rewrites to external URLs — Next 16 often returns 500 for those.
 */
const nextConfig: NextConfig = {
  output: "standalone",
};

export default nextConfig;
