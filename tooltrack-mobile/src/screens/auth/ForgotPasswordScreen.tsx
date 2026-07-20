import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { ApiError, request } from '../../api/client';
import { Button, Card, ErrorBanner, Input, Screen } from '../../components/ui';
import { colors } from '../../theme';
import { AuthStackParams } from '../../types';
import { passwordRequirements, validatePassword } from '../../validation';

export function ForgotPasswordScreen({ route, navigation }: NativeStackScreenProps<AuthStackParams, 'ForgotPassword'>) {
  const [email, setEmail] = useState(route.params?.email || '');
  const [codeSent, setCodeSent] = useState(false);
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function sendCode() {
    if (!email.trim()) return setError('Enter your account email.');
    setLoading(true); setError('');
    try {
      await request<void>('/api/auth/password/forgot', { method: 'POST', body: JSON.stringify({ email: email.trim() }) });
      setCodeSent(true);
    } catch (e) { setError(e instanceof ApiError ? e.message : 'Could not request a reset code.'); }
    finally { setLoading(false); }
  }

  async function resetPassword() {
    const passwordError = validatePassword(password);
    if (!/^\d{8}$/.test(code)) return setError('Enter the 8-digit code from the email.');
    if (passwordError) return setError(passwordError);
    if (password !== confirm) return setError('Passwords do not match.');
    setLoading(true); setError('');
    try {
      await request<void>('/api/auth/password/reset', {
        method: 'POST', body: JSON.stringify({ email: email.trim(), code, newPassword: password }),
      });
      navigation.navigate('Login');
    } catch (e) { setError(e instanceof ApiError ? e.message : 'Could not reset the password.'); }
    finally { setLoading(false); }
  }

  return <Screen>
    <Text style={styles.title}>Reset your password</Text>
    <Text style={styles.body}>We’ll send an eight-digit code to the email on the ToolTrack account.</Text>
    <ErrorBanner message={error} />
    <Input label="Account email" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" autoComplete="email" editable={!codeSent} />
    {!codeSent ? <Button title="Email reset code" onPress={sendCode} loading={loading} /> : <>
      <Card><Text style={styles.notice}>If an eligible account exists, a code has been sent. Check spam or junk mail too.</Text></Card>
      <Input label="8-digit reset code" value={code} onChangeText={value => setCode(value.replace(/\D/g, '').slice(0, 8))} keyboardType="number-pad" autoComplete="one-time-code" />
      <Input label="New password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="new-password" />
      <Text style={styles.help}>{passwordRequirements}</Text>
      <Input label="Confirm new password" value={confirm} onChangeText={setConfirm} secureTextEntry autoComplete="new-password" />
      <Button title="Reset password" onPress={resetPassword} loading={loading} />
      <Button title="Send another code" variant="ghost" onPress={sendCode} disabled={loading} />
    </>}
  </Screen>;
}

const styles = StyleSheet.create({ title: { color: colors.ink, fontSize: 30, fontWeight: '900' }, body: { color: colors.muted, lineHeight: 22 }, notice: { color: colors.ink, lineHeight: 21 }, help: { color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: -8 } });
