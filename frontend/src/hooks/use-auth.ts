import { useAuthStore } from '@/stores/auth.store';

/**
 * Hook exposing auth state and actions.
 * Use this in components instead of accessing the store directly.
 */
export function useAuth() {
  const { user, isAuthenticated, isLoading, error, login, logout } = useAuthStore();

  const displayName = user?.profile?.name || user?.profile?.preferred_username || 'User';
  const email = user?.profile?.email || '';
  const initials = displayName
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  // Extract tenant_id from token claims
  const tenantId = (user?.profile as Record<string, unknown>)?.tenant_id as string | undefined;

  // Extract roles from token
  const realmRoles = (user?.profile as Record<string, unknown>)?.realm_access as
    | { roles?: string[] }
    | undefined;
  const roles = realmRoles?.roles || [];

  return {
    user,
    isAuthenticated,
    isLoading,
    error,
    displayName,
    email,
    initials,
    tenantId,
    roles,
    login,
    logout,
    hasRole: (role: string) => roles.includes(role),
  };
}
