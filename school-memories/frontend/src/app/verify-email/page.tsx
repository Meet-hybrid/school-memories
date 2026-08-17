'use client';

import { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { api } from '@/lib/api';

function Verify() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [status, setStatus] = useState<'verifying' | 'ok' | 'error'>('verifying');
  const [message, setMessage] = useState('');

  useEffect(() => {
    api
      .verifyEmail(token)
      .then(() => setStatus('ok'))
      .catch((e: Error) => {
        setStatus('error');
        setMessage(e.message);
      });
  }, [token]);

  return (
    <div className="mt-10 border border-line bg-paper-deep/60 p-6">
      {status === 'verifying' && <p className="text-sm text-ink/55">Verifying your email…</p>}
      {status === 'ok' && (
        <>
          <p className="font-serif text-xl">Email verified</p>
          <p className="mt-2 text-sm text-ink/55">Your account is confirmed. Welcome to the archive.</p>
          <Link href="/challenge" className="btn-primary mt-6">
            Continue to the challenge
          </Link>
        </>
      )}
      {status === 'error' && (
        <>
          <p className="font-serif text-xl text-clay">Couldn&apos;t verify</p>
          <p className="mt-2 text-sm text-ink/55">{message || 'This link is invalid or has expired.'}</p>
          <Link href="/" className="btn-outline mt-6">
            Back home
          </Link>
        </>
      )}
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-5 py-16">
      <p className="label mb-3">Email verification</p>
      <h1 className="font-serif text-4xl font-medium">Almost there</h1>
      <Suspense fallback={null}>
        <Verify />
      </Suspense>
    </div>
  );
}
