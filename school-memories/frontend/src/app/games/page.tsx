'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';

const GAMES = [
  {
    href: '/games/guess-who',
    name: 'Guess Who',
    blurb: 'A memory, four classmates. Match it to the person who wrote it.',
    tag: 'memories',
  },
  {
    href: '/games/trivia',
    name: 'School Trivia',
    blurb: 'How well do you really know your school? Four options, one right answer.',
    tag: 'quiz',
  },
  {
    href: '/games/bingo',
    name: 'Classmate Bingo',
    blurb: 'A 5×5 card of classmates to find. Mark five in a row for bingo.',
    tag: 'explore',
  },
] as const;

function GamesHub() {
  const { user } = useAuth();
  const { data: score } = useQuery({ queryKey: ['game-score'], queryFn: api.gameScore, enabled: !!user });
  const { data: leaderboard } = useQuery({ queryKey: ['game-leaderboard'], queryFn: api.gameLeaderboard });

  return (
    <div className="mx-auto max-w-3xl px-5 py-16">
      <p className="label mb-3">The games room</p>
      <h1 className="font-serif text-4xl font-medium">Play with your memories</h1>
      <p className="mt-3 text-sm leading-6 text-ink/55">
        Three small games that turn the archive into something to do together. Nothing here is a race —
        it&apos;s all about remembering.
      </p>

      {score && (
        <p className="mt-6 inline-flex items-baseline gap-2 rounded-full border border-line bg-surface px-4 py-2 text-sm">
          Your score: <span className="font-serif text-xl font-semibold">{score.total}</span>
          <span className="text-xs text-ink/45">
            ({score.guessWhoCorrect} guesses · {score.triviaCorrect} trivia · {score.bingosCompleted} bingos)
          </span>
        </p>
      )}

      <div className="mt-10 grid gap-4 sm:grid-cols-3">
        {GAMES.map((g) => (
          <Link
            key={g.href}
            href={g.href}
            className="group flex flex-col gap-3 border border-line bg-surface p-5 transition-colors hover:border-ink/40"
          >
            <span className="label">{g.tag}</span>
            <h2 className="font-serif text-2xl font-medium group-hover:underline">{g.name}</h2>
            <p className="text-sm leading-6 text-ink/55">{g.blurb}</p>
            <span className="mt-auto pt-2 text-sm text-ink/60 group-hover:text-ink">Play →</span>
          </Link>
        ))}
      </div>

      {leaderboard && leaderboard.length > 0 && (
        <section className="mt-14">
          <h2 className="label mb-4">Games leaderboard</h2>
          <ol className="flex flex-col gap-1">
            {leaderboard.map((row, i) => (
              <li key={row.userId} className="flex items-center gap-3 border-b border-line py-2.5 text-sm last:border-b-0">
                <span className="w-6 text-right font-serif text-lg text-ink/40">{i + 1}</span>
                <Link href={`/u/${row.userId}`} className="font-medium hover:underline">
                  {row.name}
                </Link>
                <span className="ml-auto font-serif text-lg font-semibold">{row.total}</span>
              </li>
            ))}
          </ol>
        </section>
      )}
    </div>
  );
}

export default function GamesPage() {
  return (
    <RequireAuth>
      <GamesHub />
    </RequireAuth>
  );
}
