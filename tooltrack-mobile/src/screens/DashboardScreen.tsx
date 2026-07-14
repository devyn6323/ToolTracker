import { CompositeNavigationProp, useFocusEffect, useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBanner, formatDate, Screen, SectionTitle } from '../components/ui';
import { ToolCard } from '../components/ToolCard';
import { useTools } from '../hooks/useTools';
import { colors } from '../theme';
import { AppStackParams, DashboardData, TabParams, ToolStatus } from '../types';

type Nav = CompositeNavigationProp<BottomTabNavigationProp<TabParams, 'Home'>, NativeStackNavigationProp<AppStackParams>>;

export function DashboardScreen() {
  const navigation = useNavigation<Nav>();
  const { session, logout } = useAuth();
  const { tools, error } = useTools();
  const [dashboard, setDashboard] = useState<DashboardData>();
  const [dashboardError, setDashboardError] = useState('');
  const loadDashboard = useCallback(() => request<DashboardData>('/api/dashboard', {}, session?.token)
    .then(setDashboard).catch(e => setDashboardError(e instanceof Error ? e.message : 'Could not load dashboard.')), [session?.token]);
  useFocusEffect(useCallback(() => { loadDashboard(); }, [loadDashboard]));
  const counts = (status: ToolStatus) => dashboard?.counts[status] ?? tools.filter(tool => tool.status === status).length;
  const attention = tools.filter(tool => ['OVERDUE', 'DAMAGED', 'LOST'].includes(tool.status));

  return <Screen>
    <View style={styles.welcome}><View><Text style={styles.eyebrow}>{session?.companyName}</Text><Text style={styles.title}>Good to see you,{`\n`}{session?.user.name.split(' ')[0]}.</Text></View><Button title="Settings" variant="ghost" onPress={() => navigation.navigate('Settings')} style={{ minHeight: 40, paddingHorizontal: 8 }} /></View>
    <ErrorBanner message={error || dashboardError} />
    <View style={styles.stats}>
      <Stat label="Available" value={counts('AVAILABLE')} color={colors.success} />
      <Stat label="Out now" value={counts('CHECKED_OUT')} color={colors.blue} />
      <Stat label="Overdue" value={counts('OVERDUE')} color={colors.warning} />
      <Stat label="Issues" value={counts('DAMAGED') + counts('LOST')} color={colors.danger} />
    </View>
    <Card style={styles.scanCard}>
      <View style={styles.scanMark}><Text style={styles.scanSymbol}>⌗</Text></View><View style={{ flex: 1 }}><Text style={styles.scanTitle}>Scan a tool</Text><Text style={styles.scanText}>Check out or return in seconds.</Text></View>
      <Button title="Scan" onPress={() => navigation.navigate('Scan')} style={{ minHeight: 44 }} />
    </Card>
    {session?.user.role !== 'EMPLOYEE' && <View style={styles.quickRow}>
      <Button title="Add tool" variant="secondary" onPress={() => navigation.navigate('ToolForm')} style={{ flex: 1 }} />
      <Button title="Employees" variant="secondary" onPress={() => navigation.navigate('Employees')} style={{ flex: 1 }} />
    </View>}
    <SectionTitle title={attention.length ? 'Needs attention' : 'Recently added'} />
    {(attention.length ? attention : tools.slice(0, 3)).map(tool => <ToolCard key={tool.id} tool={tool} onPress={() => navigation.navigate('ToolDetail', { toolId: tool.id })} />)}
    {!tools.length && <Card><Text style={styles.emptyTitle}>Your inventory is ready.</Text><Text style={styles.scanText}>Add your first tool, then print its QR code from the tool details screen.</Text></Card>}
    {!!dashboard?.recentActivity.length && <><SectionTitle title="Recent activity" action={<Text style={styles.link} onPress={() => navigation.navigate('Activity')}>View all</Text>} />
      {dashboard.recentActivity.slice(0, 3).map(item => <Pressable key={item.id} onPress={() => navigation.navigate('ToolDetail', { toolId: item.toolId })}><Card style={styles.activity}><View style={[styles.activityDot, { backgroundColor: item.returnedAt ? colors.success : colors.blue }]} /><View style={{ flex: 1 }}><Text style={styles.activityTitle}>{item.transactionType === 'TRANSFER' ? 'Transferred' : item.returnedAt ? 'Returned' : 'Checked out'} · {item.toolName}</Text><Text style={styles.activityMeta}>{item.user.name} · {formatDate(item.occurredAt, true)}</Text></View></Card></Pressable>)}</>}
  </Screen>;
}

function Stat({ label, value, color }: { label: string; value: number; color: string }) {
  return <Card style={styles.stat}><View style={[styles.statLine, { backgroundColor: color }]} /><Text style={styles.statValue}>{value}</Text><Text style={styles.statLabel}>{label}</Text></Card>;
}
const styles = StyleSheet.create({
  welcome: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' }, eyebrow: { color: colors.orangeDark, textTransform: 'uppercase', letterSpacing: 1.4, fontWeight: '900', fontSize: 10 },
  title: { color: colors.ink, fontSize: 30, lineHeight: 34, fontWeight: '900', marginTop: 5 }, stats: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  stat: { width: '48%', padding: 14, overflow: 'hidden' }, statLine: { position: 'absolute', top: 0, left: 0, bottom: 0, width: 4 }, statValue: { color: colors.ink, fontWeight: '900', fontSize: 27 }, statLabel: { color: colors.muted, fontWeight: '600' },
  scanCard: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.navy, borderColor: colors.navy }, scanMark: { width: 48, height: 48, borderRadius: 15, backgroundColor: '#FFFFFF18', alignItems: 'center', justifyContent: 'center' }, scanSymbol: { color: colors.orange, fontSize: 28 },
  scanTitle: { color: '#fff', fontWeight: '900', fontSize: 17 }, scanText: { color: '#AEBCC4', lineHeight: 20 }, quickRow: { flexDirection: 'row', gap: 10 }, emptyTitle: { color: colors.ink, fontWeight: '900', fontSize: 17, marginBottom: 5 }, link: { color: colors.blue, fontWeight: '800' }, activity: { flexDirection: 'row', alignItems: 'center', gap: 11, padding: 13 }, activityDot: { width: 9, height: 9, borderRadius: 5 }, activityTitle: { color: colors.ink, fontWeight: '800' }, activityMeta: { color: colors.muted, fontSize: 11, marginTop: 3 },
});
