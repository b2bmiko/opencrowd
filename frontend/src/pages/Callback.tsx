import { useEffect } from 'react';
import { useAuthStore } from '@/stores/auth.store';

export function CallbackPage() {
  const { handleCallback, isAuthenticated, error } = useAuthStore();

  useEffect(() => {
    // Only process callback if we have a code in the URL
    const params = new URLSearchParams(window.location.search);
    if (params.get('code')) {
      handleCallback();
    }
  }, [handleCallback]);

  // Redirect to home after successful auth
  useEffect(() => {
    if (isAuthenticated) {
      // Use replace so back button doesn't return to callback
      window.location.replace('/');
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
