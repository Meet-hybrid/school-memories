'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import Avatar from '@/components/Avatar';

function AdminUsers() {
  const queryClient = useQueryClient();
  const [q, setQ] = useState('');
  const { data } = useQuery({ queryKey: ['admin-users', q], queryFn: () => api.adminUsers(q) });

  const toggleActive = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => api.adminSetUserActive(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
  });
  const toggleRole = useMutation({
    mutationFn: ({ id, role }: { id: number; role: string }) => api.adminSetUserRole(id, role),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  return (
    <div>
      <h1 className="font-serif text-3xl font-medium">Users</h1>
      <p className="mt-2 text-sm text-ink/55">Manage accounts and roles.</p>

      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        className="input mt-6 max-w-sm"
        placeholder="Search by name or email"
      />

      <div className="mt-6 overflow-x-auto border border-line bg-surface">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead className="border-b border-line">
            <tr className="text-xs uppercase tracking-wide2 text-ink/45">
              <th className="px-4 py-3 font-medium">User</th>
              <th className="px-4 py-3 font-medium">School</th>
              <th className="px-4 py-3 font-medium">Memories</th>
              <th className="px-4 py-3 font-medium">Role</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {data?.content.map((u) => (
              <tr key={u.id}>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <Avatar name={u.fullName} src={null} size={30} />
                    <div>
                      <p className="font-medium">{u.fullName}</p>
                      <p className="text-xs text-ink/45">{u.email}</p>
                    </div>
                  </div>
                </td>
                <td className="px-4 py-3 text-ink/60">{u.schoolName ?? '—'}</td>
                <td className="px-4 py-3">{u.memories}</td>
                <td className="px-4 py-3">
                  <select
                    value={u.role}
                    onChange={(e) => toggleRole.mutate({ id: u.id, role: e.target.value })}
                    className="input !w-28 !py-1.5 text-xs"
                  >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td className="px-4 py-3">
                  <span className={`text-xs font-medium ${u.active ? 'text-moss' : 'text-clay'}`}>
                    {u.active ? 'Active' : 'Disabled'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => toggleActive.mutate({ id: u.id, active: !u.active })}
                    className="text-xs text-ink/45 hover:text-ink"
                  >
                    {u.active ? 'Disable' : 'Enable'}
                  </button>
                </td>
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-ink/40">
                  No users found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function Page() {
  return <AdminUsers />;
}
