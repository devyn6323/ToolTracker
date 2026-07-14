import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from '../theme';
import { Tool } from '../types';
import { StatusPill, pretty } from './ui';

export function ToolCard({ tool, onPress }: { tool: Tool; onPress(): void }) {
  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.card, pressed && { opacity: .78 }]}>
      <View style={styles.icon}><Text style={styles.iconText}>{tool.name.slice(0, 2).toUpperCase()}</Text></View>
      <View style={styles.body}>
        <Text style={styles.name} numberOfLines={1}>{tool.name}</Text>
        <Text style={styles.meta}>{tool.assetNumber} · {tool.category || 'Uncategorized'}</Text>
        <View style={styles.bottom}><StatusPill status={tool.status} /><Text style={styles.location} numberOfLines={1}>{tool.currentLocation || pretty(tool.condition)}</Text></View>
      </View>
      <Text style={styles.chevron}>›</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.surface, borderRadius: 17, borderWidth: 1, borderColor: colors.line, padding: 13, gap: 12 },
  icon: { width: 48, height: 48, borderRadius: 14, backgroundColor: colors.paleOrange, alignItems: 'center', justifyContent: 'center' },
  iconText: { color: colors.orangeDark, fontWeight: '900' }, body: { flex: 1, gap: 3 }, name: { fontSize: 16, fontWeight: '800', color: colors.ink },
  meta: { color: colors.muted, fontSize: 12 }, bottom: { marginTop: 6, flexDirection: 'row', alignItems: 'center', gap: 8 },
  location: { flex: 1, color: colors.muted, fontSize: 11 }, chevron: { fontSize: 28, color: '#9AA5AA' },
});
