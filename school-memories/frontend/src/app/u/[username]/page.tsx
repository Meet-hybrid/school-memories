'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { RequireAuth, useAuth } from '@/lib/auth';
import Avatar from '@/components/Avatar';
import FollowButton from '@/components/FollowButton';
import MemoryCard from '@/components/MemoryCard';
import EmptyState from '@/components/EmptyState';
import { dayLabel } from '@/lib/format';

function ProfilePage() {
  const params = useParams<{ username: string }>();
  const handle = params.username;
  const { user } = useAuth();

  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile', handle],
    queryFn: () => api.userProfile(handle),
  });
  const { data: memories } = useQuery({
    queryKey: ['profile-memories', profile?.id],
    queryFn: () => api.userMemories(profile!.id),
    enabled: !!profile,
  });
  const { data: achievements } = useQuery({
    queryKey: ['profile-achievements', profile?.id],
    queryFn: () => api.myAchievements(),
    enabled: !!profile && profile.id === user?.id,
  });

  if (isLoading) return <p className="py-20 text-center text-sm text-ink/40">Loading…</p>;
  if (!profile) return <p className="py-20 text-center text-sm text-ink/50">Classmate not found.</p>;

  const isMe = user?.id === profile.id;
  const myMemories = memories ?? [];

  return (
    <div className="mx-auto max-w-3xl px-5 py-14">
      {/* header */}
      <header className="border-b border-line pb-10">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <Avatar name={profile.fullName} src={profile.avatarUrl} size={96} />
            <div>
              <h1 className="font-serif text-4xl font-medium">{profile.nickname || profile.fullName}</h1>
              <p className="mt-1 text-base text-ink/55">
                {profile.fullName}
                {profile.nickname && profile.nickname !== profile.fullName && ` · “${profile.nickname}”`}
              </p>
              <p className="mt-2 text-sm text-ink/45">
                {[profile.school?.name, profile.classSet?.name, profile.graduationYear ? `Class of ${profile.graduationYear}` : null]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
              {profile.bio && <p className="mt-4 max-w-md text-sm leading-6 text-ink/70">{profile.bio}</p>}
            </div>
          </div>

          <div className="flex shrink-0 flex-col items-start gap-3 sm:items-end">
            {isMe ? (
              <Link href="/settings" className="btn-outline !py-2 text-xs">
                Edit profile
              </Link>
            ) : (
              <FollowButton userId={profile.id} following={profile.following} />
            )}
            <p className="text-xs text-ink/40">
              Joined {new Date(profile.createdAt).toLocaleDateString(undefined, { month: 'short', year: 'numeric' })}
              {profile.verified && ' · verified'}
            </p>
          </div>
        </div>

        {/* stats */}
        <dl className="mt-8 grid grid-cols-2 gap-px overflow-hidden border border-line bg-line sm:grid-cols-4">
          {[
            { label: 'Memories', value: profile.memories },
            { label: 'Likes received', value: profile.likesReceived },
            { label: 'Followers', value: profile.followers },
            { label: 'Following', value: profile.followingCount },
          ].map((s) => (
            <div key={s.label} className="bg-surface px-5 py-4">
              <dt className="label">{s.label}</dt>
              <dd className="mt-1 font-serif text-2xl font-medium">{s.value}</dd>
            </div>
          ))}
        </dl>
      </header>

      {/* challenge progress */}
      {myMemories.length > 0 && (
        <section className="mt-10">
          <div className="mb-4 flex items-baseline justify-between">
            <h2 className="label">Challenge progress</h2>
            <span className="text-xs text-ink/45">
              {myMemories.length} of 30 days
            </span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {Array.from({ length: 30 }, (_, i) => i + 1).map((d) => {
              const answered = myMemories.some((m) => m.dayNumber === d);
              return (
                <Link
                  key={d}
                  href={answered ? `/memory/${myMemories.find((m) => m.dayNumber === d)!.id}` : `/challenge/${d}`}
                  title={`Day ${dayLabel(d)}${answered ? '' : ' — unanswered'}`}
                  className={`flex h-7 w-7 items-center justify-center rounded-full text-[0.65rem] font-medium transition-transform hover:scale-110 ${
                    answered ? 'bg-clay text-paper' : 'border border-line text-ink/30 hover:text-ink'
                  }`}
                >
                  {d}
                </Link>
              );
            })}
          </div>
        </section>
      )}

      {/* achievements */}
      {isMe && achievements && achievements.length > 0 && (
        <section className="mt-10">
          <h2 className="label mb-4">Achievements</h2>
          <div className="flex flex-wrap gap-2">
            {achievements.map((a) => (
              <span key={a.code} title={a.description} className="border border-line bg-surface px-3 py-1.5 text-xs text-ink/70">
                {a.name}
              </span>
            ))}
          </div>
        </section>
      )}

      {/* memories */}
      <section className="mt-12">
        <h2 className="label mb-2">Memories</h2>
        {myMemories.length === 0 ? (
          <EmptyState
            title={isMe ? 'Your archive is empty' : 'Nothing shared yet'}
            body={
              isMe
                ? 'Answer your first question and it will live here forever.'
                : 'This classmate hasn\u2019t shared a memory yet. Send them a nudge.'
            }
            action={
              isMe ? (
                <Link href="/challenge" className="btn-primary mt-2">
                  Start the challenge
                </Link>
              ) : undefined
            }
          />
        ) : (
          <div>
            {[...myMemories].reverse().map((m) => (
              <MemoryCard key={m.id} memory={m} schoolName={profile.school?.name} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <ProfilePage />
    </RequireAuth>
  );
}
