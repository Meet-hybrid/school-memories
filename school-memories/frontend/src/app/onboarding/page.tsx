'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import type { School } from '@/lib/types';

/** One-time setup for accounts created via Google: pick a school (and optionally set). */
export default function OnboardingPage() {
  return (
    <RequireAuth>
      <OnboardingForm />
    </RequireAuth>
  );
}

function OnboardingForm() {
  const { user, refresh } = useAuth();
  const router = useRouter();
  const [schoolId, setSchoolId] = useState('');
  const [classSetId, setClassSetId] = useState('');
  const [graduationYear, setGraduationYear] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Already part of a school? Nothing to do here.
  useEffect(() => {
    if (user && user.schoolId) router.replace('/challenge');
  }, [user, router]);

  const { data: schools } = useQuery({ queryKey: ['schools'], queryFn: api.schools });
  const { data: sets } = useQuery({
    queryKey: ['sets', schoolId],
    queryFn: () => api.sets(Number(schoolId)),
    enabled: !!schoolId,
  });

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!schoolId) {
      setError('Choose your school to continue');
      return;
    }
    setError(null);
    setBusy(true);
    try {
      await api.updateProfile({
        schoolId: Number(schoolId),
        classSetId: classSetId ? Number(classSetId) : null,
        graduationYear: graduationYear ? Number(graduationYear) : null,
      });
      await refresh();
      router.replace('/challenge');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Saving failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-5 py-16">
      <p className="label mb-3">Almost there</p>
      <h1 className="font-serif text-4xl font-medium">Which school were you part of?</h1>
      <p className="mt-3 text-sm leading-6 text-ink/55">
        Your school connects you with your classmates. We'll use it to build your feed and suggest people to follow.
      </p>

      <form onSubmit={onSubmit} className="mt-10 flex flex-col gap-5">
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">School</span>
          <select required value={schoolId} onChange={(e) => { setSchoolId(e.target.value); setClassSetId(''); }} className="input">
            <option value="">Select…</option>
            {(schools ?? []).map((s: School) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>

        <div className="grid grid-cols-2 gap-4">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Set / Class</span>
            <select value={classSetId} onChange={(e) => setClassSetId(e.target.value)} className="input">
              <option value="">Select…</option>
              {(sets ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Graduation year</span>
            <input type="number" min={1950} max={2030} value={graduationYear} onChange={(e) => setGraduationYear(e.target.value)} className="input" placeholder="2019" />
          </label>
        </div>

        {error && <p className="text-sm text-clay">{error}</p>}

        <button type="submit" disabled={busy} className="btn-primary mt-2">
          {busy ? 'Saving…' : 'Continue to the challenge'}
        </button>
      </form>

      <p className="mt-6 text-sm text-ink/55">
        <Link href="/challenge" className="underline decoration-line underline-offset-4 hover:text-ink">
          Skip for now
        </Link>{' '}
        — you can look around first.
      </p>
    </div>
  );
}
