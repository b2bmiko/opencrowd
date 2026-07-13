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
      set({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: 'Failed to initialize auth',
      });
    }
  },

  login: async () => {
    try {
      set({ isLoading: true, error: null });
      await login();
    } catch (error) {
      set({ isLoading: false, error: 'Login failed' });
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
      const user = await handleCallback();
      set({
        user,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    } catch (error) {
      set({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: 'Authentication callback failed',
      });
    }
  },
}));
