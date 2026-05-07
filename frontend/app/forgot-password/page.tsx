"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import Card from "@/components/ui/Card";
import Input from "@/components/ui/Input";
import PageShell from "@/components/ui/PageShell";
import { Button } from "@/components/ui/Button";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSent(true);
  };

  return (
    <main className="min-h-[100svh] py-10">
      <PageShell className="grid place-items-center">
        <Card className="w-full max-w-md p-7 md:p-8">
          <h1 className="text-3xl font-black tracking-tight">Reset password</h1>
          <p className="mt-1 text-[color:var(--color-text-secondary)]">Enter your email and we will send reset instructions.</p>

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

          {sent ? <p className="text-sm text-emerald-400">If this email exists, reset instructions have been sent.</p> : null}

          <Button type="submit" variant="primary" className="w-full">
            Send instructions
          </Button>
        </form>

        <p className="mt-5 text-sm text-[color:var(--color-text-secondary)]">
          Back to{" "}
          <Link className="font-semibold text-[color:var(--color-text-primary)] hover:opacity-90" href="/login">
            Sign in
          </Link>
        </p>
        </Card>
      </PageShell>
    </main>
  );
}
