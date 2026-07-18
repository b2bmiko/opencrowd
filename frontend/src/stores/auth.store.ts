import { create } from 'zustand';
import { type OidcUser, getUser, login, logout, handleCallback, refreshAccessToken } from '@/lib/auth';

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
      console.log('[Auth] Initializing...');
      const user = await getUser();
      console.log('[Auth] getUser result:', user ? `found (${user.profile.preferred_username})` : 'null');
      set({
        user,
        isAuthenticated: !!user && !user.expired,
        isLoading: false,
        error: null,
      });

      // Listen for session expiry events (fired by silent refresh failure)
      window.addEventListener('auth:session-expired', () => {
        set({ user: null, isAuthenticated: false, error: 'Session expired. Please log in again.' });
      });
    } catch (error) {
      console.error('[Auth] Initialize error:', error);
      set({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      });
    }
  },

  login: async () => {
    try {
      await login();
    } catch (error) {
      console.error('[Auth] Login error:', error);
      set({ isLoading: false, error: 'Unable to connect to identity provider' });
    }
  },

  logout: async () => {
    try {
      await logout();
      set({ user: null, isAuthenticated: false });
    } catch (error) {
      set({ user: null, isAuthenticated: false, error: 'Logout failed' });
    }
  },

  handleCallback: async () => {
    try {
      set({ isLoading: true, error: null });
      console.log('[Auth] Starting token exchange...');
      const user = await handleCallback();
      console.log('[Auth] Token exchange SUCCESS:', user.profile.preferred_username);
      
      set({
        user,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    } catch (error) {
      console.error('[Auth] Callback FAILED:', error);
      set({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Authentication callback failed',
      });
    }
  },
}));
