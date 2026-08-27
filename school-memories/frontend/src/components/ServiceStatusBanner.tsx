'use client';

import { useEffect, useState } from 'react';

type ServiceEvent = { message?: string };

export default function ServiceStatusBanner() {
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const onUnavailable = (event: Event) => {
      const detail = (event as CustomEvent<ServiceEvent>).detail;
      setMessage(detail?.message || 'The service is temporarily unavailable. Please try again in a moment.');
    };
    const onOnline = () => setMessage(null);

    window.addEventListener('keepsake:service-unavailable', onUnavailable);
    window.addEventListener('online', onOnline);
    return () => {
      window.removeEventListener('keepsake:service-unavailable', onUnavailable);
      window.removeEventListener('online', onOnline);
    };
  }, []);

  if (!message) return null;

  return (
    <div role="alert" className="border-b border-clay/30 bg-clay/10 px-5 py-3 text-center text-sm text-ink">
      {message}{' '}
      <button type="button" onClick={() => window.location.reload()} className="font-medium underline underline-offset-4">
        Try again
      </button>
    </div>
  );
}
