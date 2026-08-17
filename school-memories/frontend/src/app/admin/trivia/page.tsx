'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

const OPTION_LABELS = ['A', 'B', 'C', 'D'];

function AdminTrivia() {
  const queryClient = useQueryClient();
  const { data: questions } = useQuery({ queryKey: ['admin-trivia'], queryFn: api.adminTrivia });

  const [question, setQuestion] = useState('');
  const [options, setOptions] = useState(['', '', '', '']);
  const [correctIndex, setCorrectIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-trivia'] });
  };

  const createMutation = useMutation({
    mutationFn: () => api.adminCreateTrivia({ question, options, correctIndex }),
    onSuccess: () => {
      setQuestion('');
      setOptions(['', '', '', '']);
      setCorrectIndex(0);
      setError(null);
      invalidate();
    },
    onError: (e: Error) => setError(e.message),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => api.adminUpdateTrivia(id, active),
    onSuccess: invalidate,
  });

  const deleteMutation = useMutation({ mutationFn: api.adminDeleteTrivia, onSuccess: invalidate });

  const complete = question.trim() && options.every((o) => o.trim());

  return (
    <div>
      <h1 className="font-serif text-3xl font-medium">Trivia</h1>
      <p className="mt-2 text-sm text-ink/55">
        Multiple-choice questions for the School Trivia game. The correct answer is never sent to players.
      </p>

      <section className="mt-10 max-w-xl">
        <h2 className="label mb-4">New question</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="flex flex-col gap-4"
        >
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            rows={2}
            className="input resize-none"
            placeholder="Which year did the library open?"
            required
          />
          {options.map((option, i) => (
            <div key={i} className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setCorrectIndex(i)}
                title={correctIndex === i ? 'Correct answer' : 'Mark as correct'}
                className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-xs font-semibold transition-colors ${
                  correctIndex === i
                    ? 'border-ink bg-ink text-paper'
                    : 'border-line text-ink/45 hover:border-ink/40'
                }`}
              >
                {OPTION_LABELS[i]}
              </button>
              <input
                value={option}
                onChange={(e) => setOptions(options.map((o, j) => (j === i ? e.target.value : o)))}
                className="input"
                placeholder={`Option ${OPTION_LABELS[i]}`}
                required
              />
            </div>
          ))}
          <p className="text-xs text-ink/40">Tap a letter to mark the correct answer.</p>
          {error && <p className="text-sm text-clay">{error}</p>}
          <div>
            <button type="submit" disabled={!complete || createMutation.isPending} className="btn-primary !py-2 text-xs">
              {createMutation.isPending ? 'Saving…' : 'Add question'}
            </button>
          </div>
        </form>
      </section>

      <section className="mt-14">
        <h2 className="label mb-4">All questions</h2>
        <ul className="flex max-w-xl flex-col gap-3">
          {questions?.map((q) => (
            <li key={q.id} className="border border-line bg-surface px-4 py-3">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className="text-sm font-medium">{q.question}</p>
                  <ul className="mt-1.5 flex flex-col gap-0.5 text-xs text-ink/55">
                    {q.options.map((option, i) => (
                      <li key={i} className={i === q.correctIndex ? 'font-medium text-ink' : ''}>
                        {OPTION_LABELS[i]}. {option}
                        {i === q.correctIndex && ' ✓'}
                      </li>
                    ))}
                  </ul>
                  <p className="mt-1.5 text-xs text-ink/40">
                    {q.schoolName ? q.schoolName : 'All schools'} · {q.active ? 'live' : 'hidden'}
                  </p>
                </div>
                <div className="flex shrink-0 flex-col items-end gap-2">
                  <button
                    onClick={() => toggleMutation.mutate({ id: q.id, active: !q.active })}
                    className="text-xs text-ink/50 hover:text-ink"
                  >
                    {q.active ? 'Hide' : 'Show'}
                  </button>
                  <button onClick={() => deleteMutation.mutate(q.id)} className="text-xs text-ink/40 hover:text-clay">
                    Delete
                  </button>
                </div>
              </div>
            </li>
          ))}
          {questions && questions.length === 0 && (
            <p className="text-sm text-ink/40">No trivia questions yet.</p>
          )}
        </ul>
      </section>
    </div>
  );
}

export default function Page() {
  return <AdminTrivia />;
}
