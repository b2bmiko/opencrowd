/**
 * OIDC auth implementation using authorization code flow with PKCE.
 * Uses crypto.subtle for code verifier/challenge (requires secure context or localhost).
 * Falls back to plain code flow if crypto.subtle is unavailable.
 */

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180';
const REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'opencrowd';
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'opencrowd-frontend';
const REDIRECT_URI = `${window.location.origin}/callback`;
const POST_LOGOUT_URI = window.location.origin;

const AUTH_ENDPOINT = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth`;
const TOKEN_ENDPOINT = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
const LOGOUT_ENDPOINT = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`;

export interface OidcUser {
  access_token: string;
  refresh_token?: string;
  id_token?: string;
  expires_at: number;
  expired: boolean;
  profile: {
    sub: string;
    name?: string;
    preferred_username?: string;
    email?: string;
    tenant_id?: string;
    realm_access?: { roles: string[] };
  };
}

const TOKEN_KEY = 'opencrowd_auth';

// Refresh buffer: refresh token 60 seconds before expiry
const REFRESH_BUFFER_MS = 60 * 1000;
let refreshTimer: ReturnType<typeof setTimeout> | null = null;

// --- PKCE helpers ---

function generateCodeVerifier(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64UrlEncode(array);
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(hash));
}

function base64UrlEncode(buffer: Uint8Array): string {
  let str = '';
  buffer.forEach((b) => (str += String.fromCharCode(b)));
  return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function isPkceAvailable(): boolean {
  return typeof crypto !== 'undefined' && typeof crypto.subtle !== 'undefined';
}

// --- Auth functions ---

export async function login(): Promise<void> {
  const state = Math.random().toString(36).substring(2);
  sessionStorage.setItem('auth_state', state);

  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    response_type: 'code',
    scope: 'openid profile email',
    state: state,
  });

  // Use PKCE if available (secure context)
  if (isPkceAvailable()) {
    const codeVerifier = generateCodeVerifier();
    const codeChallenge = await generateCodeChallenge(codeVerifier);
    sessionStorage.setItem('code_verifier', codeVerifier);
    params.set('code_challenge', codeChallenge);
    params.set('code_challenge_method', 'S256');
  }

  window.location.href = `${AUTH_ENDPOINT}?${params.toString()}`;
}

export async function handleCallback(): Promise<OidcUser> {
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');
  const state = params.get('state');
  const storedState = sessionStorage.getItem('auth_state');

  if (!code) {
    throw new Error('No authorization code received');
  }

  // Validate state
  if (storedState && state !== storedState) {
    throw new Error('State mismatch — possible CSRF attack');
  }

  sessionStorage.removeItem('auth_state');

  // Build token request body
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    code: code,
  });

  // Include PKCE code_verifier if we used it
  const codeVerifier = sessionStorage.getItem('code_verifier');
  if (codeVerifier) {
    body.set('code_verifier', codeVerifier);
    sessionStorage.removeItem('code_verifier');
  }

  // Exchange code for tokens
  const tokenResponse = await fetch(TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body,
  });

  if (!tokenResponse.ok) {
    const errorData = await tokenResponse.text();
    throw new Error(`Token exchange failed: ${errorData}`);
  }

  const tokens = await tokenResponse.json();
  const user = tokensToUser(tokens);

  localStorage.setItem(TOKEN_KEY, JSON.stringify(user));
  scheduleRefresh(user);

  return user;
}

/**
 * Silent token refresh using the stored refresh_token.
 * Returns the new user or null if refresh failed (user must re-login).
 */
export async function refreshAccessToken(): Promise<OidcUser | null> {
  const stored = localStorage.getItem(TOKEN_KEY);
  if (!stored) return null;

  const current: OidcUser = JSON.parse(stored);
  if (!current.refresh_token) return null;

  try {
    const tokenResponse = await fetch(TOKEN_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: CLIENT_ID,
        refresh_token: current.refresh_token,
      }),
    });

    if (!tokenResponse.ok) {
      console.warn('[Auth] Token refresh failed, user must re-login');
      localStorage.removeItem(TOKEN_KEY);
      return null;
    }

    const tokens = await tokenResponse.json();
    const user = tokensToUser(tokens);

    localStorage.setItem(TOKEN_KEY, JSON.stringify(user));
    scheduleRefresh(user);

    console.log('[Auth] Token refreshed silently');
    return user;
  } catch (e) {
    console.error('[Auth] Refresh error:', e);
    localStorage.removeItem(TOKEN_KEY);
    return null;
  }
}

export async function logout(): Promise<void> {
  clearRefreshTimer();
  const stored = localStorage.getItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_KEY);

  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    post_logout_redirect_uri: POST_LOGOUT_URI,
  });

  if (stored) {
    const user = JSON.parse(stored);
    if (user.id_token) {
      params.set('id_token_hint', user.id_token);
    }
  }

  window.location.href = `${LOGOUT_ENDPOINT}?${params.toString()}`;
}

export async function getUser(): Promise<OidcUser | null> {
  const stored = localStorage.getItem(TOKEN_KEY);
  if (!stored) return null;

  const user: OidcUser = JSON.parse(stored);
  user.expired = Date.now() > user.expires_at;

  if (user.expired) {
    // Try silent refresh before declaring expired
    const refreshed = await refreshAccessToken();
    return refreshed;
  }

  // Schedule refresh if not already scheduled
  scheduleRefresh(user);

  return user;
}

export async function getAccessToken(): Promise<string | null> {
  const user = await getUser();
  return user?.access_token || null;
}

// --- Internal helpers ---

function tokensToUser(tokens: Record<string, unknown>): OidcUser {
  const payload = decodeJwtPayload(tokens.access_token as string);

  return {
    access_token: tokens.access_token as string,
    refresh_token: tokens.refresh_token as string | undefined,
    id_token: tokens.id_token as string | undefined,
    expires_at: Date.now() + (tokens.expires_in as number) * 1000,
    expired: false,
    profile: {
      sub: payload.sub as string,
      name: payload.name as string | undefined,
      preferred_username: payload.preferred_username as string | undefined,
      email: payload.email as string | undefined,
      tenant_id: payload.tenant_id as string | undefined,
      realm_access: payload.realm_access as { roles: string[] } | undefined,
    },
  };
}

function scheduleRefresh(user: OidcUser): void {
  clearRefreshTimer();
  if (!user.refresh_token) return;

  const msUntilExpiry = user.expires_at - Date.now();
  const refreshIn = Math.max(msUntilExpiry - REFRESH_BUFFER_MS, 5000); // At least 5s

  refreshTimer = setTimeout(async () => {
    const refreshed = await refreshAccessToken();
    if (!refreshed) {
      // Refresh failed — notify the app
      window.dispatchEvent(new CustomEvent('auth:session-expired'));
    }
  }, refreshIn);
}

function clearRefreshTimer(): void {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('Invalid JWT');
  const payload = parts[1];
  const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(decoded);
}
