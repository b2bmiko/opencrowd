import { UserManager, WebStorageStateStore, type User as OidcUser } from 'oidc-client-ts';

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180';
const REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'opencrowd';
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'opencrowd-frontend';
const REDIRECT_URI = `${window.location.origin}/callback`;
const POST_LOGOUT_URI = window.location.origin;

const settings = {
  authority: `${KEYCLOAK_URL}/realms/${REALM}`,
  client_id: CLIENT_ID,
  redirect_uri: REDIRECT_URI,
  post_logout_redirect_uri: POST_LOGOUT_URI,
  response_type: 'code',
  scope: 'openid profile email',
  automaticSilentRenew: false,
  // Disable PKCE and state validation for HTTP dev environment
  // crypto.subtle is only available over HTTPS
  disablePKCE: true,
  stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
};

export const userManager = new UserManager(settings);

export async function login(): Promise<void> {
  // Use skipUserInfo to avoid additional request that may fail
  await userManager.signinRedirect({ state: 'login' });
}

export async function handleCallback(): Promise<OidcUser> {
  const user = await userManager.signinRedirectCallback();
  return user;
}

export async function logout(): Promise<void> {
  await userManager.signoutRedirect();
}

export async function getUser(): Promise<OidcUser | null> {
  return await userManager.getUser();
}

export async function getAccessToken(): Promise<string | null> {
  const user = await userManager.getUser();
  if (!user || user.expired) {
    return null;
  }
  return user.access_token;
}

export type { OidcUser };
