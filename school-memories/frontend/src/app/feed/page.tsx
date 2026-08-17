'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import MemoryCard from '@/components/MemoryCard';
import EmptyState from '@/components/EmptyState';
import { timeAgo } from '@/lib/format';

function FeedPage() {
  const { user } = useAuth();
  const { data, isLoading } = useQuery({
    queryKey: ['feed'],
    queryFn: () => api.feed({ page: 0, size: 20 }),
  });
  const { data: announcements } = useQuery({
    queryKey: ['announcements'],
    queryFn: api.announcements,
  });

  const memories = data?.content ?? [];

  return (
    <div className="mx-auto max-w-2xl px-5 py-14">
      <p className="label mb-3">The archive</p>
      <h1 className="font-serif text-4xl font-medium sm:text-5xl">Latest memories</h1>
      <p className="mt-3 text-base text-ink/55">Fresh stories from your classmates, as they&apos;re told.</p>

      {/* announcements */}
      {announcements && announcements.length > 0 && (
        <div className="mt-10 flex flex-col gap-4">
          {announcements.slice(0, 2).map((a) => (
            <div key={a.id} className="border-l-2 border-clay bg-paper-deep/50 px-5 py-4">
              <div className="flex items-baseline justify-between gap-4">
                <p className="font-serif text-lg font-medium">{a.title}</p>
                <span className="shrink-0 text-xs text-ink/40">{timeAgo(a.createdAt)}</span>
              </div>
              <p className="mt-1 text-sm leading-6 text-ink/60">{a.body}</p>
            </div>
          ))}
        </div>
      )}

      <div className="mt-10">
        {isLoading && <p className="py-10 text-sm text-ink/40">Loading memories…</p>}

        {!isLoading && memories.length === 0 && (
          <EmptyState
            title="The archive is empty — for now"
            body="Be the first to share a memory. Your classmates are probably waiting for someone to start."
            action={
              <Link href="/challenge" className="btn-primary mt-2">
                Answer Day 1
              </Link>
            }
          />
        )}

        {memories.map((m) => (
          <MemoryCard key={m.id} memory={m} schoolName={user?.schoolName} />
        ))}
      </div>
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <FeedPage />
    </RequireAuth>
  );
}
