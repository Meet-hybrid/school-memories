'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { timeAgo } from '@/lib/format';

function AdminOverview() {
  const queryClient = useQueryClient();
  const { data: stats } = useQuery({ queryKey: ['admin-stats'], queryFn: api.adminStats });
  const { data: announcements } = useQuery({ queryKey: ['admin-announcements'], queryFn: api.adminAnnouncements });

  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [error, setError] = useState<string | null>(null);

  const createMutation = useMutation({
    mutationFn: () => api.adminCreateAnnouncement(title, body),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setTitle('');
      setBody('');
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['admin-announcements'] });
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: api.adminDeleteAnnouncement,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-announcements'] });
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
    },
  });

  const tiles = stats
    ? [
        { label: 'Users', value: stats.users },
        { label: 'Memories', value: stats.memories },
        { label: 'Comments', value: stats.comments },
        { label: 'Schools', value: stats.schools },
        { label: 'Questions', value: stats.questions },
        { label: 'Announcements', value: stats.announcements },
      ]
    : [];

  return (
    <div>
      <h1 className="font-serif text-3xl font-medium">Overview</h1>
      <p className="mt-2 text-sm text-ink/55">Basic engagement stats for your school&apos;s archive.</p>

      <div className="mt-8 grid grid-cols-2 gap-px overflow-hidden border border-line bg-line sm:grid-cols-3">
        {tiles.map((t) => (
          <div key={t.label} className="bg-surface px-5 py-4">
            <dt className="label">{t.label}</dt>
            <dd className="mt-1 font-serif text-3xl font-medium">{t.value}</dd>
          </div>
        ))}
      </div>

      {/* announcements */}
      <section className="mt-14">
        <h2 className="label mb-4">Publish an announcement</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="flex max-w-xl flex-col gap-3"
        >
          <input value={title} onChange={(e) => setTitle(e.target.value)} className="input" placeholder="Title" required />
          <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={3} className="input resize-none" placeholder="What should everyone know?" required />
          {error && <p className="text-sm text-clay">{error}</p>}
          <div>
            <button type="submit" disabled={!title.trim() || !body.trim()} className="btn-primary !py-2 text-xs">
              Publish
            </button>
          </div>
        </form>

        <ul className="mt-8 flex max-w-xl flex-col gap-3">
          {announcements?.map((a) => (
            <li key={a.id} className="flex items-start justify-between gap-4 border border-line bg-surface px-4 py-3">
              <div>
                <p className="text-sm font-medium">{a.title}</p>
                <p className="mt-0.5 text-xs text-ink/45">
                  {timeAgo(a.createdAt)} · {a.active ? 'active' : 'hidden'}
                </p>
              </div>
              <button onClick={() => deleteMutation.mutate(a.id)} className="text-xs text-ink/40 hover:text-clay">
                Delete
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

export default function Page() {
  return <AdminOverview />;
}
