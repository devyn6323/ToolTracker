import * as SecureStore from 'expo-secure-store';
import { createContext, PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react';
import { request } from '../api/client';
import { AuthResponse } from '../types';

const SESSION_KEY = 'tooltrack.session';

interface AuthContextValue {
  session: AuthResponse | null;
  restoring: boolean;
  login(email: string, password: string): Promise<void>;
  register(companyName: string, name: string, email: string, password: string): Promise<void>;
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
    async logout() {
      setSession(null);
      await SecureStore.deleteItemAsync(SESSION_KEY);
    },
  }), [session, restoring]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
