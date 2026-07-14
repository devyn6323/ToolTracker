import { ActivityIndicator, Image, StyleSheet, Text, View } from 'react-native';
import { colors } from '../theme';

export function LoadingScreen({ label = 'Loading' }: { label?: string }) {
  return (
    <View style={styles.container}>
      <Image source={require('../../assets/tooltrack-icon-1024.png')} style={styles.mark} accessibilityLabel="ToolTrack" />
      <Text style={styles.brand}>ToolTrack</Text>
      <ActivityIndicator color={colors.orange} size="large" />
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.credit}>Created by Flightline Software</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.paper, gap: 14 },
  mark: { width: 82, height: 82, borderRadius: 22 },
  brand: { color: colors.navy, fontWeight: '900', fontSize: 25, letterSpacing: -.5 },
  label: { color: colors.muted, fontWeight: '600' },
  credit: { position: 'absolute', bottom: 42, color: colors.muted, fontSize: 12, fontWeight: '600', letterSpacing: .25 },
});
