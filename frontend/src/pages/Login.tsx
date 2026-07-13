import { useAuth } from '@/hooks/use-auth';

export function LoginPage() {
  const { login, isLoading, error } = useAuth();

  return (
    <div className="flex h-full items-center justify-center bg-background">
      <div className="w-full max-w-md space-y-8 rounded-lg border bg-card p-8 shadow-sm">
        {/* Logo */}
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-lg bg-primary">
            <span className="text-lg font-bold text-primary-foreground">O</span>
          </div>
          <h1 className="mt-4 text-2xl font-bold text-foreground">OpenCrowd</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Identity & Access Governance for the Open Source World
          </p>
        </div>

        {/* Login button */}
        <div className="space-y-4">
          {error && (
            <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>
          )}

          <button
            onClick={() => login()}
            disabled={isLoading}
            className="w-full rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
          >
            {isLoading ? 'Redirecting...' : 'Sign in with Keycloak'}
          </button>

          <p className="text-center text-xs text-muted-foreground">
            Authenticate via your organization's identity provider
          </p>
        </div>
      </div>
    </div>
  );
}
