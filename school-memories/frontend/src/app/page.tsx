import Link from 'next/link';

const SAMPLE_QUESTIONS = [
  { day: 5, text: 'Your Favourite School Event' },
  { day: 10, text: 'Your Best School Memory' },
  { day: 14, text: 'Most Embarrassing School Moment' },
  { day: 21, text: 'Your Biggest School Crush — optional' },
  { day: 30, text: 'Write a Letter to Your Younger School Self' },
];

const STEPS = [
  {
    n: '01',
    title: 'Join your school',
    body: "Find your school, your set, your year. Bring your real name — that's the whole point.",
  },
  {
    n: '02',
    title: 'Answer one question a day',
    body: 'Thirty questions about the people, places and moments that made school school. No pressure, no judging.',
  },
  {
    n: '03',
    title: 'Keep it forever',
    body: 'Every answer becomes a permanent memory on your profile — and part of your school\u2019s shared archive.',
  },
];

export default function LandingPage() {
  return (
    <div>
      {/* Hero — editorial, no gradient */}
      <section className="border-b border-line">
        <div className="mx-auto max-w-6xl px-5 pb-20 pt-16 sm:pt-24">
          <p className="label mb-6">A 30-day challenge · A permanent archive</p>
          <h1 className="max-w-4xl font-serif text-5xl font-medium leading-[1.05] tracking-tight sm:text-7xl">
            The stories we told in school,
            <span className="italic text-clay"> kept forever.</span>
          </h1>
          <p className="mt-8 max-w-xl text-lg leading-8 text-ink/60">
            One question a day for thirty days. Reconnect with your classmates, relive the moments, and leave a record
            your school can keep for years.
          </p>
          <div className="mt-10 flex flex-wrap items-center gap-4">
            <Link href="/register" className="btn-primary !px-7 !py-3.5 !text-base">
              Join your school
            </Link>
            <Link href="/login" className="btn-outline !px-7 !py-3.5 !text-base">
              I have an account
            </Link>
          </div>
          <div className="mt-16 flex flex-wrap gap-x-10 gap-y-3 border-t border-line pt-8 text-sm text-ink/50">
            <span>30 questions</span>
            <span>1 answer a day</span>
            <span>Likes, comments & friends</span>
            <span>A digital yearbook for your school</span>
          </div>
        </div>
      </section>

      {/* Sample questions */}
      <section className="border-b border-line">
        <div className="mx-auto grid max-w-6xl gap-12 px-5 py-20 lg:grid-cols-[1fr_1.4fr]">
          <div>
            <p className="label mb-4">The questions</p>
            <h2 className="font-serif text-3xl font-medium leading-snug sm:text-4xl">
              Not a quiz. A conversation with the people you grew up with.
            </h2>
            <p className="mt-5 max-w-md text-base leading-7 text-ink/55">
              Day 21 is optional. Day 30 is a letter to the person you were. The questions are only the mechanism —
              the memories are the point.
            </p>
          </div>
          <ol className="divide-y divide-line border-y border-line">
            {SAMPLE_QUESTIONS.map((q) => (
              <li key={q.day} className="flex items-baseline gap-5 py-4">
                <span className="font-serif text-lg font-semibold text-clay">Day {String(q.day).padStart(2, '0')}</span>
                <span className="font-serif text-xl text-ink/85">{q.text}</span>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* How it works */}
      <section>
        <div className="mx-auto max-w-6xl px-5 py-20">
          <p className="label mb-4">How it works</p>
          <div className="grid gap-12 md:grid-cols-3">
            {STEPS.map((s) => (
              <div key={s.n} className="border-t-2 border-ink pt-5">
                <p className="font-serif text-sm font-semibold text-clay">{s.n}</p>
                <h3 className="mt-2 font-serif text-2xl font-medium">{s.title}</h3>
                <p className="mt-3 text-sm leading-7 text-ink/55">{s.body}</p>
              </div>
            ))}
          </div>

          <blockquote className="mx-auto mt-24 max-w-2xl border-t border-line pt-12 text-center">
            <p className="font-serif text-2xl italic leading-relaxed text-ink/80 sm:text-3xl">
              “We thought our school days would be forgotten. Then thirty questions brought everyone back.”
            </p>
            <footer className="label mt-6">A classmate, probably</footer>
          </blockquote>

          <div className="mt-20 flex flex-col items-center gap-4 border border-line bg-paper-deep/60 px-6 py-14 text-center">
            <h2 className="font-serif text-3xl font-medium">Your school is waiting.</h2>
            <p className="max-w-md text-sm leading-6 text-ink/55">
              If your school isn&apos;t here yet, an admin can add it in minutes. Start with your own memories.
            </p>
            <Link href="/register" className="btn-primary mt-2">
              Start the challenge
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
