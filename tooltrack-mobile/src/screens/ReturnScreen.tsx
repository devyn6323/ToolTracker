import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { ApiError, request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBanner, Input, Screen, SelectChips } from '../components/ui';
import { colors } from '../theme';
import { AppStackParams, ToolCondition, ToolTransaction } from '../types';
const conditions: ToolCondition[] = ['NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'MISSING'];
export function ReturnScreen({ route, navigation }: NativeStackScreenProps<AppStackParams, 'Return'>) {
  const { tool } = route.params; const { session } = useAuth(); const [condition, setCondition] = useState<ToolCondition>(tool.condition); const [location, setLocation] = useState(tool.currentLocation || 'Main Shop'); const [notes, setNotes] = useState(''); const [loading, setLoading] = useState(false); const [error, setError] = useState('');
  async function submit() { setLoading(true); setError(''); try { await request<ToolTransaction>(`/api/tools/${tool.id}/return`, { method: 'POST', body: JSON.stringify({ conditionAtReturn: condition, location, notes }) }, session?.token); navigation.replace('ToolDetail', { toolId: tool.id }); } catch (e) { setError(e instanceof ApiError ? e.message : 'Could not return tool.'); } finally { setLoading(false); } }
  return <Screen><Card><Text style={styles.asset}>{tool.assetNumber}</Text><Text style={styles.name}>{tool.name}</Text><Text style={styles.person}>Held by {tool.checkedOutTo?.name || 'current employee'}</Text></Card><ErrorBanner message={error} /><SelectChips label="Condition on return" value={condition} options={conditions} onChange={value => setCondition(value as ToolCondition)} /><Input label="Return location" value={location} onChangeText={setLocation} placeholder="Main Shop" /><Input label="Return notes" value={notes} onChangeText={setNotes} multiline placeholder="Mention damage or missing parts" />
    {condition === 'DAMAGED' && <Card style={styles.warning}><Text style={styles.warningTitle}>This tool will be marked damaged.</Text><Text style={styles.warningText}>Managers will see it in the issues count instead of available inventory.</Text></Card>}
    {condition === 'MISSING' && <Card style={styles.warning}><Text style={styles.warningTitle}>This tool will be marked lost.</Text><Text style={styles.warningText}>Use notes to record the last known location.</Text></Card>}
    <Button title="Confirm return" onPress={submit} loading={loading} />
  </Screen>;
}
const styles = StyleSheet.create({ asset: { color: colors.orangeDark, fontSize: 11, fontWeight: '900', letterSpacing: 1.2 }, name: { color: colors.ink, fontWeight: '900', fontSize: 22, marginTop: 4 }, person: { color: colors.muted, marginTop: 5 }, warning: { backgroundColor: '#FFF3E7', borderColor: '#F2CFAD' }, warningTitle: { color: colors.warning, fontWeight: '900' }, warningText: { color: colors.muted, lineHeight: 20, marginTop: 4 } });
