import Link from 'next/link';
import Wordmark from './Wordmark';

export default function Footer() {
  return (
    <footer className="mt-24 border-t border-line">
      <div className="mx-auto flex max-w-6xl flex-col gap-6 px-5 py-12 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-2">
          <Wordmark />
          <p className="max-w-sm text-sm leading-6 text-ink/50">
            Thirty questions. One school. A permanent archive of the people we were.
          </p>
        </div>
        <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-ink/50">
          <Link href="/challenge" className="hover:text-ink">
            The Challenge
          </Link>
          <Link href="/discover" className="hover:text-ink">
            Find classmates
          </Link>
          <Link href="/register" className="hover:text-ink">
            Join
          </Link>
        </div>
      </div>
      <div className="border-t border-line py-4 text-center text-xs text-ink/35">
        © {new Date().getFullYear()} Keepsake — the school memory community
      </div>
    </footer>
  );
}
