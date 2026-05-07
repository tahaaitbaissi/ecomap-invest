"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import Card from "@/components/ui/Card";
import Input from "@/components/ui/Input";
import PageShell from "@/components/ui/PageShell";
import { Button } from "@/components/ui/Button";
import { login } from "@/lib/api";
import { getToken, setToken } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (getToken()) {
      router.replace("/dashboard");
    }
  }, [router]);

  const onSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await login(email, password);
      setToken(res.token);
      router.replace("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-[100svh] py-10">
      <PageShell className="grid place-items-center">
        <Card className="w-full max-w-md p-7 md:p-8">
          <h1 className="text-3xl font-black tracking-tight">Welcome back</h1>
          <p className="mt-1 text-[color:var(--color-text-secondary)]">Sign in to continue to EcoMap Invest.</p>

          <form onSubmit={onSubmit} className="mt-6 space-y-4">
          <div>
            <label className="text-sm font-semibold text-[color:var(--color-text-secondary)]">Email</label>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="mt-1"
              placeholder="name@example.com"
            />
          </div>

          <div>
            <label className="text-sm font-semibold text-[color:var(--color-text-secondary)]">Password</label>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="mt-1"
              placeholder="********"
            />
          </div>

          {error ? <p className="text-sm text-red-400">{error}</p> : null}

          <Button type="submit" disabled={loading} variant="primary" className="w-full">
            {loading ? "Signing in..." : "Sign in"}
          </Button>
        </form>

        <div className="mt-5 flex justify-between text-sm">
          <Link className="font-semibold text-[color:var(--color-text-secondary)] hover:text-[color:var(--color-text-primary)]" href="/forgot-password">
            Forgot password?
          </Link>
          <Link className="font-semibold text-[color:var(--color-text-secondary)] hover:text-[color:var(--color-text-primary)]" href="/signup">
            Create account
          </Link>
        </div>
        </Card>
      </PageShell>
    </main>
  );
}
