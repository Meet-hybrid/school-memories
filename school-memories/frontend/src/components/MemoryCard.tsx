'use client';

import Link from 'next/link';
import type { MemoryDto } from '@/lib/types';
import Avatar from './Avatar';
import LikeButton from './LikeButton';
import { dayLabel, renderQuestion, timeAgo } from '@/lib/format';

export default function MemoryCard({ memory, schoolName }: { memory: MemoryDto; schoolName?: string | null }) {
  const author = memory.author;
  const authorHandle = author.username ? `/u/${author.username}` : `/u/${author.id}`;

  return (
    <article className="border-b border-line py-8 first:pt-0 last:border-b-0">
      {/* author row */}
      <div className="mb-4 flex items-start justify-between gap-3">
        <Link href={authorHandle} className="group flex items-center gap-3">
          <Avatar name={author.fullName} src={author.avatarUrl} size={40} />
          <div>
            <p className="text-sm font-medium leading-tight group-hover:underline">
              {author.nickname || author.fullName}
            </p>
            <p className="text-xs text-ink/45">
              {[author.className, author.graduationYear ? `Class of ${author.graduationYear}` : null]
                .filter(Boolean)
                .join(' · ') || 'Classmate'}
            </p>
          </div>
        </Link>
        <span className="shrink-0 text-xs text-ink/40">{timeAgo(memory.createdAt)}</span>
      </div>

      {/* day + question */}
      <p className="mb-3 flex items-baseline gap-2">
        <span className="font-serif text-sm font-semibold text-clay">Day {dayLabel(memory.dayNumber)}</span>
        {memory.question && (
          <span className="text-sm italic text-ink/55">{renderQuestion(memory.question, schoolName)}</span>
        )}
      </p>

      {/* answer */}
      <p className="prose-memory whitespace-pre-wrap">{memory.answer}</p>

      {/* media — feeds load the generated preview; the full file is one click away */}
      {memory.mediaUrl && memory.mediaType === 'PHOTO' && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={memory.thumbnailUrl || memory.mediaUrl}
          alt=""
          className="mt-4 max-h-96 w-full rounded-sm object-cover"
        />
      )}
      {memory.mediaUrl && memory.mediaType === 'VIDEO' && (
        <video
          src={memory.mediaUrl}
          poster={memory.thumbnailUrl || undefined}
          controls
          className="mt-4 max-h-96 w-full rounded-sm bg-black"
        />
      )}

      {/* mood */}
      {memory.mood && (
        <p className="mt-4 text-xs uppercase tracking-wide2 text-ink/35">Feeling: {memory.mood}</p>
      )}

      {/* actions */}
      <div className="mt-5 flex items-center gap-6">
        <LikeButton memoryId={memory.id} liked={memory.likedByMe} count={memory.likes} />
        <Link
          href={`/memory/${memory.id}`}
          className="inline-flex items-center gap-1.5 text-sm text-ink/55 transition-colors hover:text-ink"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
          </svg>
          <span>{memory.comments > 0 ? `${memory.comments} comment${memory.comments === 1 ? '' : 's'}` : 'Comment'}</span>
        </Link>
      </div>
    </article>
  );
}
