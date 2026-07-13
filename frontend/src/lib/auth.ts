/**
 * Simple OIDC auth implementation using manual redirect flow.
 * Avoids oidc-client-ts dependency on crypto.subtle (requires HTTPS).
 * In production with HTTPS, we'll switch back to oidc-client-ts with PKCE.
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

  if (state !== storedState) {
    throw new Error('State mismatch — possible CSRF attack');
  }

  sessionStorage.removeItem('auth_state');

  // Exchange code for tokens
  const tokenResponse = await fetch(TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: CLIENT_ID,
      redirect_uri: REDIRECT_URI,
      code: code,
    }),
  });

  if (!tokenResponse.ok) {
    const errorData = await tokenResponse.text();
    throw new Error(`Token exchange failed: ${errorData}`);
  }

  const tokens = await tokenResponse.json();

  // Decode the access token payload (JWT)
  const payload = decodeJwtPayload(tokens.access_token);

  const user: OidcUser = {
    access_token: tokens.access_token,
    refresh_token: tokens.refresh_token,
    id_token: tokens.id_token,
    expires_at: Date.now() + tokens.expires_in * 1000,
    expired: false,
    profile: {
      sub: payload.sub,
      name: payload.name,
      preferred_username: payload.preferred_username,
      email: payload.email,
      tenant_id: payload.tenant_id,
      realm_access: payload.realm_access,
    },
  };

  sessionStorage.setItem(TOKEN_KEY, JSON.stringify(user));

  return user;
}

export async function logout(): Promise<void> {
  const stored = sessionStorage.getItem(TOKEN_KEY);
  sessionStorage.removeItem(TOKEN_KEY);

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
  const stored = sessionStorage.getItem(TOKEN_KEY);
  if (!stored) return null;

  const user: OidcUser = JSON.parse(stored);
  user.expired = Date.now() > user.expires_at;

  if (user.expired) {
    sessionStorage.removeItem(TOKEN_KEY);
    return null;
  }

  return user;
}

export async function getAccessToken(): Promise<string | null> {
  const user = await getUser();
  return user?.access_token || null;
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('Invalid JWT');
  const payload = parts[1];
  const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(decoded);
}
