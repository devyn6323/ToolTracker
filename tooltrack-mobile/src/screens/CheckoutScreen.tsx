import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { ApiError, request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, DateField, ErrorBanner, Input, Screen, SelectChips, toInputDate } from '../components/ui';
import { colors } from '../theme';
import { AppStackParams, ToolCondition, ToolTransaction } from '../types';

const conditions: ToolCondition[] = ['NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'MISSING'];
const defaultDue = () => { const date = new Date(); date.setDate(date.getDate() + 7); return toInputDate(date); };
export function CheckoutScreen({ route, navigation }: NativeStackScreenProps<AppStackParams, 'Checkout'>) {
  const { tool } = route.params; const { session } = useAuth(); const [jobName, setJobName] = useState(''); const [location, setLocation] = useState(tool.currentLocation || ''); const [due, setDue] = useState(defaultDue); const [condition, setCondition] = useState<ToolCondition>(tool.condition); const [notes, setNotes] = useState(''); const [loading, setLoading] = useState(false); const [error, setError] = useState('');
  async function checkout() { const expected = new Date(`${due}T17:00:00`); if (Number.isNaN(expected.getTime()) || expected <= new Date()) return setError('Choose a future return date.'); setLoading(true); setError(''); try { await request<ToolTransaction>(`/api/tools/${tool.id}/checkout`, { method: 'POST', body: JSON.stringify({ jobName, location, expectedReturnAt: expected.toISOString(), conditionAtCheckout: condition, notes }) }, session?.token); navigation.replace('ToolDetail', { toolId: tool.id }); } catch (e) { setError(e instanceof ApiError ? e.message : 'Could not check out tool.'); } finally { setLoading(false); } }
  return <Screen><Card><Text style={styles.asset}>{tool.assetNumber}</Text><Text style={styles.name}>{tool.name}</Text><Text style={styles.person}>Checking out to {session?.user.name}</Text></Card><ErrorBanner message={error} />
    <Input label="Job or project" value={jobName} onChangeText={setJobName} placeholder="Warehouse Remodel" /><Input label="Use location" value={location} onChangeText={setLocation} placeholder="Job 42" /><DateField label="Expected return date *" value={due} onChange={setDue} minimumDate={tomorrow()} />
    <SelectChips label="Condition at checkout" value={condition} options={conditions} onChange={value => setCondition(value as ToolCondition)} /><Input label="Condition notes" value={notes} onChangeText={setNotes} multiline placeholder="Battery and case included" />
    <Button title="Confirm checkout" onPress={checkout} loading={loading} />
  </Screen>;
}
const styles = StyleSheet.create({ asset: { color: colors.orangeDark, fontSize: 11, fontWeight: '900', letterSpacing: 1.2 }, name: { color: colors.ink, fontWeight: '900', fontSize: 22, marginTop: 4 }, person: { color: colors.muted, marginTop: 5 } });
function tomorrow() { const date = new Date(); date.setDate(date.getDate() + 1); return date; }
