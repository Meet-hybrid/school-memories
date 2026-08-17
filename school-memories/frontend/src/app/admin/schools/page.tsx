'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

function AdminSchools() {
  const queryClient = useQueryClient();
  const { data: schools } = useQuery({ queryKey: ['schools'], queryFn: api.schools });

  const [schoolName, setSchoolName] = useState('');
  const [schoolDesc, setSchoolDesc] = useState('');
  const [setFor, setSetFor] = useState<number | null>(null);
  const [newSetName, setNewSetName] = useState('');
  const [newSetYear, setNewSetYear] = useState('');
  const [error, setError] = useState<string | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['schools'] });
    queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
  };

  const createSchool = useMutation({
    mutationFn: () => api.adminCreateSchool(schoolName, schoolDesc || undefined),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setSchoolName('');
      setSchoolDesc('');
      setError(null);
      invalidate();
    },
  });

  const createSet = useMutation({
    mutationFn: () => api.adminCreateSet(setFor!, newSetName, newSetYear ? Number(newSetYear) : undefined),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setNewSetName('');
      setNewSetYear('');
      setError(null);
      invalidate();
    },
  });

  return (
    <div>
      <h1 className="font-serif text-3xl font-medium">Schools</h1>
      <p className="mt-2 max-w-lg text-sm text-ink/55">
        Schools and graduating sets are how classmates find each other. Keep names consistent.
      </p>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          createSchool.mutate();
        }}
        className="mt-8 flex max-w-2xl flex-col gap-3 border border-line bg-surface p-5"
      >
        <p className="label">Add a school</p>
        <div className="flex gap-3">
          <input value={schoolName} onChange={(e) => setSchoolName(e.target.value)} className="input" placeholder="School name" required />
          <input value={schoolDesc} onChange={(e) => setSchoolDesc(e.target.value)} className="input" placeholder="Short description (optional)" />
        </div>
        {error && <p className="text-sm text-clay">{error}</p>}
        <div>
          <button type="submit" disabled={!schoolName.trim()} className="btn-primary !py-2 text-xs">
            Add school
          </button>
        </div>
      </form>

      <ul className="mt-10 flex max-w-2xl flex-col gap-8">
        {schools?.map((s) => (
          <li key={s.id} className="border border-line bg-surface p-5">
            <div className="flex items-baseline justify-between gap-4">
              <h2 className="font-serif text-xl font-medium">{s.name}</h2>
              <span className="text-xs text-ink/40">{s.description}</span>
            </div>

            {/* add set */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                setSetFor(s.id);
                createSet.mutate();
              }}
              className="mt-4 flex items-center gap-3"
            >
              <input
                value={setFor === s.id ? newSetName : ''}
                onChange={(e) => {
                  setSetFor(s.id);
                  setNewSetName(e.target.value);
                }}
                className="input !py-2 text-xs"
                placeholder="New set, e.g. “Set of 2022”"
                required
              />
              <input
                value={setFor === s.id ? newSetYear : ''}
                onChange={(e) => {
                  setSetFor(s.id);
                  setNewSetYear(e.target.value);
                }}
                type="number"
                min={1950}
                max={2030}
                className="input !w-28 !py-2 text-xs"
                placeholder="Year"
              />
              <button type="submit" className="btn-outline !py-2 text-xs">
                Add set
              </button>
            </form>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function Page() {
  return <AdminSchools />;
}
