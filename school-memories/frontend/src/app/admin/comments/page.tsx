'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { timeAgo } from '@/lib/format';

function AdminComments() {
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ['admin-comments'], queryFn: api.adminComments });

  const remove = useMutation({
    mutationFn: api.adminDeleteComment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-comments'] }),
  });

  return (
    <div>
      <h1 className="font-serif text-3xl font-medium">Comments</h1>
      <p className="mt-2 text-sm text-ink/55">Remove inappropriate comments.</p>

      <ul className="mt-8 flex flex-col">
        {data?.content.map((c) => (
          <li key={c.id} className={`border-b border-line py-4 ${c.deleted ? 'opacity-50' : ''}`}>
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-xs text-ink/45">
                  {c.authorName} · on memory #{c.memoryId} · {timeAgo(c.createdAt)}
                  {c.deleted && <span className="ml-2 font-medium text-clay">removed</span>}
                </p>
                <p className="mt-1 line-clamp-2 text-sm text-ink/80">{c.body}</p>
              </div>
              {!c.deleted && (
                <button onClick={() => remove.mutate(c.id)} className="shrink-0 text-xs text-ink/45 hover:text-clay">
                  Remove
                </button>
              )}
            </div>
          </li>
        ))}
        {data && data.content.length === 0 && (
          <li className="py-10 text-center text-sm text-ink/40">No comments yet.</li>
        )}
      </ul>
    </div>
  );
}

export default function Page() {
  return <AdminComments />;
}
