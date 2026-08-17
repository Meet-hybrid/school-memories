'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth } from '@/lib/auth';
import { dayLabel, renderQuestion } from '@/lib/format';
import type { GuessWhoResult } from '@/lib/types';
import Avatar from '@/components/Avatar';

function GuessWho() {
  const { data: round, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['guess-who'],
    queryFn: api.guessWhoRound,
  });
  const [result, setResult] = useState<GuessWhoResult | null>(null);
  const [busy, setBusy] = useState(false);

  async function choose(userId: number) {
    if (!round) return;
    setBusy(true);
    try {
      const res = await api.guessWho(round.memoryId, userId);
      setResult(res);
    } finally {
      setBusy(false);
    }
  }

  function next() {
    setResult(null);
    refetch();
  }

  return (
    <div className="mx-auto max-w-2xl px-5 py-16">
      <p className="label mb-3">Guess Who</p>
      <h1 className="font-serif text-4xl font-medium">Who wrote this?</h1>

      {isLoading && <p className="mt-10 text-sm text-ink/40">Finding a memory…</p>}
      {isError && (
        <div className="mt-10">
          <p className="text-sm text-clay">{error instanceof Error ? error.message : 'Could not load a round.'}</p>
          <button onClick={() => refetch()} className="btn-quiet mt-4">
            Try again
          </button>
        </div>
      )}

      {round && (
        <>
          {result ? (
            <div className="mt-10 border border-line bg-surface p-6">
              <p className="font-serif text-2xl font-medium">
                {result.correct ? 'Got it!' : `It was ${result.correctName}.`}
              </p>
              <p className="mt-2 text-sm text-ink/55">
                {result.correct
                  ? `You've now guessed ${result.guessWhoCorrect} right.`
                  : `You've still guessed ${result.guessWhoCorrect} right in total.`}
              </p>
              <button onClick={next} disabled={isFetching} className="btn-primary mt-5">
                {isFetching ? 'Loading…' : 'Next round'}
              </button>
            </div>
          ) : (
            <>
              <div className="mt-10">
                <p className="mb-3 flex items-baseline gap-2">
                  <span className="font-serif text-sm font-semibold text-clay">Day {dayLabel(round.dayNumber)}</span>
                  {round.question && (
                    <span className="text-sm italic text-ink/55">{renderQuestion(round.question)}</span>
                  )}
                </p>
                <blockquote className="prose-memory border-l-2 border-ink/20 pl-4">{round.answer}</blockquote>
              </div>

              <div className="mt-10 grid gap-3 sm:grid-cols-2">
                {round.options.map((option) => (
                  <button
                    key={option.userId}
                    onClick={() => choose(option.userId)}
                    disabled={busy}
                    className="flex items-center gap-3 border border-line bg-surface px-4 py-3 text-left transition-colors hover:border-ink/40 disabled:opacity-50"
                  >
                    <Avatar name={option.name} src={option.avatarUrl} size={36} />
                    <span className="text-sm font-medium">{option.nickname || option.name}</span>
                  </button>
                ))}
              </div>
            </>
          )}
        </>
      )}

      <p className="mt-10 text-sm text-ink/55">
        <Link href="/games" className="underline decoration-line underline-offset-4 hover:text-ink">
          Back to the games room
        </Link>
      </p>
    </div>
  );
}

export default function GuessWhoPage() {
  return (
    <RequireAuth>
      <GuessWho />
    </RequireAuth>
  );
}
