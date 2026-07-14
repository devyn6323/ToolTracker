import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { API_URL, ApiError } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { Button, ErrorBanner, Input, Screen } from '../../components/ui';
import { colors } from '../../theme';
import { AuthStackParams } from '../../types';

export function LoginScreen({ navigation }: NativeStackScreenProps<AuthStackParams, 'Login'>) {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function submit() {
    if (!email || !password) return setError('Enter your email and password.');
    setLoading(true); setError('');
    try { await login(email.trim(), password); }
    catch (e) { setError(e instanceof ApiError ? e.message : 'Could not connect to ToolTrack.'); }
    finally { setLoading(false); }
  }

  return <Screen style={styles.content}>
    <View style={styles.brandRow}><View style={styles.mark}><Text style={styles.markText}>TT</Text></View><Text style={styles.brand}>ToolTrack</Text></View>
    <View style={styles.hero}><Text style={styles.eyebrow}>CREW INVENTORY</Text><Text style={styles.title}>Every tool.{`\n`}Accounted for.</Text><Text style={styles.subtitle}>Scan, check out, and return equipment without the clipboard chase.</Text></View>
    <View style={styles.form}>
      <ErrorBanner message={error} />
      <Input label="Work email" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" autoComplete="email" placeholder="you@company.com" />
      <Input label="Password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="password" placeholder="Your password" />
      <Button title="Sign in" loading={loading} onPress={submit} />
      <Button title="Create a company account" variant="ghost" onPress={() => navigation.navigate('Register')} />
    </View>
    {__DEV__ && <Text style={styles.api}>API: {API_URL}</Text>}
    <Text style={styles.credit}>Created by Flightline Software</Text>
  </Screen>;
}

const styles = StyleSheet.create({
  content: { paddingTop: 54 }, brandRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  mark: { width: 42, height: 42, borderRadius: 12, backgroundColor: colors.navy, alignItems: 'center', justifyContent: 'center' }, markText: { color: colors.orange, fontWeight: '900' },
  brand: { color: colors.navy, fontWeight: '900', fontSize: 21 }, hero: { marginTop: 28, gap: 10 }, eyebrow: { color: colors.orangeDark, fontWeight: '900', letterSpacing: 2, fontSize: 11 },
  title: { color: colors.ink, fontWeight: '900', fontSize: 42, lineHeight: 45, letterSpacing: -1.5 }, subtitle: { color: colors.muted, fontSize: 16, lineHeight: 24, maxWidth: 330 },
  form: { marginTop: 26, gap: 14 }, api: { textAlign: 'center', color: '#98A2A7', fontSize: 10, marginTop: 8 },
  credit: { textAlign: 'center', color: colors.muted, fontSize: 12, fontWeight: '600', marginTop: 4 },
});
