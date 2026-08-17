'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';

interface GsiWindow {
  google?: {
    accounts: {
      id: {
        initialize: (config: { client_id: string; callback: (res: { credential: string }) => void }) => void;
        renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
      };
    };
  };
}

/**
 * "Continue with Google" button. It renders nothing until the backend reports
 * Google sign-in is configured (via /api/auth/oauth-config), then lazily loads
 * the Google Identity Services script and hands the ID token to the backend,
 * which exchanges it for a Keepsake JWT.
 */
export default function GoogleSignInButton() {
  const { googleSignIn } = useAuth();
  const router = useRouter();
  const buttonRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const initialized = useRef(false);

  const { data: config } = useQuery({
    queryKey: ['oauth-config'],
    queryFn: api.oauthConfig,
    staleTime: Infinity,
  });

  const handleCredential = useCallback(
    async (response: { credential: string }) => {
      setError(null);
      setBusy(true);
      try {
        const user = await googleSignIn(response.credential);
        // New Google accounts have no school yet — route them to pick one.
        router.replace(user.schoolId ? '/challenge' : '/onboarding');
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Google sign-in failed');
      } finally {
        setBusy(false);
      }
    },
    [googleSignIn, router],
  );

  useEffect(() => {
    if (!config?.enabled || !config.clientId || initialized.current) return;
    const clientId = config.clientId; // narrowed: non-null after the guard above
    const gsi = window as GsiWindow;
    if (gsi.google?.accounts?.id) {
      render(buttonRef.current, clientId, handleCredential);
      initialized.current = true;
      return;
    }
    let cancelled = false;
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => {
      if (!cancelled) {
        render(buttonRef.current, clientId, handleCredential);
        initialized.current = true;
      }
    };
    document.head.appendChild(script);
    return () => {
      cancelled = true;
    };
  }, [config, handleCredential]);

  if (!config?.enabled || !config.clientId) return null;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <span className="h-px flex-1 bg-line" />
        <span className="text-xs uppercase tracking-widest text-ink/40">or</span>
        <span className="h-px flex-1 bg-line" />
      </div>
      <div ref={buttonRef} className="flex min-h-11 items-center justify-center" />
      {busy && <p className="text-center text-sm text-ink/55">Connecting…</p>}
      {error && <p className="text-center text-sm text-clay">{error}</p>}
    </div>
  );
}

function render(container: HTMLElement | null, clientId: string, callback: (res: { credential: string }) => void) {
  if (!container) return;
  const gsi = window as GsiWindow;
  if (!gsi.google?.accounts?.id) return;
  gsi.google.accounts.id.initialize({ client_id: clientId, callback });
  gsi.google.accounts.id.renderButton(container, {
    theme: 'outline',
    size: 'large',
    shape: 'pill',
    text: 'continue_with',
    width: 320,
  });
}
