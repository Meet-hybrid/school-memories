export function formatDate(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

export function timeAgo(iso?: string | null): string {
  if (!iso) return '';
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return formatDate(iso);
}

export function dayLabel(day: number): string {
  return String(day).padStart(2, '0');
}

/** Day 20's question contains "[SCHOOL NAME]" — substitute the user's school. */
export function renderQuestion(question: string, schoolName?: string | null): string {
  if (!question) return '';
  return question.replaceAll('[SCHOOL NAME]', schoolName || 'my school');
}

export function initials(name?: string | null): string {
  if (!name) return '?';
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();
}

export const MOODS = ['nostalgic', 'joyful', 'funny', 'proud', 'grateful', 'sad'] as const;

export const LEADERBOARD_TYPES = [
  { key: 'memories', label: 'Most memories' },
  { key: 'likes', label: 'Most likes' },
  { key: 'comments', label: 'Most comments' },
  { key: 'streak', label: 'Longest streak' },
] as const;
