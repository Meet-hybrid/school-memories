'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useTheme } from '@/lib/theme';
import Avatar from './Avatar';
import Wordmark from './Wordmark';
import { useState } from 'react';

const LINKS = [
  { href: '/feed', label: 'Feed' },
  { href: '/challenge', label: 'Challenge' },
  { href: '/games', label: 'Games' },
  { href: '/discover', label: 'Classmates' },
];

export default function Nav() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  const { data: unread } = useQuery({
    queryKey: ['unread'],
    queryFn: api.unreadCount,
    enabled: !!user,
    refetchInterval: 60_000,
  });

  const linkClass = (href: string) =>
    `text-sm transition-colors ${pathname.startsWith(href) ? 'text-ink font-medium' : 'text-ink/55 hover:text-ink'}`;

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-paper/90 backdrop-blur">
      <div className="mx-auto flex h-16 min-w-0 max-w-6xl items-center justify-between gap-2 px-4 sm:px-5">
        <div className="min-w-0 flex-1">
          <Wordmark />
          {user && (
            <nav className="hidden items-center gap-6 md:flex">
              {LINKS.map((l) => (
                <Link key={l.href} href={l.href} className={linkClass(l.href)}>
                  {l.label}
                </Link>
              ))}
            </nav>
          )}
        </div>

        <div className="flex shrink-0 items-center gap-1 sm:gap-4">
          <button
            onClick={toggle}
            className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-line bg-surface text-ink/60 transition-colors hover:border-ink/40 hover:text-ink"
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {theme === 'dark' ? (
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32 1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
              </svg>
            ) : (
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
            )}
          </button>
          {user ? (
            <>
              <Link
                href="/notifications"
                className="relative text-sm text-ink/55 transition-colors hover:text-ink"
                aria-label="Notifications"
              >
                Notifications
                {unread && unread.count > 0 && (
                  <span className="absolute -right-3 -top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-clay px-1 text-[0.6rem] font-semibold text-paper">
                    {unread.count > 9 ? '9+' : unread.count}
                  </span>
                )}
              </Link>
              <Link
                href={user.username ? `/u/${user.username}` : `/u/${user.id}`}
                className="flex items-center gap-2 rounded-full border border-line bg-surface py-1 pl-1 pr-3 transition-colors hover:border-ink/30"
              >
                <Avatar name={user.fullName} src={user.avatarUrl} size={28} />
                <span className="hidden max-w-28 truncate text-sm sm:block">
                  {user.nickname || user.fullName.split(' ')[0]}
                </span>
              </Link>
              {user.role === 'ADMIN' && (
                <Link href="/admin" className="hidden text-sm text-ink/55 hover:text-ink lg:block">
                  Admin
                </Link>
              )}
              <button onClick={() => setOpen((v) => !v)} className="md:hidden text-ink/60" aria-label="Menu">
                {open ? 'Close' : 'Menu'}
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="btn-quiet !px-2.5 !py-2 text-xs sm:!px-5 sm:!py-2.5 sm:!text-sm">
                Sign in
              </Link>
              <Link href="/register" className="btn-primary !px-2.5 !py-2 text-xs sm:!px-5 sm:!py-2.5 sm:!text-sm">
                <span className="sm:hidden">Join</span>
                <span className="hidden sm:inline">Join your school</span>
              </Link>
            </>
          )}
        </div>
      </div>

      {open && user && (
        <div className="border-t border-line bg-paper px-5 py-4 md:hidden">
          <nav className="flex flex-col gap-3">
            {LINKS.map((l) => (
              <Link key={l.href} href={l.href} onClick={() => setOpen(false)} className={linkClass(l.href)}>
                {l.label}
              </Link>
            ))}
            <Link href="/notifications" onClick={() => setOpen(false)} className={linkClass('/notifications')}>
              Notifications
            </Link>
            {user.role === 'ADMIN' && (
              <Link href="/admin" onClick={() => setOpen(false)} className={linkClass('/admin')}>
                Admin
              </Link>
            )}
            <button
              onClick={() => {
                logout();
                setOpen(false);
              }}
              className="text-left text-sm text-ink/55"
            >
              Sign out
            </button>
          </nav>
        </div>
      )}
    </header>
  );
}
