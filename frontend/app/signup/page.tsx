"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
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
      router.replace("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen grid place-items-center px-4" style={{ background: "radial-gradient(circle at 80% 10%, #bae6fd 0%, #f0f9ff 35%, #dbeafe 100%)" }}>
      <div className="w-full max-w-lg rounded-3xl shadow-2xl border border-slate-200 bg-white/95 p-8 backdrop-blur">
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Create your account</h1>
        <p className="text-slate-600 mt-1">Join EcoMap Invest and start exploring.</p>

        <form onSubmit={onSubmit} className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className="text-sm font-semibold text-slate-600">Full name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="mt-1 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-900 placeholder:text-slate-400 outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Jane Doe"
            />
          </div>

          <div>
            <label className="text-sm font-semibold text-slate-600">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="mt-1 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-900 placeholder:text-slate-400 outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="jane.d"
            />
          </div>

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

          <div className="md:col-span-2">
            <label className="text-sm font-semibold text-slate-600">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="mt-1 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-900 placeholder:text-slate-400 outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Create a strong password"
            />
          </div>

          {error ? <p className="md:col-span-2 text-sm text-red-600">{error}</p> : null}

          <button
            type="submit"
            disabled={loading}
            className="md:col-span-2 w-full rounded-xl bg-blue-700 hover:bg-blue-800 text-white font-bold py-3 transition disabled:opacity-70"
          >
            {loading ? "Creating account..." : "Create account"}
          </button>
        </form>

        <p className="mt-5 text-sm text-slate-600">
          Already have an account?{" "}
          <Link className="text-blue-700 hover:underline font-medium" href="/login">
            Sign in
          </Link>
        </p>
      </div>
    </main>
  );
}
