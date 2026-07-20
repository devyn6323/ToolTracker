import {
  GoogleOneTapSignIn,
  isCancelledResponse,
  isNoSavedCredentialFoundResponse,
  isSuccessResponse,
} from 'react-native-nitro-google-signin';

let configured = false;

function configureGoogle() {
  const webClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID;
  if (!webClientId) throw new Error('Google sign-in is not configured in this build.');
  if (!configured) {
    GoogleOneTapSignIn.configure({ webClientId, offlineAccess: false, autoSelectOnSignIn: false });
    configured = true;
  }
}

export async function startGoogleSignIn() {
  configureGoogle();
  await GoogleOneTapSignIn.checkPlayServices(true);
  let response = await GoogleOneTapSignIn.signIn();
  if (isNoSavedCredentialFoundResponse(response)) response = await GoogleOneTapSignIn.createAccount();
  if (isNoSavedCredentialFoundResponse(response)) response = await GoogleOneTapSignIn.presentExplicitSignIn();
  if (isCancelledResponse(response)) throw new Error('Google sign-in was cancelled.');
  if (!isSuccessResponse(response)) throw new Error('Google sign-in did not complete.');
  return response.data.idToken;
}

export async function currentGoogleIdToken() {
  configureGoogle();
  try {
    return (await GoogleOneTapSignIn.getTokens()).idToken;
  } catch {
    return startGoogleSignIn();
  }
}

export async function forgetGoogleSession() {
  if (!configured) return;
  await GoogleOneTapSignIn.signOut();
}
