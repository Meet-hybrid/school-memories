'use client';

import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth } from '@/lib/auth';
import Avatar from '@/components/Avatar';
import EmptyState from '@/components/EmptyState';
import { timeAgo } from '@/lib/format';

function NotificationsPage() {
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ['notifications'], queryFn: api.notifications });

  const markRead = useMutation({
    mutationFn: api.markAllRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread'] });
    },
  });

  const notifications = data?.content ?? [];
  const unread = notifications.filter((n) => !n.read).length;

  return (
    <div className="mx-auto max-w-2xl px-5 py-14">
      <div className="flex items-baseline justify-between">
        <div>
          <p className="label mb-3">Notifications</p>
          <h1 className="font-serif text-4xl font-medium">Your inbox</h1>
        </div>
        {unread > 0 && (
          <button onClick={() => markRead.mutate()} className="text-sm text-ink/50 hover:text-ink">
            Mark all read
          </button>
        )}
      </div>

      <div className="mt-10">
        {notifications.length === 0 ? (
          <EmptyState
            title="Nothing here yet"
            body="When someone likes your memory, comments, or follows you, it shows up here."
          />
        ) : (
          <ul className="divide-y divide-line border-t border-line">
            {notifications.map((n) => {
              const href =
                n.memoryId != null
                  ? `/memory/${n.memoryId}`
                  : n.actorId != null
                    ? `/u/${n.actorId}`
                    : '/feed';
              return (
                <li key={n.id}>
                  <Link href={href} className="flex items-center gap-4 py-4 hover:bg-paper-deep/50">
                    <Avatar name={n.actorName} src={n.actorAvatarUrl} size={36} />
                    <div className="flex-1">
                      <p className={`text-sm leading-6 ${n.read ? 'text-ink/55' : 'text-ink'}`}>
                        {n.message}
                        {!n.read && <span className="ml-2 inline-block h-1.5 w-1.5 rounded-full bg-clay align-middle" />}
                      </p>
                      <p className="mt-0.5 text-xs text-ink/35">{timeAgo(n.createdAt)}</p>
                    </div>
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <NotificationsPage />
    </RequireAuth>
  );
}
