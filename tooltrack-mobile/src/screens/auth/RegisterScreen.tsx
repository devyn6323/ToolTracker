import { useState } from 'react';
import { Text, StyleSheet } from 'react-native';
import { ApiError } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { Button, ErrorBanner, Input, Screen } from '../../components/ui';
import { colors } from '../../theme';
import { passwordRequirements, validatePassword } from '../../validation';

export function RegisterScreen() {
  const { register } = useAuth();
  const [companyName, setCompanyName] = useState(''); const [name, setName] = useState('');
  const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false); const [error, setError] = useState('');

  async function submit() {
    if (![companyName, name, email, password].every(Boolean)) return setError('Complete every field to continue.');
    const passwordError = validatePassword(password);
    if (passwordError) return setError(passwordError);
    setLoading(true); setError('');
    try { await register(companyName.trim(), name.trim(), email.trim(), password); }
    catch (e) { setError(e instanceof ApiError ? e.message : 'Could not create your account.'); }
    finally { setLoading(false); }
  }

  return <Screen>
    <Text style={styles.title}>Set up your crew</Text><Text style={styles.subtitle}>You’ll be the company owner. You can add managers and employees next.</Text>
    <ErrorBanner message={error} />
    <Input label="Company name" value={companyName} onChangeText={setCompanyName} placeholder="Demo Construction" />
    <Input label="Your name" value={name} onChangeText={setName} autoComplete="name" placeholder="Alex Morgan" />
    <Input label="Work email" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" autoComplete="email" placeholder="alex@company.com" />
    <Input label="Password" value={password} onChangeText={setPassword} secureTextEntry placeholder="Strong password" autoComplete="new-password" />
    <Text style={styles.passwordHelp}>{passwordRequirements}</Text>
    <Button title="Create company" onPress={submit} loading={loading} />
    <Text style={styles.credit}>Created by Flightline Software</Text>
  </Screen>;
}
const styles = StyleSheet.create({
  title: { fontSize: 30, fontWeight: '900', color: colors.ink },
  subtitle: { color: colors.muted, lineHeight: 21, marginBottom: 6 },
  passwordHelp: { color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: -8 },
  credit: { textAlign: 'center', color: colors.muted, fontSize: 12, fontWeight: '600', marginTop: 8 },
});
