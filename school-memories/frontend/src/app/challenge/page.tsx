'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import { dayLabel, renderQuestion } from '@/lib/format';

function ChallengePage() {
  const { user } = useAuth();
  const { data, isLoading } = useQuery({ queryKey: ['challenge'], queryFn: api.challenge });

  const answered = data?.answeredCount ?? 0;
  const total = data?.total ?? 30;
  const pct = total ? Math.round((answered / total) * 100) : 0;

  return (
    <div className="mx-auto max-w-3xl px-5 py-14">
      <p className="label mb-3">The 30-day challenge</p>
      <h1 className="font-serif text-4xl font-medium sm:text-5xl">Your school, thirty days.</h1>
      <p className="mt-4 max-w-xl text-base leading-7 text-ink/55">
        Answer when it suits you — days don&apos;t expire, and there&apos;s no wrong time to remember.
      </p>

      {/* progress */}
      <div className="mt-10 border border-line bg-surface p-6">
        <div className="flex items-end justify-between">
          <div>
            <p className="font-serif text-3xl font-medium">
              {answered}
              <span className="text-lg text-ink/40"> / {total} memories</span>
            </p>
            <p className="mt-1 text-sm text-ink/50">
              {answered === 0
                ? 'Your archive is empty. Start with Day 1.'
                : answered === total
                  ? 'Challenge complete. The archive is yours.'
                  : 'Keep going — every answer makes the archive richer.'}
            </p>
          </div>
          {data && data.streak > 1 && <p className="label">{data.streak}-day streak</p>}
        </div>
        <div className="mt-4 h-1 w-full overflow-hidden rounded-full bg-line">
          <div className="h-full bg-clay transition-all duration-700" style={{ width: `${pct}%` }} />
        </div>
      </div>

      {/* timeline */}
      <ol className="mt-12 border-t border-line">
        {isLoading && <p className="py-10 text-sm text-ink/40">Loading the questions…</p>}
        {data?.days.map((day) => {
          const question = renderQuestion(day.question, user?.schoolName);
          return (
            <li key={day.dayNumber} className="group border-b border-line">
              <Link
                href={`/challenge/${day.dayNumber}`}
                className="flex items-baseline gap-5 py-5 transition-colors hover:bg-paper-deep/50"
              >
                <span
                  className={`font-serif text-lg font-semibold ${
                    day.answered ? 'text-clay' : day.active ? 'text-ink/30' : 'text-ink/20'
                  }`}
                >
                  {dayLabel(day.dayNumber)}
                </span>
                <span className="flex-1">
                  <span className={`block font-serif text-lg leading-snug ${day.answered ? 'text-ink' : 'text-ink/60'}`}>
                    {question}
                  </span>
                  {day.answerSnippet && (
                    <span className="mt-1 block truncate text-sm italic text-ink/45">“{day.answerSnippet}”</span>
                  )}
                </span>
                <span className="shrink-0 text-xs">
                  {day.answered ? (
                    <span className="font-medium text-moss">Answered</span>
                  ) : day.active ? (
                    <span className="text-ink/40 transition-colors group-hover:text-ink">Answer →</span>
                  ) : (
                    <span className="text-ink/30">Closed</span>
                  )}
                </span>
              </Link>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <ChallengePage />
    </RequireAuth>
  );
}
