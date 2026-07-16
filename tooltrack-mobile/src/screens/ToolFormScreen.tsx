import { NativeStackScreenProps } from '@react-navigation/native-stack';
import * as ImagePicker from 'expo-image-picker';
import { useEffect, useState } from 'react';
import { Image, Text, StyleSheet, View } from 'react-native';
import { ApiError, assetUrl, request, uploadToolPhoto } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, DateField, ErrorBanner, Input, Screen, SelectChips } from '../components/ui';
import { colors } from '../theme';
import { AppStackParams, Tool, ToolCondition, ToolStatus } from '../types';

const conditions: ToolCondition[] = ['NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED'];
const statuses: ToolStatus[] = ['AVAILABLE', 'MAINTENANCE', 'DAMAGED', 'LOST', 'RETIRED'];
export function ToolFormScreen({ route, navigation }: NativeStackScreenProps<AppStackParams, 'ToolForm'>) {
  const { session } = useAuth(); const id = route.params?.toolId;
  const [form, setForm] = useState({ assetNumber: '', name: '', category: '', manufacturer: '', model: '', serialNumber: '', purchaseDate: '', condition: 'GOOD' as ToolCondition, status: 'AVAILABLE' as ToolStatus, currentLocation: '', photoUrl: '', notes: '' });
  const [loading, setLoading] = useState(false); const [uploading, setUploading] = useState(false); const [error, setError] = useState('');
  useEffect(() => { if (id) request<Tool>(`/api/tools/${id}`, {}, session?.token).then(tool => setForm({ assetNumber: tool.assetNumber, name: tool.name, category: tool.category || '', manufacturer: tool.manufacturer || '', model: tool.model || '', serialNumber: tool.serialNumber || '', purchaseDate: tool.purchaseDate || '', condition: tool.condition, status: tool.status === 'OVERDUE' ? 'CHECKED_OUT' : tool.status, currentLocation: tool.currentLocation || '', photoUrl: tool.photoUrl || '', notes: tool.notes || '' })).catch(e => setError(e.message)); }, [id, session?.token]);
  const set = (key: keyof typeof form, value: string) => setForm(current => ({ ...current, [key]: value }));
  async function uploadPhoto(result: ImagePicker.ImagePickerResult) {
    if (result.canceled || !session?.token) return;
    const asset = result.assets[0];
    if (asset.fileSize && asset.fileSize > 8 * 1024 * 1024) return setError('Choose a photo smaller than 8 MB.');
    setUploading(true);
    try { set('photoUrl', await uploadToolPhoto(asset, session.token)); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not upload photo.'); }
    finally { setUploading(false); }
  }
  async function choosePhoto() {
    setError('');
    try { await uploadPhoto(await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: true, aspect: [4, 3], quality: 0.65 })); }
    catch { setError('The photo library could not be opened. Check the app permissions and try again.'); }
  }
  async function takePhoto() {
    setError('');
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) return setError('Camera access is needed to take a tool photo.');
    try { await uploadPhoto(await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], allowsEditing: true, aspect: [4, 3], quality: 0.65 })); }
    catch { setError('The camera could not be opened. Check the app permissions and try again.'); }
  }
  async function save() {
    if (!form.assetNumber.trim() || !form.name.trim()) return setError('Asset number and tool name are required.');
    setLoading(true); setError('');
    try { const saved = await request<Tool>(id ? `/api/tools/${id}` : '/api/tools', { method: id ? 'PUT' : 'POST', body: JSON.stringify({ ...form, purchaseDate: form.purchaseDate || null, photoUrl: form.photoUrl || null }) }, session?.token); navigation.replace('ToolDetail', { toolId: saved.id }); }
    catch (e) { setError(e instanceof ApiError ? e.message : 'Could not save tool.'); } finally { setLoading(false); }
  }
  return <Screen><Text style={styles.help}>{id ? 'Update the inventory record.' : 'Add the identifying details first. A unique QR value will be generated automatically.'}</Text><ErrorBanner message={error} />
    <Input label="Asset number *" value={form.assetNumber} onChangeText={v => set('assetNumber', v)} placeholder="DRILL-001" autoCapitalize="characters" />
    <Input label="Tool name *" value={form.name} onChangeText={v => set('name', v)} placeholder="Cordless Drill" />
    <Input label="Category" value={form.category} onChangeText={v => set('category', v)} placeholder="Power Tools" />
    <Input label="Manufacturer" value={form.manufacturer} onChangeText={v => set('manufacturer', v)} placeholder="DeWalt" />
    <Input label="Model" value={form.model} onChangeText={v => set('model', v)} /><Input label="Serial number" value={form.serialNumber} onChangeText={v => set('serialNumber', v)} />
    <DateField label="Purchase date" value={form.purchaseDate} onChange={v => set('purchaseDate', v)} maximumDate={new Date()} optional />
    <SelectChips label="Condition" value={form.condition} options={conditions} onChange={v => set('condition', v)} />
    {id && !['CHECKED_OUT', 'OVERDUE'].includes(form.status) && <SelectChips label="Status" value={form.status} options={statuses} onChange={v => set('status', v)} />}
    <Input label="Current location" value={form.currentLocation} onChangeText={v => set('currentLocation', v)} placeholder="Main Shop" />
    <View style={styles.photoSection}><Text style={styles.photoLabel}>Tool photo</Text>{form.photoUrl ? <Image source={{ uri: assetUrl(form.photoUrl) }} style={styles.photo} /> : <View style={styles.photoEmpty}><Text style={styles.photoEmptyText}>No photo selected</Text></View>}<View style={styles.photoActions}><Button title="Photo library" variant="secondary" onPress={choosePhoto} loading={uploading} style={{ flex: 1 }} /><Button title="Take photo" variant="secondary" onPress={takePhoto} disabled={uploading} style={{ flex: 1 }} /></View>{!!form.photoUrl && <Button title="Remove photo" variant="ghost" onPress={() => set('photoUrl', '')} />}</View>
    <Input label="Notes" value={form.notes} onChangeText={v => set('notes', v)} multiline />
    <Button title={id ? 'Save changes' : 'Add tool'} onPress={save} loading={loading} />
  </Screen>;
}
const styles = StyleSheet.create({ help: { color: colors.muted, lineHeight: 21, marginBottom: 4 }, photoSection: { gap: 8 }, photoLabel: { color: colors.ink, fontWeight: '700', fontSize: 13 }, photo: { height: 190, borderRadius: 16, backgroundColor: colors.line }, photoEmpty: { height: 120, borderRadius: 16, borderWidth: 1, borderStyle: 'dashed', borderColor: colors.line, backgroundColor: colors.surface, alignItems: 'center', justifyContent: 'center' }, photoEmptyText: { color: colors.muted }, photoActions: { flexDirection: 'row', gap: 8 } });
