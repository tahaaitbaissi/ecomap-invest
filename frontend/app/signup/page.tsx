"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import Card from "@/components/ui/Card";
import Input from "@/components/ui/Input";
import PageShell from "@/components/ui/PageShell";
import { Button } from "@/components/ui/Button";
import { register } from "@/lib/api";
import { getToken, setToken } from "@/lib/auth";

export default function SignupPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [username, setUsername] = useState("");
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
      const res = await register({
        email,
        password,
        companyName: name.trim() || username.trim() || "EcoMap account",
      });
      setToken(res.token);
      let target = "/dashboard";
      if (typeof window !== "undefined") {
        const raw = new URLSearchParams(window.location.search).get("from");
        if (raw) {
          try {
            const decoded = decodeURIComponent(raw);
            if (decoded.startsWith("/")) target = decoded;
          } catch {
            /* ignore */
          }
        }
      }
      router.replace(target);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-[100svh] py-10">
      <PageShell className="grid place-items-center">
        <Card className="w-full max-w-lg p-7 md:p-8">
          <h1 className="text-3xl font-black tracking-tight">Create your account</h1>
          <p className="mt-1 text-[color:var(--color-text-secondary)]">Join EcoMap Invest and start exploring.</p>

          <form onSubmit={onSubmit} className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="md:col-span-2">
            <label className="text-sm font-semibold text-[color:var(--color-text-secondary)]">Full name</label>
            <Input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="mt-1"
              placeholder="Jane Doe"
            />
          </div>

          <div>
            <label className="text-sm font-semibold text-[color:var(--color-text-secondary)]">Username</label>
            <Input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="mt-1"
              placeholder="jane.d"
            />
          </div>

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

          <div className="md:col-span-2">
            <label className="text-sm font-semibold text-[color:var(--color-text-secondary)]">Password</label>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="mt-1"
              placeholder="Create a strong password"
            />
          </div>

          {error ? <p className="md:col-span-2 text-sm text-red-400">{error}</p> : null}

          <Button type="submit" disabled={loading} variant="primary" className="md:col-span-2 w-full">
            {loading ? "Creating account..." : "Create account"}
          </Button>
        </form>

        <p className="mt-5 text-sm text-[color:var(--color-text-secondary)]">
          Already have an account?{" "}
          <Link className="font-semibold text-[color:var(--color-text-primary)] hover:opacity-90" href="/login">
            Sign in
          </Link>
        </p>
        </Card>
      </PageShell>
    </main>
  );
}
