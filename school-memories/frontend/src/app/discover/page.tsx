'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import Avatar from '@/components/Avatar';
import FollowButton from '@/components/FollowButton';
import EmptyState from '@/components/EmptyState';
import { LEADERBOARD_TYPES } from '@/lib/format';
import type { UserDto } from '@/lib/types';

function ClassmateRow({ u }: { u: UserDto }) {
  const handle = u.username ? `/u/${u.username}` : `/u/${u.id}`;
  return (
    <li className="flex items-center gap-4 border-b border-line py-4 last:border-b-0">
      <Link href={handle} className="flex min-w-0 flex-1 items-center gap-4">
        <Avatar name={u.fullName} src={u.avatarUrl} size={44} />
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{u.nickname || u.fullName}</p>
          <p className="truncate text-xs text-ink/45">
            {[u.school?.name, u.classSet?.name, u.graduationYear ? `Class of ${u.graduationYear}` : null]
              .filter(Boolean)
              .join(' · ')}{' '}
            · {u.memories} memor{u.memories === 1 ? 'y' : 'ies'}
          </p>
        </div>
      </Link>
      <FollowButton userId={u.id} following={u.following} />
    </li>
  );
}

function DiscoverPage() {
  const { user } = useAuth();
  const [q, setQ] = useState('');
  const [leaderboardType, setLeaderboardType] = useState<string>('memories');

  const { data: suggested } = useQuery({ queryKey: ['suggested'], queryFn: api.suggested, enabled: !!user });
  const { data: results } = useQuery({
    queryKey: ['search', q],
    queryFn: () => api.search({ q, schoolId: user?.schoolId ?? undefined, page: 0, size: 20 }),
    enabled: q.trim().length > 0,
  });
  const { data: leaderboard } = useQuery({
    queryKey: ['leaderboard', leaderboardType],
    queryFn: () => api.leaderboard(leaderboardType),
  });

  return (
    <div className="mx-auto max-w-3xl px-5 py-14">
      <p className="label mb-3">Classmates</p>
      <h1 className="font-serif text-4xl font-medium sm:text-5xl">Find your people</h1>
      <p className="mt-3 max-w-xl text-base text-ink/55">
        Search by name, nickname, set or graduation year. Reconnecting is the whole point.
      </p>

      {/* search */}
      <div className="mt-10">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          className="input !py-3 !text-base"
          placeholder="Search classmates… e.g. “chi”, “2019”, “Set of 2020”"
        />
        {q.trim() && (
          <div className="mt-4">
            {results && results.content.length === 0 ? (
              <EmptyState title="No one matches that" body="Try a nickname, a set, or a year." />
            ) : (
              <ul className="border-t border-line">
                {results?.content.map((u) => (
                  <ClassmateRow key={u.id} u={u} />
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      {/* suggested */}
      {!q.trim() && suggested && suggested.length > 0 && (
        <section className="mt-14">
          <h2 className="label mb-4">Suggested classmates</h2>
          <ul className="border-t border-line">
            {suggested.map((u) => (
              <ClassmateRow key={u.id} u={u} />
            ))}
          </ul>
        </section>
      )}

      {/* leaderboard */}
      <section className="mt-16">
        <h2 className="label mb-4">Community</h2>
        <p className="mb-5 max-w-lg text-sm leading-6 text-ink/55">
          A quiet kind of recognition — the people whose memories the school keeps coming back to.
        </p>
        <div className="mb-6 flex flex-wrap gap-2">
          {LEADERBOARD_TYPES.map((t) => (
            <button
              key={t.key}
              onClick={() => setLeaderboardType(t.key)}
              className={`rounded-sm border px-3 py-1.5 text-xs transition-colors ${
                leaderboardType === t.key
                  ? 'border-ink bg-ink text-paper'
                  : 'border-line text-ink/60 hover:border-ink/40'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
        <ol className="border-t border-line">
          {leaderboard?.map((e, i) => (
            <li key={e.userId} className="flex items-center gap-4 border-b border-line py-3.5">
              <span className={`w-7 text-right font-serif text-lg ${i === 0 ? 'font-semibold text-clay' : 'text-ink/40'}`}>
                {i + 1}
              </span>
              <Link href={`/u/${e.userId}`} className="flex min-w-0 flex-1 items-center gap-3">
                <Avatar name={e.name} src={e.avatarUrl} size={32} />
                <span className="truncate text-sm font-medium">{e.name}</span>
              </Link>
              <span className="text-sm text-ink/45">{e.value}</span>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <DiscoverPage />
    </RequireAuth>
  );
}
