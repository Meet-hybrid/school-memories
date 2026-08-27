'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import GoogleSignInButton from '@/components/GoogleSignInButton';

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email, password);
      router.replace('/feed');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign in failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-[60vh] max-w-md flex-col justify-center px-5 py-10 sm:min-h-[70vh] sm:py-16">
      <p className="label mb-3">Welcome back</p>
      <h1 className="font-serif text-4xl font-medium">Sign in</h1>
      <p className="mt-3 text-sm leading-6 text-ink/55">
        Your classmates have been posting. Catch up on what they remember.
      </p>

      <GoogleSignInButton />

      <form onSubmit={onSubmit} className="mt-8 flex flex-col gap-5">
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">Email</span>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="input"
            placeholder="you@example.com"
            autoComplete="email"
          />
        </label>
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">Password</span>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="input"
            placeholder="••••••••"
            autoComplete="current-password"
          />
        </label>

        {error && <p className="text-sm text-clay">{error}</p>}

        <button type="submit" disabled={busy} className="btn-primary mt-2">
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <div className="mt-8 flex flex-col gap-2 text-sm text-ink/55">
        <Link href="/forgot-password" className="hover:text-ink">
          Forgot your password?
        </Link>
        <p>
          New here?{' '}
          <Link href="/register" className="font-medium text-ink underline decoration-line underline-offset-4 hover:decoration-ink">
            Join your school
          </Link>
        </p>
      </div>
    </div>
  );
}
