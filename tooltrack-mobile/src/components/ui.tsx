import DateTimePicker, { DateTimePickerEvent } from '@react-native-community/datetimepicker';
import { PropsWithChildren, ReactNode, useState } from 'react';
import {
  ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleProp,
  StyleSheet, Text, TextInput, TextInputProps, View, ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, shadow } from '../theme';
import { ToolStatus } from '../types';

export function Screen({ children, scroll = true, style }: PropsWithChildren<{ scroll?: boolean; style?: StyleProp<ViewStyle> }>) {
  const content = scroll ? (
    <ScrollView contentContainerStyle={[styles.screenContent, style]} keyboardShouldPersistTaps="handled" keyboardDismissMode="interactive" automaticallyAdjustKeyboardInsets={Platform.OS === 'ios'}>{children}</ScrollView>
  ) : <View style={[styles.screenContent, { flex: 1 }, style]}>{children}</View>;
  return <SafeAreaView style={styles.safe} edges={['bottom']}><KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>{content}</KeyboardAvoidingView></SafeAreaView>;
}

export function Button({ title, onPress, loading, variant = 'primary', disabled, style }: {
  title: string; onPress(): void; loading?: boolean; variant?: 'primary' | 'secondary' | 'danger' | 'ghost'; disabled?: boolean; style?: StyleProp<ViewStyle>;
}) {
  return (
    <Pressable disabled={disabled || loading} onPress={onPress}
      style={({ pressed }) => [styles.button, styles[`button_${variant}`], pressed && { opacity: .82 }, (disabled || loading) && { opacity: .5 }, style]}>
      {loading ? <ActivityIndicator color={variant === 'secondary' || variant === 'ghost' ? colors.navy : '#fff'} />
        : <Text style={[styles.buttonText, styles[`buttonText_${variant}`]]}>{title}</Text>}
    </Pressable>
  );
}

export function Input({ label, error, ...props }: TextInputProps & { label: string; error?: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput placeholderTextColor="#96A1A7" {...props} style={[styles.input, props.multiline && styles.multiline, error && styles.inputError, props.style]} />
      {!!error && <Text style={styles.errorText}>{error}</Text>}
    </View>
  );
}

function parseInputDate(value?: string) {
  if (!value) return new Date();
  const [year, month, day] = value.split('-').map(Number);
  const parsed = new Date(year, month - 1, day, 12);
  return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
}

export function toInputDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function DateField({ label, value, onChange, minimumDate, maximumDate, optional = false }: {
  label: string; value?: string; onChange(value: string): void; minimumDate?: Date; maximumDate?: Date; optional?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const min = minimumDate ? new Date(minimumDate) : undefined;
  if (min) min.setHours(0, 0, 0, 0);
  const max = maximumDate ? new Date(maximumDate) : undefined;
  if (max) max.setHours(23, 59, 59, 999);
  const changed = (event: DateTimePickerEvent, date?: Date) => {
    if (Platform.OS === 'android') setOpen(false);
    if (event.type !== 'dismissed' && date) onChange(toInputDate(date));
  };
  return <View style={styles.field}><Text style={styles.label}>{label}</Text><Pressable accessibilityRole="button" onPress={() => setOpen(true)} style={styles.dateButton}><Text style={[styles.dateText, !value && styles.datePlaceholder]}>{value ? formatDate(`${value}T12:00:00`) : 'Select a date'}</Text><Text style={styles.calendar}>▣</Text></Pressable>{optional && !!value && <Pressable onPress={() => onChange('')}><Text style={styles.clearDate}>Clear date</Text></Pressable>}{open && <View style={styles.pickerWrap}><DateTimePicker value={parseInputDate(value)} mode="date" display={Platform.OS === 'ios' ? 'inline' : 'default'} minimumDate={min} maximumDate={max} onChange={changed} />{Platform.OS === 'ios' && <Button title="Done" variant="secondary" onPress={() => setOpen(false)} />}</View>}</View>;
}

export function SelectChips<T extends string>({ label, value, options, onChange }: { label: string; value: T; options: readonly T[]; onChange(value: T): void }) {
  return <View style={styles.field}><Text style={styles.label}>{label}</Text><View style={styles.chips}>{options.map(option => (
    <Pressable key={option} onPress={() => onChange(option)} style={[styles.chip, value === option && styles.chipActive]}>
      <Text style={[styles.chipText, value === option && styles.chipTextActive]}>{pretty(option)}</Text>
    </Pressable>
  ))}</View></View>;
}

export function SectionTitle({ title, action }: { title: string; action?: ReactNode }) {
  return <View style={styles.sectionRow}><Text style={styles.sectionTitle}>{title}</Text>{action}</View>;
}

export function Card({ children, style }: PropsWithChildren<{ style?: StyleProp<ViewStyle> }>) {
  return <View style={[styles.card, style]}>{children}</View>;
}

const statusColor: Record<ToolStatus, string> = {
  AVAILABLE: colors.success, CHECKED_OUT: colors.blue, OVERDUE: colors.warning,
  MAINTENANCE: '#7866A5', DAMAGED: colors.danger, LOST: '#642F2B', RETIRED: colors.muted,
};

export function StatusPill({ status }: { status: ToolStatus }) {
  const color = statusColor[status];
  return <View style={[styles.pill, { backgroundColor: color + '18' }]}><View style={[styles.dot, { backgroundColor: color }]} /><Text style={[styles.pillText, { color }]}>{pretty(status)}</Text></View>;
}

export function EmptyState({ title, message, action }: { title: string; message: string; action?: ReactNode }) {
  return <View style={styles.empty}><View style={styles.emptyIcon}><Text style={{ fontSize: 28 }}>⌁</Text></View><Text style={styles.emptyTitle}>{title}</Text><Text style={styles.emptyMessage}>{message}</Text>{action}</View>;
}

export function ErrorBanner({ message }: { message?: string }) {
  if (!message) return null;
  return <View style={styles.errorBanner}><Text style={styles.errorBannerText}>{message}</Text></View>;
}

export function pretty(value?: string) {
  if (!value) return '—';
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, letter => letter.toUpperCase());
}

