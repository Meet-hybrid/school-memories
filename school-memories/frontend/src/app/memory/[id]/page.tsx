'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import MemoryCard from '@/components/MemoryCard';
import CommentSection from '@/components/CommentSection';

function MemoryPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const { user } = useAuth();

  const { data: memory, isLoading, error } = useQuery({
    queryKey: ['memory', id],
    queryFn: () => api.memory(id),
  });

  return (
    <div className="mx-auto max-w-2xl px-5 py-14">
      <Link href="/feed" className="text-sm text-ink/50 hover:text-ink">
        ← Back to the feed
      </Link>

      {isLoading && <p className="py-20 text-center text-sm text-ink/40">Loading…</p>}
      {error && <p className="py-20 text-center text-sm text-ink/50">This memory isn&apos;t available.</p>}

      {memory && (
        <div className="mt-8">
          <MemoryCard memory={memory} schoolName={user?.schoolName} />
          <CommentSection memoryId={id} />
        </div>
      )}
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <MemoryPage />
    </RequireAuth>
  );
}
