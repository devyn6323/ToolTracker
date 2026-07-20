import { useState } from 'react';
import { Alert, StyleSheet, Text } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, ErrorBanner, Input, Screen } from '../components/ui';
import { colors } from '../theme';
import { passwordRequirements, validatePassword } from '../validation';
import { AppStackParams } from '../types';

type Props = Partial<NativeStackScreenProps<AppStackParams, 'ChangePassword'>>;

export function ChangePasswordScreen({ navigation }: Props = {}) {
  const { session, changePassword } = useAuth();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function submit() {
    const passwordError = validatePassword(next);
    if (!current) return setError('Enter your current password.');
    if (passwordError) return setError(passwordError);
    if (next !== confirm) return setError('New passwords do not match.');
    setLoading(true); setError('');
    try {
      const wasRequired = session?.passwordChangeRequired === true;
      await changePassword(current, next);
      if (!wasRequired) {
        Alert.alert('Password changed', 'Your other signed-in devices have been signed out.', [
          { text: 'OK', onPress: () => navigation?.goBack() },
        ]);
      }
    }
    catch (e) { setError(e instanceof ApiError || e instanceof Error ? e.message : 'Could not change the password.'); }
    finally { setLoading(false); }
  }

  return <Screen>
    <Text style={styles.title}>{session?.passwordChangeRequired ? 'Create your private password' : 'Change password'}</Text>
    <Text style={styles.body}>{session?.passwordChangeRequired
      ? 'The password your manager provided was temporary. Replace it before using company inventory.'
      : 'Changing your password signs out every other device using this account.'}</Text>
    <ErrorBanner message={error} />
    <Input label="Current password" value={current} onChangeText={setCurrent} secureTextEntry autoComplete="current-password" />
    <Input label="New password" value={next} onChangeText={setNext} secureTextEntry autoComplete="new-password" />
    <Text style={styles.help}>{passwordRequirements}</Text>
    <Input label="Confirm new password" value={confirm} onChangeText={setConfirm} secureTextEntry autoComplete="new-password" />
    <Button title="Save new password" onPress={submit} loading={loading} />
  </Screen>;
}

const styles = StyleSheet.create({ title: { color: colors.ink, fontSize: 29, fontWeight: '900' }, body: { color: colors.muted, lineHeight: 22 }, help: { color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: -8 } });
