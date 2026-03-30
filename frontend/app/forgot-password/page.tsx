"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSent(true);
  };

  return (
    <main className="min-h-screen grid place-items-center px-4" style={{ background: "radial-gradient(circle at 50% 0%, #cffafe 0%, #eff6ff 45%, #e2e8f0 100%)" }}>
      <div className="w-full max-w-md rounded-3xl shadow-2xl border border-slate-200 bg-white/95 p-8 backdrop-blur">
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Reset password</h1>
        <p className="text-slate-600 mt-1">Enter your email and we will send reset instructions.</p>

        <form onSubmit={onSubmit} className="mt-6 space-y-4">
          <div>
            <label className="text-sm font-semibold text-slate-600">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="mt-1 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-900 placeholder:text-slate-400 outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="name@example.com"
            />
          </div>

          {sent ? <p className="text-sm text-emerald-600">If this email exists, reset instructions have been sent.</p> : null}

          <button type="submit" className="w-full rounded-xl bg-blue-700 hover:bg-blue-800 text-white font-bold py-3 transition">
            Send instructions
          </button>
        </form>

        <p className="mt-5 text-sm text-slate-600">
          Back to{" "}
          <Link className="text-blue-700 hover:underline font-medium" href="/login">
            Sign in
          </Link>
        </p>
      </div>
    </main>
  );
}
