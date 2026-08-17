'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { CommentDto } from '@/lib/types';
import Avatar from './Avatar';
import { timeAgo } from '@/lib/format';

export default function CommentSection({ memoryId }: { memoryId: number }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [body, setBody] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['comments', memoryId],
    queryFn: () => api.comments(memoryId),
  });

  const addMutation = useMutation({
    mutationFn: () => api.addComment(memoryId, body),
    onMutate: () => setError(null),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setBody('');
      queryClient.invalidateQueries({ queryKey: ['comments', memoryId] });
      queryClient.invalidateQueries({ queryKey: ['memory', memoryId] });
      queryClient.invalidateQueries({ queryKey: ['feed'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.deleteComment(memoryId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments', memoryId] });
      queryClient.invalidateQueries({ queryKey: ['memory', memoryId] });
    },
  });

  const comments: CommentDto[] = data?.content ?? [];

  return (
    <div className="mt-10 border-t border-line pt-8">
      <h2 className="label mb-6">{comments.length} comment{comments.length === 1 ? '' : 's'}</h2>

      {isLoading && <p className="text-sm text-ink/40">Loading comments…</p>}

      {comments.length === 0 && !isLoading && (
        <p className="mb-6 text-sm text-ink/45">No comments yet. Start the conversation.</p>
      )}

      <ul className="mb-8 flex flex-col gap-6">
        {comments.map((c) => {
          const handle = c.authorUsername ? `/u/${c.authorUsername}` : `/u/${c.authorId}`;
          const mine = user?.id === c.authorId;
          return (
            <li key={c.id} className="flex gap-3">
              <Link href={handle}>
                <Avatar name={c.authorName} src={c.authorAvatarUrl} size={32} />
              </Link>
              <div className="flex-1">
                <div className="flex items-baseline gap-2">
                  <Link href={handle} className="text-sm font-medium hover:underline">
                    {c.authorNickname || c.authorName}
                  </Link>
                  <span className="text-xs text-ink/35">{timeAgo(c.createdAt)}</span>
                  {mine && (
                    <button
                      onClick={() => deleteMutation.mutate(c.id)}
                      className="ml-auto text-xs text-ink/35 hover:text-clay"
                    >
                      Delete
                    </button>
                  )}
                </div>
                <p className="mt-0.5 text-sm leading-6 text-ink/80">{c.body}</p>
              </div>
            </li>
          );
        })}
      </ul>

      {user && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (body.trim()) addMutation.mutate();
          }}
          className="flex flex-col gap-3"
        >
          <div className="flex items-start gap-3">
            <Avatar name={user.fullName} src={user.avatarUrl} size={32} />
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={2}
              placeholder="Share a thought with your classmate…"
              className="input resize-none"
            />
          </div>
          {error && <p className="text-sm text-clay">{error}</p>}
          <div className="flex justify-end">
            <button type="submit" disabled={!body.trim() || addMutation.isPending} className="btn-primary !py-2 text-xs">
              Post comment
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
