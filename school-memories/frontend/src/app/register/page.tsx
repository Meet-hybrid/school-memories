'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { School } from '@/lib/types';

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();
  const [form, setForm] = useState({
    fullName: '',
    nickname: '',
    email: '',
    password: '',
    schoolId: '',
    classSetId: '',
    graduationYear: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const { data: schools } = useQuery({ queryKey: ['schools'], queryFn: api.schools });
  const { data: sets } = useQuery({
    queryKey: ['sets', form.schoolId],
    queryFn: () => api.sets(Number(form.schoolId)),
    enabled: !!form.schoolId,
  });

  const set = (k: string, v: string) => setForm((f) => ({ ...f, [k]: v }));

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await register({
        fullName: form.fullName,
        nickname: form.nickname || null,
        email: form.email,
        password: form.password,
        schoolId: Number(form.schoolId),
        classSetId: form.classSetId ? Number(form.classSetId) : null,
        graduationYear: form.graduationYear ? Number(form.graduationYear) : null,
      });
      router.replace('/challenge');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-5 py-16">
      <p className="label mb-3">The challenge starts here</p>
      <h1 className="font-serif text-4xl font-medium">Join your school</h1>
      <p className="mt-3 text-sm leading-6 text-ink/55">
        Use your real name — the whole point is that your classmates find you.
      </p>

      <form onSubmit={onSubmit} className="mt-10 flex flex-col gap-5">
        <div className="grid grid-cols-2 gap-4">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Full name</span>
            <input required value={form.fullName} onChange={(e) => set('fullName', e.target.value)} className="input" placeholder="Ada Obi" />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Nickname (optional)</span>
            <input value={form.nickname} onChange={(e) => set('nickname', e.target.value)} className="input" placeholder="Ada" />
          </label>
        </div>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">Email</span>
          <input type="email" required value={form.email} onChange={(e) => set('email', e.target.value)} className="input" placeholder="you@example.com" autoComplete="email" />
        </label>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">Password</span>
          <input type="password" required minLength={8} value={form.password} onChange={(e) => set('password', e.target.value)} className="input" placeholder="At least 8 characters" autoComplete="new-password" />
        </label>

        <div className="grid grid-cols-2 gap-4">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">School</span>
            <select required value={form.schoolId} onChange={(e) => set('schoolId', e.target.value)} className="input">
              <option value="">Select…</option>
              {(schools ?? []).map((s: School) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Set / Class</span>
            <select value={form.classSetId} onChange={(e) => set('classSetId', e.target.value)} className="input">
              <option value="">Select…</option>
              {(sets ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">Graduation year (optional)</span>
          <input type="number" min={1950} max={2030} value={form.graduationYear} onChange={(e) => set('graduationYear', e.target.value)} className="input" placeholder="2019" />
        </label>

        {error && <p className="text-sm text-clay">{error}</p>}

        <button type="submit" disabled={busy} className="btn-primary mt-2">
          {busy ? 'Creating your account…' : 'Start the challenge'}
        </button>
      </form>

      <p className="mt-8 text-sm text-ink/55">
        Already joined?{' '}
        <Link href="/login" className="font-medium text-ink underline decoration-line underline-offset-4 hover:decoration-ink">
          Sign in
        </Link>
      </p>
    </div>
  );
}
