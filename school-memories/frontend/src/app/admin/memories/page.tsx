'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { timeAgo } from '@/lib/format';

function AdminMemories() {
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ['admin-memories'], queryFn: api.adminMemories });

  const moderate = useMutation({
    mutationFn: ({ id, deleted }: { id: number; deleted: boolean }) => api.adminModerateMemory(id, deleted),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-memories'] }),
  });

  return (
    <div>
      <h1 className="font-serif text-3xl font-medium">Memories</h1>
      <p className="mt-2 text-sm text-ink/55">
        Hide anything that doesn&apos;t belong. Hidden memories disappear from the feed and profiles.
      </p>

      <ul className="mt-8 flex flex-col">
        {data?.content.map((m) => (
          <li key={m.id} className={`border-b border-line py-4 ${m.deleted ? 'opacity-50' : ''}`}>
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-xs text-ink/45">
                  {m.authorName} · Day {String(m.dayNumber).padStart(2, '0')} · {timeAgo(m.createdAt)}
                  {m.deleted && <span className="ml-2 font-medium text-clay">hidden</span>}
                </p>
                <p className="mt-1 line-clamp-2 text-sm text-ink/80">{m.answer}</p>
              </div>
              <button
                onClick={() => moderate.mutate({ id: m.id, deleted: !m.deleted })}
                className="shrink-0 text-xs text-ink/45 hover:text-ink"
              >
                {m.deleted ? 'Restore' : 'Hide'}
              </button>
            </div>
          </li>
        ))}
        {data && data.content.length === 0 && (
          <li className="py-10 text-center text-sm text-ink/40">No memories yet.</li>
        )}
      </ul>
    </div>
  );
}

export default function Page() {
  return <AdminMemories />;
}
