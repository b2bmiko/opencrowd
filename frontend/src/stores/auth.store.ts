import { create } from 'zustand';
import { type OidcUser, getUser, login, logout, handleCallback } from '@/lib/auth';

interface AuthState {
  user: OidcUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  // Actions
  initialize: () => Promise<void>;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  handleCallback: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  error: null,

  initialize: async () => {
    try {
      const user = await getUser();
      set({
        user,
        isAuthenticated: !!user && !user.expired,
        isLoading: false,
        error: null,
      });
    } catch (error) {
      console.error('Auth initialization error:', error);
      set({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null, // Don't show error on first load — no user is normal
      });
    }
  },

  login: async () => {
    try {
      await login();
      // If we get here without redirect, something went wrong
    } catch (error) {
      console.error('Login error:', error);
      set({ isLoading: false, error: 'Unable to connect to identity provider' });
    }
  },

  logout: async () => {
    try {
      await logout();
      set({ user: null, isAuthenticated: false });
    } catch (error) {
      // Clear state even if Keycloak logout fails
      set({ user: null, isAuthenticated: false, error: 'Logout failed' });
    }
  },

  handleCallback: async () => {
    try {
      set({ isLoading: true, error: null });
      console.log('[Auth] Starting callback handling...');
      const user = await handleCallback();
      console.log('[Auth] Token exchange successful, user:', user.profile.preferred_username);
      console.log('[Auth] Storing in sessionStorage...');
      // Verify storage worked
      const stored = sessionStorage.getItem('opencrowd_auth');
      console.log('[Auth] SessionStorage after save:', stored ? 'OK' : 'FAILED');
      set({
        user,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    } catch (error) {
      console.error('[Auth] Callback failed:', error);
      set({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Authentication callback failed',
      });
    }
  },
}));
