export default function EmptyState({
  title,
  body,
  action,
}: {
  title: string;
  body: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-3 border border-dashed border-line bg-paper-deep/50 px-6 py-16 text-center">
      <p className="font-serif text-xl text-ink/80">{title}</p>
      <p className="max-w-sm text-sm leading-6 text-ink/50">{body}</p>
      {action}
    </div>
  );
}
