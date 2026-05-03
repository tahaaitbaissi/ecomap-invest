import type { NextRequest } from "next/server";

/**
 * Server-side proxy to Spring Boot. Replaces next.config rewrites (Next 16 standalone
 * often returns 500 when proxying compressed upstream responses).
 * Set at runtime: BACKEND_PROXY_URL=http://backend:8080 (Docker) or http://127.0.0.1:8080 (local).
 */
function backendOrigin(): string {
  return (
    process.env.BACKEND_PROXY_URL?.trim() ||
    process.env.INTERNAL_API_URL?.trim() ||
    "http://127.0.0.1:8080"
  );
}

async function proxy(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  const { path: segments } = await ctx.params;
  const rest = segments?.length ? segments.join("/") : "";
  const targetUrl = `${backendOrigin()}/api/${rest}${req.nextUrl.search}`;

  const headers = new Headers();
  req.headers.forEach((value, key) => {
    const low = key.toLowerCase();
    if (low === "host" || low === "connection") return;
    // Avoid broken gzip/br pass-through in Node fetch → Spring (common 500 cause)
    if (low === "accept-encoding") {
      headers.set("accept-encoding", "identity");
      return;
    }
    headers.set(key, value);
  });

  const method = req.method;
  const init: RequestInit = {
    method,
    headers,
    redirect: "manual",
  };

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const buf = await req.arrayBuffer();
    if (buf.byteLength > 0) {
      init.body = buf;
    }
  }

  let upstream: Response;
  try {
    upstream = await fetch(targetUrl, init);
  } catch (e) {
    console.error("[api proxy] fetch failed", targetUrl, e);
    return new Response(JSON.stringify({ error: "proxy_upstream_unreachable", message: String(e) }), {
      status: 502,
      headers: { "Content-Type": "application/json" },
    });
  }

  const out = new Headers();
  upstream.headers.forEach((v, k) => {
    if (k.toLowerCase() === "transfer-encoding") return;
    out.set(k, v);
  });

  return new Response(upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: out,
  });
}

export async function GET(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  return proxy(req, ctx);
}

export async function POST(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  return proxy(req, ctx);
}

export async function PUT(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  return proxy(req, ctx);
}

export async function PATCH(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  return proxy(req, ctx);
}

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  return proxy(req, ctx);
}

export async function OPTIONS(req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  return proxy(req, ctx);
}
