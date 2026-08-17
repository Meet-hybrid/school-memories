'use client';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

export default function LikeButton({ memoryId, liked, count }: { memoryId: number; liked: boolean; count: number }) {
  const queryClient = useQueryClient();
  const [optimistic, setOptimistic] = useState<{ liked: boolean; count: number } | null>(null);

  const mutation = useMutation({
    mutationFn: () => api.react(memoryId),
    onMutate: () => {
      setOptimistic((prev) => {
        const base = prev ?? { liked, count };
        return { liked: !base.liked, count: base.count + (base.liked ? -1 : 1) };
      });
    },
    onError: () => setOptimistic(null),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feed'] });
      queryClient.invalidateQueries({ queryKey: ['memory'] });
      queryClient.invalidateQueries({ queryKey: ['profile-memories'] });
    },
  });

  const state = optimistic ?? { liked, count };
  const label = state.liked ? 'Liked' : 'Like';

  return (
    <button
      onClick={() => mutation.mutate()}
      className={`group inline-flex items-center gap-1.5 text-sm transition-colors ${
        state.liked ? 'text-clay font-medium' : 'text-ink/55 hover:text-ink'
      }`}
      aria-pressed={state.liked}
    >
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill={state.liked ? 'currentColor' : 'none'}
        stroke="currentColor"
        strokeWidth="1.8"
        className="transition-transform duration-150 group-active:scale-75"
      >
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
      </svg>
      <span>
        {label} {state.count > 0 && `· ${state.count}`}
      </span>
    </button>
  );
}
