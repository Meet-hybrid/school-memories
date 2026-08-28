'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import type { UserDto } from '@/lib/types';

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
    onSuccess: (profile) => {
      setIsFollowing(profile.following);

      // Apply the server response immediately so profile counts update without
      // waiting for the invalidated query to finish refetching.
      queryClient.setQueriesData<UserDto>({ queryKey: ['profile'] }, (current) =>
        current?.id === profile.id
          ? { ...current, following: profile.following, followers: profile.followers, followingCount: profile.followingCount }
          : current,
      );
      queryClient.setQueryData<UserDto[]>(['suggested'], (current) =>
        current?.map((user) =>
          user.id === profile.id
            ? { ...user, following: profile.following, followers: profile.followers, followingCount: profile.followingCount }
            : user,
        ),
      );
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      queryClient.invalidateQueries({ queryKey: ['suggested'] });
    },
  });

  return (
    <div className="flex flex-col items-start gap-1">
      <button
        type="button"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
        className={isFollowing ? 'btn-outline !py-1.5 text-xs' : 'btn-primary !py-1.5 text-xs'}
      >
        {mutation.isPending ? 'Saving…' : isFollowing ? 'Following' : 'Follow'}
      </button>
      {mutation.isError && <p className="max-w-48 text-right text-xs text-clay">{mutation.error.message}</p>}
    </div>
  );
}
