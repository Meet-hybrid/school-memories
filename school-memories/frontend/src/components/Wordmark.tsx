import Link from 'next/link';

export default function Wordmark({ light = false }: { light?: boolean }) {
  return (
    <Link href="/" className={`group flex items-baseline gap-1.5 ${light ? 'text-paper' : 'text-ink'}`}>
      <span className="font-serif text-[1.35rem] font-semibold tracking-tight">Keepsake</span>
      <span className="hidden text-[0.65rem] uppercase tracking-wide2 opacity-60 sm:inline">the school memory community</span>
    </Link>
  );
}
