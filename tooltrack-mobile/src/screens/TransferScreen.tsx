import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { ApiError, request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, DateField, ErrorBanner, Input, Screen, SectionTitle, pretty, toInputDate } from '../components/ui';
import { colors } from '../theme';
import { AppStackParams, ToolTransaction, UserSummary } from '../types';

export function TransferScreen({ route, navigation }: NativeStackScreenProps<AppStackParams, 'Transfer'>) {
  const { tool } = route.params; const { session } = useAuth();
  const [employees, setEmployees] = useState<UserSummary[]>([]); const [targetId, setTargetId] = useState('');
  const [location, setLocation] = useState(tool.currentLocation || ''); const [due, setDue] = useState(tool.expectedReturnAt ? toInputDate(new Date(tool.expectedReturnAt)) : '');
  const [notes, setNotes] = useState(''); const [loading, setLoading] = useState(false); const [error, setError] = useState('');

  useEffect(() => { request<UserSummary[]>('/api/employees', {}, session?.token)
    .then(users => setEmployees(users.filter(user => user.active && user.id !== tool.checkedOutTo?.id)))
    .catch(e => setError(e instanceof Error ? e.message : 'Could not load employees.')); }, [session?.token, tool.checkedOutTo?.id]);

  async function submit() {
    if (!targetId) return setError('Choose the employee receiving this tool.');
    let expectedReturnAt: string | null = null;
    if (due) { const date = new Date(`${due}T17:00:00`); if (Number.isNaN(date.getTime())) return setError('Choose a valid due date.'); expectedReturnAt = date.toISOString(); }
    setLoading(true); setError('');
    try { await request<ToolTransaction>(`/api/tools/${tool.id}/transfer`, { method: 'POST', body: JSON.stringify({ targetUserId: targetId, location, expectedReturnAt, notes }) }, session?.token); navigation.replace('ToolDetail', { toolId: tool.id }); }
    catch (e) { setError(e instanceof ApiError ? e.message : 'Could not transfer tool.'); }
    finally { setLoading(false); }
  }

  return <Screen>
    <Card><Text style={styles.asset}>{tool.assetNumber}</Text><Text style={styles.name}>{tool.name}</Text><Text style={styles.meta}>Currently held by {tool.checkedOutTo?.name || 'another employee'}</Text></Card>
    <ErrorBanner message={error} /><SectionTitle title="Transfer to" />
    <View style={styles.people}>{employees.map(employee => <Pressable key={employee.id} onPress={() => setTargetId(employee.id)} style={[styles.person, targetId === employee.id && styles.personSelected]}><View style={[styles.avatar, targetId === employee.id && styles.avatarSelected]}><Text style={[styles.initials, targetId === employee.id && { color: '#fff' }]}>{employee.name.split(' ').map(part => part[0]).join('').slice(0, 2).toUpperCase()}</Text></View><View style={{ flex: 1 }}><Text style={styles.personName}>{employee.name}</Text><Text style={styles.personRole}>{pretty(employee.role)}</Text></View><View style={[styles.radio, targetId === employee.id && styles.radioSelected]}>{targetId === employee.id && <View style={styles.radioDot} />}</View></Pressable>)}</View>
    {!employees.length && <Text style={styles.meta}>No other active employees are available. Ask a manager to add the recipient first.</Text>}
    <Input label="New location" value={location} onChangeText={setLocation} placeholder="Job site or shop" />
    <DateField label="Expected return date" value={due} onChange={setDue} minimumDate={new Date()} optional />
    <Input label="Transfer notes" value={notes} onChangeText={setNotes} multiline placeholder="Parts included or handoff details" />
    <Button title="Confirm transfer" onPress={submit} loading={loading} disabled={!employees.length} />
  </Screen>;
}

const styles = StyleSheet.create({
  asset: { color: colors.orangeDark, fontSize: 11, fontWeight: '900', letterSpacing: 1.2 }, name: { color: colors.ink, fontWeight: '900', fontSize: 22, marginTop: 4 }, meta: { color: colors.muted, marginTop: 5, lineHeight: 20 },
  people: { gap: 9 }, person: { flexDirection: 'row', alignItems: 'center', gap: 12, borderRadius: 16, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.surface, padding: 12 }, personSelected: { borderColor: colors.orange, backgroundColor: colors.paleOrange },
  avatar: { width: 44, height: 44, borderRadius: 14, backgroundColor: colors.paleBlue, alignItems: 'center', justifyContent: 'center' }, avatarSelected: { backgroundColor: colors.orangeDark }, initials: { color: colors.blue, fontWeight: '900' }, personName: { color: colors.ink, fontWeight: '800' }, personRole: { color: colors.muted, fontSize: 11, marginTop: 2 },
  radio: { width: 22, height: 22, borderRadius: 11, borderWidth: 2, borderColor: colors.line, alignItems: 'center', justifyContent: 'center' }, radioSelected: { borderColor: colors.orangeDark }, radioDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: colors.orangeDark },
});
