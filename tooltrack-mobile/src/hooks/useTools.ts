import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useState } from 'react';
import { request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Tool } from '../types';

export function useTools(path = '/api/tools') {
  const { session } = useAuth();
  const [tools, setTools] = useState<Tool[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async (refresh = false) => {
    refresh ? setRefreshing(true) : setLoading(true); setError('');
    try { setTools(await request<Tool[]>(path, {}, session?.token)); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not load tools.'); }
    finally { setLoading(false); setRefreshing(false); }
  }, [path, session?.token]);

  useFocusEffect(useCallback(() => { load(); }, [load]));
  return { tools, loading, refreshing, error, reload: () => load(true) };
}
