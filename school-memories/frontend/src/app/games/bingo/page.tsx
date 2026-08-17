'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth } from '@/lib/auth';
import type { BingoCardDto, ClaimResult } from '@/lib/types';

function Bingo() {
  const queryClient = useQueryClient();
  const { data: card, isLoading, isError } = useQuery({ queryKey: ['bingo'], queryFn: api.bingoCard });
  const [error, setError] = useState<string | null>(null);
  const [lastClaim, setLastClaim] = useState<ClaimResult | null>(null);

  const claimMutation = useMutation({
    mutationFn: (rule: string) => api.claimBingo(rule),
    onSuccess: (res) => {
      setError(null);
      setLastClaim(res);
      queryClient.setQueryData<BingoCardDto>(['bingo'], (old) => {
        if (!old) return old;
        const alreadyDone = old.cells.find((c) => c.rule === res.rule)?.done ?? false;
        return {
          ...old,
          completedCount: res.done && !alreadyDone ? old.completedCount + 1 : old.completedCount,
          bingo: old.bingo || res.bingo,
          cells: old.cells.map((c) =>
            c.rule === res.rule ? { ...c, done: res.done, matched: res.matched ?? c.matched } : c,
          ),
        };
      });
    },
    onError: (e: Error) => setError(e.message),
  });

  const regenerateMutation = useMutation({
    mutationFn: api.regenerateBingo,
    onSuccess: (fresh) => {
      queryClient.setQueryData(['bingo'], fresh);
      setLastClaim(null);
      setError(null);
    },
  });

  return (
    <div className="mx-auto max-w-3xl px-5 py-16">
      <div className="flex items-baseline justify-between gap-4">
        <div>
          <p className="label mb-3">Classmate Bingo</p>
          <h1 className="font-serif text-4xl font-medium">Find five in a row</h1>
        </div>
        <button onClick={() => regenerateMutation.mutate()} disabled={regenerateMutation.isPending} className="btn-quiet !py-2 text-xs">
          New card
        </button>
      </div>
      <p className="mt-3 text-sm leading-6 text-ink/55">
        Every square is a classmate to find. Tap one and we&apos;ll check it against the archive — no guessing, no
        cheating, just remembering.
      </p>

      {isLoading && <p className="mt-10 text-sm text-ink/40">Dealing your card…</p>}
      {isError && <p className="mt-10 text-sm text-clay">Could not load your card.</p>}

      {card && (
        <>
          {card.bingo && (
            <div className="mt-8 border border-ink bg-ink px-6 py-5 text-paper">
              <p className="font-serif text-2xl font-medium">BINGO! 🎉</p>
              <p className="mt-1 text-sm text-paper/70">Five in a row — the archive owes you one.</p>
            </div>
          )}

          {lastClaim && lastClaim.done && (
            <div className="mt-6 border border-line bg-surface px-5 py-4 text-sm">
              {lastClaim.matched ? (
                <>
                  <span className="font-medium">Matched with {lastClaim.matched.nickname || lastClaim.matched.name}.</span>{' '}
                  <Link href={`/u/${lastClaim.matched.username || lastClaim.matched.userId}`} className="underline decoration-line underline-offset-4 hover:text-ink">
                    Say hello
                  </Link>
                </>
              ) : (
                <span className="font-medium">That one&apos;s done. ✓</span>
              )}
            </div>
          )}
          {error && <p className="mt-6 text-sm text-clay">{error}</p>}

          <div className="mt-8 grid grid-cols-5 gap-px overflow-hidden border border-line bg-line">
            {card.cells.map((cell, i) => (
              <button
                key={`${cell.rule}-${i}`}
                onClick={() => !cell.done && claimMutation.mutate(cell.rule)}
                disabled={cell.done || claimMutation.isPending}
                className={`flex min-h-24 flex-col items-center justify-center gap-1 p-2 text-center transition-colors ${
                  cell.done ? 'bg-ink text-paper' : 'bg-surface hover:bg-paper-deep'
                }`}
              >
                <span className={`text-[0.6rem] font-semibold uppercase tracking-widest ${cell.done ? 'text-paper/60' : 'text-ink/35'}`}>
                  {cell.done ? '✓' : `${Math.floor(i / 5) + 1}-${(i % 5) + 1}`}
                </span>
                <span className="text-[0.68rem] leading-tight">{cell.prompt}</span>
                {cell.matched && <span className="text-[0.6rem] underline">{cell.matched.nickname || cell.matched.name}</span>}
              </button>
            ))}
          </div>

          <p className="mt-4 text-xs text-ink/40">
            {card.completedCount} of 25 squares marked. Cards are checked against real data — refreshing never hurts.
          </p>
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

export default function BingoPage() {
  return (
    <RequireAuth>
      <Bingo />
    </RequireAuth>
  );
}
