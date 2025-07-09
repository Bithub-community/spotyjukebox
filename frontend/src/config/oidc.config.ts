import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

const oidcConfig = {
  authority: 'https://accounts.spotify.com',
  client_id: '308c73a474314c9bb28fa34f5c63e6e5',
  redirect_uri: 'http://localhost:8080/callback',
  post_logout_redirect_uri: 'http://localhost:5173/',
  response_type: 'code',
  scope: 'user-read-private user-read-email playlist-read-private',
  userStore: new WebStorageStateStore({ store: window.localStorage }),
};

export const userManager = new UserManager(oidcConfig);