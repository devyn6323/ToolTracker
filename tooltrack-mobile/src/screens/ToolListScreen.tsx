import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMemo, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, Text, TextInput, View } from 'react-native';
import { useAuth } from '../auth/AuthContext';
import { Button, EmptyState, ErrorBanner, Screen, SelectChips } from '../components/ui';
import { ToolCard } from '../components/ToolCard';
import { useTools } from '../hooks/useTools';
import { colors } from '../theme';
import { AppStackParams, TabParams, ToolStatus } from '../types';

type Nav = CompositeNavigationProp<BottomTabNavigationProp<TabParams, 'Inventory'>, NativeStackNavigationProp<AppStackParams>>;
const filters = ['ALL', 'AVAILABLE', 'CHECKED_OUT', 'OVERDUE'] as const;

export function ToolListScreen() {
  const navigation = useNavigation<Nav>(); const { session } = useAuth();
  const { tools, refreshing, error, reload } = useTools(); const [search, setSearch] = useState(''); const [filter, setFilter] = useState<typeof filters[number]>('ALL');
  const shown = useMemo(() => tools.filter(tool => (filter === 'ALL' || tool.status === filter) && [tool.name, tool.assetNumber, tool.category, tool.serialNumber].some(value => value?.toLowerCase().includes(search.toLowerCase()))), [tools, filter, search]);
  return <Screen scroll={false} style={{ paddingBottom: 0 }}>
    <View style={styles.top}><TextInput value={search} onChangeText={setSearch} placeholder="Search tools or asset #" placeholderTextColor="#8D999F" style={styles.search} />
      {session?.user.role !== 'EMPLOYEE' && <Text style={styles.add} onPress={() => navigation.navigate('ToolForm')}>＋</Text>}</View>
    <SelectChips label="Status" value={filter} options={filters} onChange={value => setFilter(value as typeof filter)} /><ErrorBanner message={error} />
    <Button title="Check out multiple tools" variant="secondary" onPress={() => navigation.navigate('BatchCheckout')} />
    <FlatList data={shown} keyExtractor={item => item.id} contentContainerStyle={{ gap: 10, paddingBottom: 30, flexGrow: 1 }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={reload} tintColor={colors.orange} />}
      renderItem={({ item }) => <ToolCard tool={item} onPress={() => navigation.navigate('ToolDetail', { toolId: item.id })} />}
      ListEmptyComponent={<EmptyState title="No tools found" message={search || filter !== 'ALL' ? 'Try another search or status.' : 'Add your first tool to start tracking it.'} />} />
  </Screen>;
}
const styles = StyleSheet.create({ top: { flexDirection: 'row', gap: 10 }, search: { flex: 1, height: 50, borderRadius: 14, backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, paddingHorizontal: 15, fontSize: 15, color: colors.ink }, add: { width: 50, height: 50, lineHeight: 48, textAlign: 'center', borderRadius: 14, overflow: 'hidden', backgroundColor: colors.orange, color: '#fff', fontSize: 28, fontWeight: '600' } });
