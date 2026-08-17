'use client';

import { useState } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.forgotPassword(email);
      setSent(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-5 py-16">
      <p className="label mb-3">Password reset</p>
      <h1 className="font-serif text-4xl font-medium">Forgot your password?</h1>
      <p className="mt-3 text-sm leading-6 text-ink/55">
        Enter your email and we&apos;ll send you a reset link. In this preview the link is printed to the backend
        console.
      </p>

      {sent ? (
        <div className="mt-10 border border-line bg-paper-deep/60 p-6">
          <p className="font-serif text-xl">Check your email</p>
          <p className="mt-2 text-sm leading-6 text-ink/55">
            If an account exists for <span className="font-medium text-ink">{email}</span>, a reset link is on its way.
          </p>
          <Link href="/login" className="btn-outline mt-6">
            Back to sign in
          </Link>
        </div>
      ) : (
        <form onSubmit={onSubmit} className="mt-10 flex flex-col gap-5">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Email</span>
            <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} className="input" placeholder="you@example.com" />
          </label>
          {error && <p className="text-sm text-clay">{error}</p>}
          <button type="submit" disabled={busy} className="btn-primary mt-2">
            {busy ? 'Sending…' : 'Send reset link'}
          </button>
        </form>
      )}

      <Link href="/login" className="mt-8 text-sm text-ink/55 hover:text-ink">
        ← Back to sign in
      </Link>
    </div>
  );
}
