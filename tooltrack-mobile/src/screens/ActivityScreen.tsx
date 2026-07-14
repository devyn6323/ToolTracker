import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { EmptyState, ErrorBanner, formatDate, Screen } from '../components/ui';
import { colors } from '../theme';
import { ActivityItem, AppStackParams } from '../types';

export function ActivityScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParams>>();
  const { session } = useAuth();
  const [items, setItems] = useState<ActivityItem[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setRefreshing(true); setError('');
    try { setItems(await request<ActivityItem[]>('/api/activity', {}, session?.token)); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not load activity.'); }
    finally { setRefreshing(false); }
  }, [session?.token]);
  useFocusEffect(useCallback(() => { load(); }, [load]));

  return <Screen scroll={false} style={{ paddingBottom: 0 }}>
    <ErrorBanner message={error} />
    <FlatList data={items} keyExtractor={item => item.id}
      contentContainerStyle={{ gap: 10, flexGrow: 1, paddingBottom: 30 }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={load} tintColor={colors.orange} />}
      renderItem={({ item }) => <Pressable style={styles.item} onPress={() => navigation.navigate('ToolDetail', { toolId: item.toolId })}>
        <View style={[styles.line, { backgroundColor: item.returnedAt ? colors.success : colors.blue }]} />
        <View style={{ flex: 1 }}>
          <Text style={styles.title}>{item.transactionType === 'TRANSFER' ? 'Transferred' : item.returnedAt ? 'Returned' : 'Checked out'} · {item.toolName}</Text>
          <Text style={styles.meta}>{item.user.name} · {item.jobName || item.location || 'No job or location'}</Text>
          <Text style={styles.date}>{formatDate(item.occurredAt, true)}</Text>
        </View><Text style={styles.arrow}>›</Text>
      </Pressable>}
      ListEmptyComponent={<EmptyState title="No activity yet" message="Checkout, transfer, and return activity will appear here." />} />
  </Screen>;
}

const styles = StyleSheet.create({
  item: { flexDirection: 'row', gap: 12, alignItems: 'center', backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, borderRadius: 16, padding: 14, overflow: 'hidden' },
  line: { position: 'absolute', left: 0, top: 0, bottom: 0, width: 4 }, title: { color: colors.ink, fontWeight: '800' },
  meta: { color: colors.muted, marginTop: 4, fontSize: 12 }, date: { color: '#99A3A8', fontSize: 11, marginTop: 7 }, arrow: { color: '#A2AAAE', fontSize: 26 },
});
