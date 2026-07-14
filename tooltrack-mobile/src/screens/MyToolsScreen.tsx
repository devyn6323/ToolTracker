import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { FlatList, RefreshControl } from 'react-native';
import { EmptyState, ErrorBanner, Screen } from '../components/ui';
import { ToolCard } from '../components/ToolCard';
import { useTools } from '../hooks/useTools';
import { colors } from '../theme';
import { AppStackParams, TabParams } from '../types';
type Nav = CompositeNavigationProp<BottomTabNavigationProp<TabParams, 'MyTools'>, NativeStackNavigationProp<AppStackParams>>;
export function MyToolsScreen() {
  const navigation = useNavigation<Nav>(); const { tools, refreshing, error, reload } = useTools('/api/tools/my-tools');
  return <Screen scroll={false} style={{ paddingBottom: 0 }}><ErrorBanner message={error} /><FlatList data={tools} keyExtractor={item => item.id} contentContainerStyle={{ gap: 10, flexGrow: 1, paddingBottom: 30 }} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={reload} tintColor={colors.orange} />} renderItem={({ item }) => <ToolCard tool={item} onPress={() => navigation.navigate('ToolDetail', { toolId: item.id })} />} ListEmptyComponent={<EmptyState title="Nothing checked out" message="Tools you check out will appear here until they’re returned." />} /></Screen>;
}
