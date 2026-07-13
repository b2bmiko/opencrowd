import { useEffect } from 'react';
import { useAuthStore } from '@/stores/auth.store';

export function CallbackPage() {
  const { handleCallback, isAuthenticated, error } = useAuthStore();

  useEffect(() => {
    handleCallback();
  }, [handleCallback]);

  // Redirect to home after successful auth
  useEffect(() => {
    if (isAuthenticated) {
      window.location.href = '/';
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
