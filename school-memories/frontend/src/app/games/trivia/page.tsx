'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth } from '@/lib/auth';
import type { TriviaResult } from '@/lib/types';

function shuffle<T>(list: T[]): T[] {
  const copy = [...list];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function Trivia() {
  const { data: round, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['trivia-next'],
    queryFn: api.triviaNext,
  });
  const [result, setResult] = useState<TriviaResult | null>(null);
  const [options, setOptions] = useState<{ text: string; index: number }[] | null>(null);
  const [busy, setBusy] = useState(false);

  // Shuffle the options per question client-side so the server never leaks the answer.
  useEffect(() => {
    if (round) setOptions(shuffle(round.options.map((text, index) => ({ text, index }))));
  }, [round]);

  async function answer(index: number) {
    if (!round) return;
    setBusy(true);
    try {
      const res = await api.answerTrivia(round.questionId, index);
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
      <p className="label mb-3">School Trivia</p>
      <h1 className="font-serif text-4xl font-medium">How well do you know your school?</h1>

      {isLoading && <p className="mt-10 text-sm text-ink/40">Fetching a question…</p>}
      {isError && (
        <div className="mt-10">
          <p className="text-sm text-clay">{error instanceof Error ? error.message : 'Could not load a question.'}</p>
          <button onClick={() => refetch()} className="btn-quiet mt-4">
            Try again
          </button>
        </div>
      )}

      {round && (
        <>
          {result ? (
            <div className="mt-10 border border-line bg-surface p-6">
              <p className="font-serif text-2xl font-medium">{result.correct ? 'Correct!' : 'Not quite.'}</p>
              <p className="mt-2 text-sm text-ink/55">
                {result.correct
                  ? `You've answered ${result.triviaCorrect} right in total.`
                  : `You've still answered ${result.triviaCorrect} right in total.`}
              </p>
              <button onClick={next} disabled={isFetching} className="btn-primary mt-5">
                {isFetching ? 'Loading…' : 'Next question'}
              </button>
            </div>
          ) : (
            <>
              <p className="mt-10 font-serif text-2xl font-medium leading-snug">{round.question}</p>
              <div className="mt-8 grid gap-3">
                {options?.map((option) => (
                  <button
                    key={option.index}
                    onClick={() => answer(option.index)}
                    disabled={busy}
                    className="border border-line bg-surface px-5 py-3.5 text-left text-sm transition-colors hover:border-ink/40 disabled:opacity-50"
                  >
                    {option.text}
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

export default function TriviaPage() {
  return (
    <RequireAuth>
      <Trivia />
    </RequireAuth>
  );
}
