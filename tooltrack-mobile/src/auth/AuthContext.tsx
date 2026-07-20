import * as SecureStore from 'expo-secure-store';
import { createContext, PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react';
import { request } from '../api/client';
import { AuthResponse, GoogleAuthResponse } from '../types';
import { currentGoogleIdToken, forgetGoogleSession, startGoogleSignIn } from './googleAuth';

const SESSION_KEY = 'tooltrack.session';

interface AuthContextValue {
  session: AuthResponse | null;
  restoring: boolean;
  login(email: string, password: string): Promise<void>;
  register(companyName: string, name: string, email: string, password: string): Promise<void>;
  googleLogin(): Promise<GoogleAuthResponse>;
  googleCreateCompany(companyName: string): Promise<void>;
  googleReauthenticationToken(): Promise<string>;
  changeGoogleAccount(): Promise<void>;
  changePassword(currentPassword: string, newPassword: string): Promise<void>;
  transferOwnership(targetUserId: string, confirmation: { password?: string; googleIdToken?: string }): Promise<void>;
  logout(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthResponse | null>(null);
  const [restoring, setRestoring] = useState(true);

  useEffect(() => {
    SecureStore.getItemAsync(SESSION_KEY)
      .then(value => value && setSession(JSON.parse(value)))
      .finally(() => setRestoring(false));
  }, []);

  async function persist(next: AuthResponse) {
    setSession(next);
    await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(next));
  }

  const value = useMemo<AuthContextValue>(() => ({
    session,
    restoring,
    async login(email, password) {
      const response = await request<AuthResponse>('/api/auth/login', {
        method: 'POST', body: JSON.stringify({ email, password }),
      });
      await persist(response);
    },
    async register(companyName, name, email, password) {
      const response = await request<AuthResponse>('/api/auth/register', {
        method: 'POST', body: JSON.stringify({ companyName, name, email, password }),
      });
      await persist(response);
    },
    async googleLogin() {
      const idToken = await startGoogleSignIn();
      const response = await request<GoogleAuthResponse>('/api/auth/google', {
        method: 'POST', body: JSON.stringify({ idToken }),
      });
      if (response.session) await persist(response.session);
      return response;
    },
    async googleCreateCompany(companyName) {
      const idToken = await currentGoogleIdToken();
      const response = await request<GoogleAuthResponse>('/api/auth/google', {
        method: 'POST', body: JSON.stringify({ idToken, companyName }),
      });
      if (!response.session) throw new Error('Google company setup did not complete.');
      await persist(response.session);
    },
    googleReauthenticationToken: currentGoogleIdToken,
    async changeGoogleAccount() { await forgetGoogleSession(); },
    async changePassword(currentPassword, newPassword) {
      if (!session) throw new Error('Sign in again to change your password.');
      const response = await request<AuthResponse>('/api/auth/password', {
        method: 'PUT', body: JSON.stringify({ currentPassword, newPassword }),
      }, session.token);
      await persist(response);
    },
    async transferOwnership(targetUserId, confirmation) {
      if (!session) throw new Error('Sign in again to transfer ownership.');
      const response = await request<AuthResponse>(`/api/auth/ownership/${targetUserId}`, {
        method: 'PUT', body: JSON.stringify(confirmation),
      }, session.token);
      await persist(response);
    },
    async logout() {
      setSession(null);
      await SecureStore.deleteItemAsync(SESSION_KEY);
      try { await forgetGoogleSession(); } catch { /* Local sign-out must still succeed if Google is unavailable. */ }
    },
  }), [session, restoring]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
