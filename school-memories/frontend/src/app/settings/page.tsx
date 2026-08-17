'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import Avatar from '@/components/Avatar';

function SettingsPage() {
  const { user, refresh } = useAuth();
  const queryClient = useQueryClient();
  const { data: profile } = useQuery({ queryKey: ['my-profile'], queryFn: api.myProfile, enabled: !!user });

  const [form, setForm] = useState({
    fullName: '',
    nickname: '',
    username: '',
    bio: '',
    graduationYear: '',
    classSetId: '',
  });
  const [initialized, setInitialized] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // populate once the profile loads
  if (profile && !initialized) {
    setForm({
      fullName: profile.fullName || '',
      nickname: profile.nickname || '',
      username: profile.username || '',
      bio: profile.bio || '',
      graduationYear: profile.graduationYear ? String(profile.graduationYear) : '',
      classSetId: profile.classSet?.id ? String(profile.classSet.id) : '',
    });
    setInitialized(true);
  }

  const { data: sets } = useQuery({
    queryKey: ['sets', profile?.school?.id],
    queryFn: () => api.sets(profile!.school!.id),
    enabled: !!profile?.school?.id,
  });

  const saveMutation = useMutation({
    mutationFn: () =>
      api.updateProfile({
        fullName: form.fullName,
        nickname: form.nickname || null,
        username: form.username || null,
        bio: form.bio || null,
        graduationYear: form.graduationYear ? Number(form.graduationYear) : null,
        classSetId: form.classSetId ? Number(form.classSetId) : null,
      }),
    onSuccess: async () => {
      setMessage('Profile saved.');
      setError(null);
      await refresh();
      queryClient.invalidateQueries({ queryKey: ['my-profile'] });
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
    onError: (e: Error) => setError(e.message),
  });

  const avatarMutation = useMutation({
    mutationFn: (file: File) => {
      const fd = new FormData();
      fd.append('file', file);
      return api.uploadAvatar(fd);
    },
    onSuccess: () => {
      setMessage('Profile picture updated.');
      refresh();
      queryClient.invalidateQueries({ queryKey: ['my-profile'] });
    },
    onError: (e: Error) => setError(e.message),
  });

  return (
    <div className="mx-auto max-w-xl px-5 py-14">
      <p className="label mb-3">Settings</p>
      <h1 className="font-serif text-4xl font-medium">Your profile</h1>
      <p className="mt-3 text-sm text-ink/55">
        How your classmates see you in the archive. Your username becomes your profile link.
      </p>

      <div className="mt-10 flex items-center gap-5">
        <Avatar name={profile?.fullName} src={profile?.avatarUrl} size={64} />
        <label className="btn-outline !py-2 text-xs">
          Change photo
          <input
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) avatarMutation.mutate(f);
            }}
          />
        </label>
        {profile?.avatarUrl && (
          <button
            onClick={() => api.updateProfile({ avatarUrl: '' }).then(() => refresh())}
            className="text-xs text-ink/45 hover:text-clay"
          >
            Remove
          </button>
        )}
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          saveMutation.mutate();
        }}
        className="mt-8 flex flex-col gap-5"
      >
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">Full name</span>
          <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} className="input" required />
        </label>
        <div className="grid grid-cols-2 gap-4">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Nickname</span>
            <input value={form.nickname} onChange={(e) => setForm({ ...form, nickname: e.target.value })} className="input" />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Username</span>
            <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} className="input" placeholder="ada-obi" />
          </label>
        </div>
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="label">Bio</span>
          <textarea
            value={form.bio}
            onChange={(e) => setForm({ ...form, bio: e.target.value })}
            rows={3}
            className="input resize-none"
            placeholder="A line or two about you, school, or what you're up to now."
            maxLength={500}
          />
        </label>
        <div className="grid grid-cols-2 gap-4">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Set / Class</span>
            <select
              value={form.classSetId}
              onChange={(e) => setForm({ ...form, classSetId: e.target.value })}
              className="input"
            >
              <option value="">None</option>
              {(sets ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="label">Graduation year</span>
            <input
              type="number"
              min={1950}
              max={2030}
              value={form.graduationYear}
              onChange={(e) => setForm({ ...form, graduationYear: e.target.value })}
              className="input"
            />
          </label>
        </div>

        {error && <p className="text-sm text-clay">{error}</p>}
        {message && <p className="text-sm text-moss">{message}</p>}

        <button type="submit" disabled={saveMutation.isPending} className="btn-primary mt-2">
          {saveMutation.isPending ? 'Saving…' : 'Save profile'}
        </button>
      </form>
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <SettingsPage />
    </RequireAuth>
  );
}