export function formatDate(value?: string, includeTime = false) {
  if (!value) return '—';
  return new Intl.DateTimeFormat(undefined, includeTime
    ? { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' }
    : { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(value));
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.paper },
  screenContent: { padding: 18, paddingBottom: 36, gap: 16 },
  button: { minHeight: 52, borderRadius: 14, paddingHorizontal: 20, alignItems: 'center', justifyContent: 'center' },
  button_primary: { backgroundColor: colors.orange }, button_secondary: { backgroundColor: colors.paleBlue },
  button_danger: { backgroundColor: colors.danger }, button_ghost: { backgroundColor: 'transparent' },
  buttonText: { fontSize: 16, fontWeight: '800' }, buttonText_primary: { color: '#fff' }, buttonText_danger: { color: '#fff' },
  buttonText_secondary: { color: colors.navy }, buttonText_ghost: { color: colors.blue },
  field: { gap: 7 }, label: { color: colors.ink, fontWeight: '700', fontSize: 13 },
  input: { minHeight: 52, borderRadius: 13, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.surface, paddingHorizontal: 15, color: colors.ink, fontSize: 16 },
  dateButton: { minHeight: 52, borderRadius: 13, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.surface, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, dateText: { color: colors.ink, fontSize: 16, fontWeight: '600' }, datePlaceholder: { color: '#96A1A7', fontWeight: '400' }, calendar: { color: colors.blue, fontSize: 20 }, clearDate: { color: colors.blue, fontWeight: '700', fontSize: 12, alignSelf: 'flex-start' }, pickerWrap: { gap: 8, borderRadius: 16, backgroundColor: colors.surface, padding: 8 },
  multiline: { minHeight: 100, paddingTop: 14, textAlignVertical: 'top' }, inputError: { borderColor: colors.danger },
  errorText: { color: colors.danger, fontSize: 12 }, chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { borderWidth: 1, borderColor: colors.line, backgroundColor: colors.surface, borderRadius: 100, paddingHorizontal: 13, paddingVertical: 9 },
  chipActive: { backgroundColor: colors.navy, borderColor: colors.navy }, chipText: { color: colors.muted, fontWeight: '600' }, chipTextActive: { color: '#fff' },
  sectionRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 4 }, sectionTitle: { fontSize: 19, fontWeight: '900', color: colors.ink },
  card: { backgroundColor: colors.surface, borderRadius: 18, padding: 16, borderWidth: 1, borderColor: colors.line, ...shadow },
  pill: { flexDirection: 'row', alignItems: 'center', gap: 6, alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 100 },
  dot: { width: 7, height: 7, borderRadius: 4 }, pillText: { fontWeight: '800', fontSize: 11 },
  empty: { alignItems: 'center', paddingVertical: 48, paddingHorizontal: 24, gap: 8 }, emptyIcon: { width: 64, height: 64, borderRadius: 22, backgroundColor: colors.paleBlue, alignItems: 'center', justifyContent: 'center', marginBottom: 6 },
  emptyTitle: { fontSize: 20, fontWeight: '900', color: colors.ink }, emptyMessage: { textAlign: 'center', color: colors.muted, lineHeight: 21, marginBottom: 12 },
  errorBanner: { borderRadius: 12, padding: 13, backgroundColor: '#FDEBE8', borderWidth: 1, borderColor: '#F3C1BA' }, errorBannerText: { color: colors.danger, fontWeight: '600' },
});
