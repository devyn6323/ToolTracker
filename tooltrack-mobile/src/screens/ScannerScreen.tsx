import { useIsFocused, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { BarcodeScanningResult, CameraView, useCameraPermissions } from 'expo-camera';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { ApiError, request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, ErrorBanner, Screen } from '../components/ui';
import { colors } from '../theme';
import { AppStackParams, Tool } from '../types';

export function ScannerScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParams>>(); const { session } = useAuth(); const focused = useIsFocused();
  const [permission, requestPermission] = useCameraPermissions(); const [locked, setLocked] = useState(false); const [error, setError] = useState('');
  async function scanned(result: BarcodeScanningResult) { if (locked) return; setLocked(true); setError(''); try { const tool = await request<Tool>(`/api/tools/by-qr/${encodeURIComponent(result.data)}`, {}, session?.token); navigation.navigate('ToolDetail', { toolId: tool.id }); } catch (e) { setError(e instanceof ApiError && e.status === 404 ? 'That QR code is not a tool in your company.' : 'Could not look up this QR code.'); setTimeout(() => setLocked(false), 1200); } }
  if (!permission) return <Screen><Text style={styles.message}>Checking camera permission…</Text></Screen>;
  if (!permission.granted) return <Screen style={styles.permission}><View style={styles.cameraMark}><Text style={{ fontSize: 34 }}>⌗</Text></View><Text style={styles.permissionTitle}>Camera access needed</Text><Text style={styles.message}>ToolTrack only uses the camera while this screen is open to scan QR labels.</Text><Button title="Allow camera" onPress={requestPermission} /></Screen>;
  return <View style={styles.container}>{focused && <CameraView style={StyleSheet.absoluteFill} facing="back" barcodeScannerSettings={{ barcodeTypes: ['qr'] }} onBarcodeScanned={locked ? undefined : scanned} />}<View style={styles.overlay}><View style={styles.top}><Text style={styles.scanTitle}>Point at a tool label</Text><Text style={styles.scanSub}>The QR code will scan automatically.</Text><ErrorBanner message={error} /></View><View style={styles.frame}><View style={[styles.corner, styles.tl]} /><View style={[styles.corner, styles.tr]} /><View style={[styles.corner, styles.bl]} /><View style={[styles.corner, styles.br]} /></View><View style={styles.bottom}>{locked && <Text style={styles.found}>Looking up tool…</Text>}<Button title="Scan another" variant="secondary" onPress={() => { setError(''); setLocked(false); }} /></View></View></View>;
}
const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' }, overlay: { flex: 1, justifyContent: 'space-between', padding: 24, backgroundColor: '#07121A55' }, top: { marginTop: 30, gap: 8 }, scanTitle: { color: '#fff', fontWeight: '900', fontSize: 25, textAlign: 'center' }, scanSub: { color: '#D3DCE0', textAlign: 'center' },
  frame: { width: 252, height: 252, alignSelf: 'center' }, corner: { position: 'absolute', width: 48, height: 48, borderColor: colors.orange }, tl: { left: 0, top: 0, borderLeftWidth: 5, borderTopWidth: 5, borderTopLeftRadius: 18 }, tr: { right: 0, top: 0, borderRightWidth: 5, borderTopWidth: 5, borderTopRightRadius: 18 }, bl: { left: 0, bottom: 0, borderLeftWidth: 5, borderBottomWidth: 5, borderBottomLeftRadius: 18 }, br: { right: 0, bottom: 0, borderRightWidth: 5, borderBottomWidth: 5, borderBottomRightRadius: 18 },
  bottom: { gap: 12, marginBottom: 20 }, found: { color: '#fff', textAlign: 'center', fontWeight: '800' }, permission: { justifyContent: 'center', alignItems: 'center', paddingHorizontal: 36 }, cameraMark: { width: 72, height: 72, borderRadius: 22, backgroundColor: colors.paleOrange, justifyContent: 'center', alignItems: 'center' }, permissionTitle: { color: colors.ink, fontWeight: '900', fontSize: 24, marginTop: 8 }, message: { color: colors.muted, lineHeight: 21, textAlign: 'center' },
});
