import { useEffect, useRef } from 'react';
import { useAuthStore } from '@/stores/auth.store';

export function CallbackPage() {
  const { handleCallback, isAuthenticated, error } = useAuthStore();
  const processed = useRef(false);

  useEffect(() => {
    // Only process once to avoid double-execution in StrictMode
    if (processed.current) return;
    
    const params = new URLSearchParams(window.location.search);
    if (params.get('code')) {
      processed.current = true;
      console.log('[Callback] Code found, processing...');
      handleCallback();
    } else {
      console.log('[Callback] No code in URL, redirecting to home');
      window.location.replace('/');
    }
  }, [handleCallback]);

  // Redirect to home after successful auth
  useEffect(() => {
    if (isAuthenticated) {
      console.log('[Callback] Authenticated, redirecting to dashboard...');
      // Small delay to ensure sessionStorage write is complete
      setTimeout(() => {
        window.location.replace('/');
      }, 100);
    }
  }, [isAuthenticated]);

  if (error) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-center">
          <h1 className="text-xl font-semibold text-destructive">Authentication Failed</h1>
          <p className="mt-2 text-muted-foreground">{error}</p>
          <a href="/" className="mt-4 inline-block text-primary hover:underline">
            Return to login
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-full items-center justify-center">
      <div className="text-center">
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        <p className="mt-4 text-muted-foreground">Completing authentication...</p>
      </div>
    </div>
  );
}
