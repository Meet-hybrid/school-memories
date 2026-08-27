'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

export default function FollowButton({ userId, following }: { userId: number; following: boolean }) {
  const queryClient = useQueryClient();
  const [isFollowing, setIsFollowing] = useState(following);

  useEffect(() => {
    setIsFollowing(following);
  }, [following]);

  const mutation = useMutation({
    mutationFn: () => (isFollowing ? api.unfollow(userId) : api.follow(userId)),
    onMutate: () => setIsFollowing((v) => !v),
    onError: () => setIsFollowing(following),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      queryClient.invalidateQueries({ queryKey: ['suggested'] });
    },
  });

  return (
    <button
      type="button"
      onClick={() => mutation.mutate()}
      disabled={mutation.isPending}
      className={isFollowing ? 'btn-outline !py-1.5 text-xs' : 'btn-primary !py-1.5 text-xs'}
    >
      {isFollowing ? 'Following' : 'Follow'}
    </button>
  );
}
