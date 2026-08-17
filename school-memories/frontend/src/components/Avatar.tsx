import { initials } from '@/lib/format';

export default function Avatar({
  name,
  src,
  size = 40,
  className = '',
}: {
  name?: string | null;
  src?: string | null;
  size?: number;
  className?: string;
}) {
  const style = { width: size, height: size, fontSize: Math.max(11, size * 0.36) };
  if (src) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={src}
        alt={name || ''}
        style={style}
        className={`shrink-0 rounded-full object-cover bg-line ${className}`}
      />
    );
  }
  return (
    <div
      style={style}
      aria-hidden
      className={`flex shrink-0 items-center justify-center rounded-full bg-clay/15 font-medium text-clay ${className}`}
    >
      {initials(name)}
    </div>
  );
}
