import { useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import QRCode from 'react-native-qrcode-svg';
import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import { useCallback, useRef, useState } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import { assetUrl, request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBanner, formatDate, Screen, SectionTitle, StatusPill, pretty } from '../components/ui';
import { LoadingScreen } from '../components/LoadingScreen';
import { colors } from '../theme';
import { AppStackParams, Tool, ToolTransaction } from '../types';

export function ToolDetailScreen({ route, navigation }: NativeStackScreenProps<AppStackParams, 'ToolDetail'>) {
  const { session } = useAuth(); const qrRef = useRef<any>(null); const [tool, setTool] = useState<Tool>(); const [history, setHistory] = useState<ToolTransaction[]>([]); const [error, setError] = useState(''); const [printing, setPrinting] = useState(false);
  const load = useCallback(async () => { try { const [nextTool, nextHistory] = await Promise.all([request<Tool>(`/api/tools/${route.params.toolId}`, {}, session?.token), request<ToolTransaction[]>(`/api/tools/${route.params.toolId}/history`, {}, session?.token)]); setTool(nextTool); setHistory(nextHistory); } catch (e) { setError(e instanceof Error ? e.message : 'Could not load tool.'); } }, [route.params.toolId, session?.token]);
  useFocusEffect(useCallback(() => { load(); }, [load]));
  if (!tool && !error) return <LoadingScreen label="Loading tool" />;
  if (!tool) return <Screen><ErrorBanner message={error} /></Screen>;
  const available = tool.status === 'AVAILABLE'; const canEdit = session?.user.role !== 'EMPLOYEE';
  const qrImage = () => new Promise<string>((resolve, reject) => {
    if (!qrRef.current) return reject(new Error('QR code is not ready'));
    qrRef.current.toDataURL(resolve);
  });
  const labelHtml = async () => `<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"><style>@page{margin:24px}body{font-family:Arial,sans-serif;text-align:center;color:#102b3a}.label{border:2px solid #102b3a;border-radius:18px;padding:24px;max-width:420px;margin:auto}.asset{color:#c45b18;font-weight:800;letter-spacing:1px}.name{font-size:28px;font-weight:800;margin:6px 0 18px}.qr{width:260px;height:260px}.hint{font-size:12px;color:#596b75;margin-top:12px}</style></head><body><div class="label"><div class="asset">${escapeHtml(tool.assetNumber)}</div><div class="name">${escapeHtml(tool.name)}</div><img class="qr" src="data:image/png;base64,${await qrImage()}"><div class="hint">Scan with ToolTrack</div></div></body></html>`;
  async function printLabel(share = false) { setPrinting(true); setError(''); try { const html = await labelHtml(); if (share) { const { uri } = await Print.printToFileAsync({ html }); if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(uri, { mimeType: 'application/pdf', dialogTitle: `${tool!.assetNumber} QR label` }); else setError('File sharing is not available on this device.'); } else await Print.printAsync({ html }); } catch { setError('Could not create the QR label. Try again.'); } finally { setPrinting(false); } }
  return <Screen>
    <ErrorBanner message={error} />
    {tool.photoUrl ? <Image source={{ uri: assetUrl(tool.photoUrl) }} style={styles.photo} /> : <View style={styles.photoPlaceholder}><Text style={styles.photoLetters}>{tool.name.slice(0, 2).toUpperCase()}</Text></View>}
    <View style={styles.heading}><View style={{ flex: 1 }}><Text style={styles.asset}>{tool.assetNumber}</Text><Text style={styles.title}>{tool.name}</Text><Text style={styles.sub}>{[tool.manufacturer, tool.model].filter(Boolean).join(' · ') || tool.category || 'Uncategorized'}</Text></View><StatusPill status={tool.status} /></View>
    <View style={styles.actions}>
      {available ? <Button title="Check out" onPress={() => navigation.navigate('Checkout', { tool })} style={{ flex: 1 }} /> : tool.checkedOutTo?.id === session?.user.id || canEdit ? <Button title="Return tool" onPress={() => navigation.navigate('Return', { tool })} style={{ flex: 1 }} /> : null}
      {!available && (tool.checkedOutTo?.id === session?.user.id || canEdit) && <Button title="Transfer" variant="secondary" onPress={() => navigation.navigate('Transfer', { tool })} style={{ flex: 1 }} />}
      {canEdit && <Button title="Edit" variant="secondary" onPress={() => navigation.navigate('ToolForm', { toolId: tool.id })} style={{ flex: available ? undefined : 1 }} />}
    </View>
    {tool.checkedOutTo && <Card style={styles.holder}><Text style={styles.cardEyebrow}>CURRENT HOLDER</Text><Text style={styles.holderNameLight}>{tool.checkedOutTo.name}</Text><Text style={styles.holderSub}>{tool.currentLocation || 'No location'} · Due {formatDate(tool.expectedReturnAt)}</Text></Card>}
    <Card><SectionTitle title="Details" /><Detail label="Category" value={tool.category} /><Detail label="Serial number" value={tool.serialNumber} /><Detail label="Condition" value={pretty(tool.condition)} /><Detail label="Location" value={tool.currentLocation} /><Detail label="Purchased" value={formatDate(tool.purchaseDate)} />{!!tool.notes && <Detail label="Notes" value={tool.notes} />}</Card>
    <Card style={styles.qrCard}><SectionTitle title="Tool QR code" /><View style={styles.qr}><QRCode ref={qrRef} value={tool.qrCodeValue} size={176} color={colors.ink} backgroundColor="#fff" /></View><Text style={styles.qrHint}>Attach this code to the tool. Scanning it opens this record.</Text><Text selectable style={styles.qrValue}>{tool.qrCodeValue}</Text><View style={styles.labelActions}><Button title="Print label" onPress={() => printLabel()} loading={printing} style={{ flex: 1 }} /><Button title="Share PDF" variant="secondary" onPress={() => printLabel(true)} disabled={printing} style={{ flex: 1 }} /></View></Card>
    <SectionTitle title={`History · ${history.length}`} />
    {history.map(item => <Card key={item.id}><View style={styles.historyTop}><Text style={styles.historyTitle}>{item.transactionType === 'TRANSFER' ? 'Transferred' : item.returnedAt ? 'Completed checkout' : 'Checked out'}</Text><Text style={styles.historyDate}>{formatDate(item.checkedOutAt, true)}</Text></View><Text style={styles.holderName}>{item.user.name}</Text><Text style={styles.sub}>{item.jobName || 'No job'} · {item.location || 'No location'}</Text>{item.returnedAt && <Text style={styles.returned}>Ended {formatDate(item.returnedAt, true)} · {pretty(item.conditionAtReturn)}</Text>}</Card>)}
    {!history.length && <Text style={styles.qrHint}>No checkout history yet.</Text>}
  </Screen>;
}

function escapeHtml(value: string) { return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;'); }
function Detail({ label, value }: { label: string; value?: string }) { return <View style={styles.detail}><Text style={styles.detailLabel}>{label}</Text><Text style={styles.detailValue}>{value || '—'}</Text></View>; }
const styles = StyleSheet.create({
  photo: { height: 220, borderRadius: 20, backgroundColor: colors.line }, photoPlaceholder: { height: 180, borderRadius: 20, backgroundColor: colors.paleOrange, alignItems: 'center', justifyContent: 'center' }, photoLetters: { fontSize: 52, fontWeight: '900', color: colors.orangeDark },
  heading: { flexDirection: 'row', gap: 10, alignItems: 'flex-start' }, asset: { color: colors.orangeDark, fontSize: 11, letterSpacing: 1.3, fontWeight: '900' }, title: { color: colors.ink, fontWeight: '900', fontSize: 28 }, sub: { color: colors.muted, marginTop: 3, lineHeight: 20 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 }, holder: { backgroundColor: colors.navy, borderColor: colors.navy }, cardEyebrow: { color: colors.orange, fontWeight: '900', fontSize: 10, letterSpacing: 1.3 }, holderName: { color: colors.ink, fontWeight: '800', fontSize: 16, marginTop: 4 }, holderNameLight: { color: '#fff', fontWeight: '800', fontSize: 18, marginTop: 5 }, holderSub: { color: '#BCC8CE', marginTop: 4 },
  detail: { flexDirection: 'row', justifyContent: 'space-between', gap: 20, borderBottomWidth: 1, borderBottomColor: colors.line, paddingVertical: 12 }, detailLabel: { color: colors.muted }, detailValue: { color: colors.ink, fontWeight: '700', flex: 1, textAlign: 'right' },
  qrCard: { alignItems: 'stretch' }, qr: { alignItems: 'center', padding: 20, marginTop: 6 }, qrHint: { color: colors.muted, textAlign: 'center', lineHeight: 20 }, qrValue: { color: colors.blue, fontSize: 10, textAlign: 'center', marginTop: 9 }, labelActions: { flexDirection: 'row', gap: 10, marginTop: 14 },
  historyTop: { flexDirection: 'row', justifyContent: 'space-between', gap: 10 }, historyTitle: { color: colors.ink, fontWeight: '900' }, historyDate: { color: colors.muted, fontSize: 11 }, returned: { color: colors.success, fontWeight: '700', fontSize: 12, marginTop: 10 },
});
