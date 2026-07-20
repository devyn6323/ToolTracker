import { Platform } from 'react-native';

const fallbackUrl = Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
export const API_URL = (process.env.EXPO_PUBLIC_API_URL || (__DEV__ ? fallbackUrl : '')).replace(/\/$/, '');

function ensureConfigured() {
  if (!API_URL) throw new ApiError('This build is missing its production API configuration.', 0);
  if (!__DEV__ && !API_URL.startsWith('https://')) {
    throw new ApiError('Production API connections must use HTTPS.', 0);
  }
}

export function assetUrl(url?: string) {
  if (!url) return undefined;
  return /^https?:\/\//i.test(url) ? url : `${API_URL}${url.startsWith('/') ? '' : '/'}${url}`;
}

export class ApiError extends Error {
  constructor(message: string, public status: number, public fields?: Record<string, string>) {
    super(message);
  }
}

export async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  ensureConfigured();
  let response: Response;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 75_000);
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch (error) {
    throw new ApiError(error instanceof Error && error.name === 'AbortError'
      ? 'ToolTrack took too long to respond. Check your connection and try again.'
      : 'Could not reach ToolTrack. Check your connection and try again.', 0);
  } finally {
    clearTimeout(timeout);
  }

  const text = await response.text();
  let data: any;
  try { data = text ? JSON.parse(text) : undefined; }
  catch { data = text ? { detail: text } : undefined; }
  if (!response.ok) {
    throw new ApiError(data?.detail || data?.title || 'Something went wrong', response.status, data?.errors);
  }
  return data as T;
}

export async function uploadToolPhoto(asset: { uri: string; fileName?: string | null; mimeType?: string | null }, token: string) {
  ensureConfigured();
  const form = new FormData();
  form.append('photo', {
    uri: asset.uri,
    name: asset.fileName || `tool-${Date.now()}.jpg`,
    type: asset.mimeType || 'image/jpeg',
  } as unknown as Blob);
  let response: Response;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 120_000);
  try {
    response = await fetch(`${API_URL}/api/uploads/tool-photo`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      body: form,
      signal: controller.signal,
    });
  } catch (error) {
    throw new ApiError(error instanceof Error && error.name === 'AbortError'
      ? 'The photo upload took too long. Check your connection and try again.'
      : 'Could not upload the photo. Check your connection and try again.', 0);
  } finally {
    clearTimeout(timeout);
  }
  const text = await response.text();
  let data: any;
  try { data = text ? JSON.parse(text) : {}; }
  catch { data = {}; }
  if (!response.ok) throw new ApiError(data?.detail || 'Could not upload photo', response.status);
  return data.url as string;
}

export async function deleteToolPhoto(url: string, token: string) {
  await request<void>('/api/uploads/tool-photo', {
    method: 'DELETE', body: JSON.stringify({ url }),
  }, token);
}
