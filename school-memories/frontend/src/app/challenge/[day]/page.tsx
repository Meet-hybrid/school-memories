'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import { dayLabel, MOODS, renderQuestion } from '@/lib/format';
import MemoryCard from '@/components/MemoryCard';
import LikeButton from '@/components/LikeButton';

function DayPage() {
  const params = useParams<{ day: string }>();
  const day = Number(params.day);
  const router = useRouter();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['challenge-day', day],
    queryFn: () => api.challengeDay(day),
  });

  const [answer, setAnswer] = useState('');
  const [mood, setMood] = useState<string>('');
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  const submitMutation = useMutation({
    mutationFn: async () => {
      const form = new FormData();
      form.append('day', String(day));
      form.append('answer', answer);
      if (mood) form.append('mood', mood);
      if (file) form.append('file', file);
      return api.createMemory(form);
    },
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setAnswer('');
      setMood('');
      setFile(null);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['challenge-day', day] });
      queryClient.invalidateQueries({ queryKey: ['challenge'] });
      queryClient.invalidateQueries({ queryKey: ['feed'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: (body: { answer: string; mood?: string }) => api.updateMemory(data!.memory!.id, body),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setEditing(false);
      queryClient.invalidateQueries({ queryKey: ['challenge-day', day] });
      queryClient.invalidateQueries({ queryKey: ['challenge'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => api.deleteMemory(data!.memory!.id),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['challenge-day', day] });
      queryClient.invalidateQueries({ queryKey: ['challenge'] });
      router.push('/challenge');
    },
  });

  if (isLoading) return <p className="py-20 text-center text-sm text-ink/40">Loading…</p>;
  if (!data) return <p className="py-20 text-center text-sm text-ink/40">Day not found.</p>;

  const memory = data.memory;
  const question = renderQuestion(data.question, user?.schoolName);

  return (
    <div className="mx-auto max-w-3xl px-5 py-14">
      <Link href="/challenge" className="text-sm text-ink/50 hover:text-ink">
        ← All 30 days
      </Link>

      <p className="label mt-10 mb-3">Day {dayLabel(day)}</p>
      <h1 className="font-serif text-4xl font-medium leading-tight sm:text-5xl">{question}</h1>
      {data.hint && <p className="mt-4 max-w-xl text-sm italic leading-6 text-ink/50">{data.hint}</p>}

      {memory ? (
        <div className="mt-12">
          <div className="mb-6 flex items-center justify-between">
            <p className="text-sm text-moss">You&apos;ve answered this day.</p>
            <div className="flex gap-4 text-sm">
              <button onClick={() => setEditing((v) => !v)} className="text-ink/55 hover:text-ink">
                {editing ? 'Cancel' : 'Edit'}
              </button>
              <button
                onClick={() => {
                  if (window.confirm('Delete this memory? This cannot be undone.')) deleteMutation.mutate();
                }}
                className="text-ink/40 hover:text-clay"
              >
                Delete
              </button>
            </div>
          </div>

          {editing ? (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (answer.trim()) updateMutation.mutate({ answer, mood: mood || memory.mood || undefined });
              }}
              className="flex flex-col gap-4"
            >
              <textarea value={answer} onChange={(e) => setAnswer(e.target.value)} rows={5} className="input resize-none" />
              <div className="flex items-center justify-between">
                <select value={mood} onChange={(e) => setMood(e.target.value)} className="input !w-40">
                  <option value="">Keep mood</option>
                  {MOODS.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
                <button type="submit" className="btn-primary !py-2 text-xs" disabled={!answer.trim()}>
                  Save changes
                </button>
              </div>
            </form>
          ) : (
            <MemoryCard memory={memory} schoolName={user?.schoolName} />
          )}
        </div>
      ) : data.active ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (answer.trim()) submitMutation.mutate();
          }}
          className="mt-12 flex flex-col gap-5"
        >
          <label className="flex flex-col gap-2">
            <span className="label">Your memory</span>
            <textarea
              value={answer}
              onChange={(e) => setAnswer(e.target.value)}
              rows={6}
              className="input resize-none !text-base"
              placeholder="Write it the way you'd tell it at a reunion…"
              maxLength={5000}
            />
          </label>

          <div className="grid gap-5 sm:grid-cols-2">
            <label className="flex flex-col gap-2">
              <span className="label">Photo / video (optional)</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,video/mp4,video/webm"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                className="input file:mr-3 file:border-0 file:bg-paper-deep file:px-3 file:py-1 file:text-sm"
              />
            </label>
            <label className="flex flex-col gap-2">
              <span className="label">How does it feel? (optional)</span>
              <select value={mood} onChange={(e) => setMood(e.target.value)} className="input">
                <option value="">No mood</option>
                {MOODS.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {error && <p className="text-sm text-clay">{error}</p>}

          <div className="flex items-center justify-between">
            <p className="text-xs text-ink/40">You can edit or delete this after saving.</p>
            <button type="submit" disabled={!answer.trim() || submitMutation.isPending} className="btn-primary">
              {submitMutation.isPending ? 'Saving…' : 'Save to the archive'}
            </button>
          </div>
        </form>
      ) : (
        <p className="mt-12 border border-line bg-paper-deep/60 p-6 text-sm text-ink/55">
          This question is no longer open for answers.
        </p>
      )}
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <DayPage />
    </RequireAuth>
  );
}
