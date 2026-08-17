'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { RequireAuth, useAuth } from '@/lib/auth';

const TABS = [
  { href: '/admin', label: 'Overview', exact: true },
  { href: '/admin/users', label: 'Users' },
  { href: '/admin/schools', label: 'Schools' },
  { href: '/admin/questions', label: 'Questions' },
  { href: '/admin/memories', label: 'Memories' },
  { href: '/admin/comments', label: 'Comments' },
];

function AdminShell({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (user && user.role !== 'ADMIN') router.replace('/feed');
  }, [user, router]);

  if (!user || user.role !== 'ADMIN') {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <p className="text-sm text-ink/40">Admins only.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-10 px-5 py-14 lg:flex-row">
      <aside className="lg:w-52 lg:shrink-0">
        <p className="label mb-4">Admin</p>
        <nav className="flex flex-row flex-wrap gap-2 lg:flex-col lg:gap-1">
          {TABS.map((t) => {
            const active = t.exact ? pathname === t.href : pathname.startsWith(t.href);
            return (
              <Link
                key={t.href}
                href={t.href}
                className={`rounded-sm px-3 py-2 text-sm transition-colors ${
                  active ? 'bg-ink text-paper' : 'text-ink/60 hover:bg-paper-deep hover:text-ink'
                }`}
              >
                {t.label}
              </Link>
            );
          })}
        </nav>
      </aside>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth>
      <AdminShell>{children}</AdminShell>
    </RequireAuth>
  );
}
