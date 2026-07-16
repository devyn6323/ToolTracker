import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { ApiError, request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, DateField, EmptyState, ErrorBanner, Input, Screen, SectionTitle, toInputDate } from '../components/ui';
import { colors } from '../theme';
import { AppStackParams, Tool, ToolTransaction } from '../types';

const defaultDue = () => { const date = new Date(); date.setDate(date.getDate() + 7); return toInputDate(date); };

export function BatchCheckoutScreen({ navigation }: NativeStackScreenProps<AppStackParams, 'BatchCheckout'>) {
  const { session } = useAuth(); const [tools, setTools] = useState<Tool[]>([]); const [selected, setSelected] = useState<string[]>([]); const [jobName, setJobName] = useState(''); const [location, setLocation] = useState(''); const [due, setDue] = useState(defaultDue); const [notes, setNotes] = useState(''); const [loading, setLoading] = useState(false); const [error, setError] = useState('');
  useEffect(() => { request<Tool[]>('/api/tools', {}, session?.token).then(items => setTools(items.filter(tool => tool.status === 'AVAILABLE'))).catch(e => setError(e instanceof Error ? e.message : 'Could not load available tools.')); }, [session?.token]);
  const toggle = (id: string) => setSelected(current => current.includes(id) ? current.filter(item => item !== id) : [...current, id]);
  async function checkout() {
    if (!selected.length) return setError('Select at least one tool.');
    if (!jobName.trim()) return setError('Enter the job or project once for this checkout.');
    const expected = new Date(`${due}T17:00:00`);
    if (Number.isNaN(expected.getTime()) || expected <= new Date()) return setError('Choose a future return date.');
    setLoading(true); setError('');
    try { const transactions = await request<ToolTransaction[]>('/api/tools/checkout/batch', { method: 'POST', body: JSON.stringify({ toolIds: selected, jobName, location, expectedReturnAt: expected.toISOString(), notes }) }, session?.token); Alert.alert('Checkout complete', `${transactions.length} tools were checked out to ${jobName.trim()}.`); navigation.navigate('Main'); }
    catch (e) { setError(e instanceof ApiError ? e.message : 'Could not check out the selected tools.'); }
    finally { setLoading(false); }
  }
  return <Screen><Text style={styles.help}>Choose every tool going to the same job, then enter the shared details once.</Text><ErrorBanner message={error} /><SectionTitle title={`Available tools · ${tools.length}`} action={tools.length ? <Pressable onPress={() => setSelected(selected.length === tools.length ? [] : tools.map(tool => tool.id))}><Text style={styles.selectAll}>{selected.length === tools.length ? 'Clear all' : 'Select all'}</Text></Pressable> : undefined} />
    <View style={styles.tools}>{tools.map(tool => { const active = selected.includes(tool.id); return <Pressable key={tool.id} onPress={() => toggle(tool.id)} style={[styles.tool, active && styles.toolActive]}><View style={[styles.check, active && styles.checkActive]}><Text style={styles.checkText}>{active ? '✓' : ''}</Text></View><View style={{ flex: 1 }}><Text style={styles.asset}>{tool.assetNumber}</Text><Text style={styles.name}>{tool.name}</Text><Text style={styles.location}>{tool.currentLocation || 'No location recorded'}</Text></View></Pressable>; })}</View>
    {!tools.length && <EmptyState title="No available tools" message="Return a checked-out tool or mark inventory available before starting a batch checkout." />}
    {!!tools.length && <Card style={styles.details}><SectionTitle title={`Job details · ${selected.length} selected`} /><Input label="Job or project *" value={jobName} onChangeText={setJobName} placeholder="Warehouse Remodel" /><Input label="Use location" value={location} onChangeText={setLocation} placeholder="Job 42" /><DateField label="Expected return date *" value={due} onChange={setDue} minimumDate={tomorrow()} /><Input label="Shared notes" value={notes} onChangeText={setNotes} multiline placeholder="Details that apply to every selected tool" /><Button title={`Check out ${selected.length || ''} tool${selected.length === 1 ? '' : 's'}`.replace('  ', ' ')} onPress={checkout} loading={loading} disabled={!selected.length} /></Card>}
  </Screen>;
}

const styles = StyleSheet.create({ help: { color: colors.muted, lineHeight: 21 }, selectAll: { color: colors.blue, fontWeight: '800' }, tools: { gap: 9 }, tool: { flexDirection: 'row', alignItems: 'center', gap: 12, borderRadius: 16, padding: 13, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.surface }, toolActive: { borderColor: colors.orange, backgroundColor: colors.paleOrange }, check: { width: 26, height: 26, borderRadius: 8, borderWidth: 2, borderColor: colors.line, alignItems: 'center', justifyContent: 'center' }, checkActive: { backgroundColor: colors.orangeDark, borderColor: colors.orangeDark }, checkText: { color: '#fff', fontWeight: '900' }, asset: { color: colors.orangeDark, fontSize: 10, fontWeight: '900', letterSpacing: 1 }, name: { color: colors.ink, fontWeight: '800', fontSize: 16, marginTop: 2 }, location: { color: colors.muted, fontSize: 11, marginTop: 3 }, details: { gap: 14 } });
function tomorrow() { const date = new Date(); date.setDate(date.getDate() + 1); return date; }
