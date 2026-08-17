'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, AdminQuestionRow } from '@/lib/api';

function AdminQuestions() {
  const queryClient = useQueryClient();
  const { data: questions } = useQuery({ queryKey: ['admin-questions'], queryFn: api.adminQuestions });

  const [newDay, setNewDay] = useState('');
  const [newQuestion, setNewQuestion] = useState('');
  const [newHint, setNewHint] = useState('');
  const [editing, setEditing] = useState<AdminQuestionRow | null>(null);
  const [error, setError] = useState<string | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-questions'] });
    queryClient.invalidateQueries({ queryKey: ['challenge'] });
  };

  const createMutation = useMutation({
    mutationFn: () =>
      api.adminCreateQuestion({
        dayNumber: Number(newDay),
        question: newQuestion,
        hint: newHint || undefined,
      }),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setNewDay('');
      setNewQuestion('');
      setNewHint('');
      setError(null);
      invalidate();
    },
  });

  const updateMutation = useMutation({
    mutationFn: (row: AdminQuestionRow) => api.adminUpdateQuestion(row.id, { ...row }),
    onError: (e: Error) => setError(e.message),
    onSuccess: () => {
      setEditing(null);
      setError(null);
      invalidate();
    },
  });

  return (
    <div>
      <h1 className="font-serif text-3xl font-medium">Challenge questions</h1>
      <p className="mt-2 max-w-lg text-sm text-ink/55">
        The 30 days are fully configurable. Questions marked inactive stay visible but can&apos;t be answered.
      </p>

      {/* add new */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          createMutation.mutate();
        }}
        className="mt-8 flex max-w-2xl flex-col gap-3 border border-line bg-surface p-5"
      >
        <p className="label">Add a question</p>
        <div className="flex gap-3">
          <input
            type="number"
            min={1}
            value={newDay}
            onChange={(e) => setNewDay(e.target.value)}
            className="input !w-28"
            placeholder="Day"
            required
          />
          <input
            value={newQuestion}
            onChange={(e) => setNewQuestion(e.target.value)}
            className="input"
            placeholder="Question text"
            required
          />
        </div>
        <input value={newHint} onChange={(e) => setNewHint(e.target.value)} className="input" placeholder="Hint (optional)" />
        {error && <p className="text-sm text-clay">{error}</p>}
        <div>
          <button type="submit" disabled={!newDay || !newQuestion.trim()} className="btn-primary !py-2 text-xs">
            Add question
          </button>
        </div>
      </form>

      {/* list */}
      <ul className="mt-10 flex max-w-2xl flex-col">
        {questions?.map((q) =>
          editing?.id === q.id ? (
            <li key={q.id} className="border-b border-line py-4">
              <div className="flex flex-col gap-2">
                <div className="flex gap-3">
                  <input
                    type="number"
                    value={editing.dayNumber}
                    onChange={(e) => setEditing({ ...editing, dayNumber: Number(e.target.value) })}
                    className="input !w-24 !py-2 text-xs"
                  />
                  <input
                    value={editing.question}
                    onChange={(e) => setEditing({ ...editing, question: e.target.value })}
                    className="input !py-2 text-xs"
                  />
                </div>
                <label className="flex items-center gap-2 text-xs text-ink/55">
                  <input
                    type="checkbox"
                    checked={editing.active}
                    onChange={(e) => setEditing({ ...editing, active: e.target.checked })}
                  />
                  Active (answerable)
                </label>
                <div className="flex gap-3">
                  <button onClick={() => updateMutation.mutate(editing)} className="text-xs font-medium text-moss">
                    Save
                  </button>
                  <button onClick={() => setEditing(null)} className="text-xs text-ink/45">
                    Cancel
                  </button>
                </div>
              </div>
            </li>
          ) : (
            <li key={q.id} className="flex items-center gap-4 border-b border-line py-4">
              <span className={`w-8 font-serif text-base font-semibold ${q.active ? 'text-clay' : 'text-ink/30'}`}>
                {String(q.dayNumber).padStart(2, '0')}
              </span>
              <div className="min-w-0 flex-1">
                <p className={`text-sm ${q.active ? 'text-ink' : 'text-ink/40 line-through'}`}>{q.question}</p>
              </div>
              <button onClick={() => setEditing(q)} className="text-xs text-ink/45 hover:text-ink">
                Edit
              </button>
            </li>
          ),
        )}
      </ul>
    </div>
  );
}

export default function Page() {
  return <AdminQuestions />;
}
