import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, StyleSheet, Text } from 'react-native';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBanner, Input, Screen } from '../components/ui';
import { colors } from '../theme';
import { AppStackParams } from '../types';

export function TransferOwnershipScreen({ route, navigation }: NativeStackScreenProps<AppStackParams, 'TransferOwnership'>) {
  const { session, transferOwnership, googleReauthenticationToken } = useAuth();
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const passwordLogin = session?.passwordLoginEnabled !== false;
  const employee = route.params.employee;

  function confirm() {
    if (passwordLogin && !password) return setError('Enter your password to confirm.');
    Alert.alert('Transfer company ownership?', `${employee.name} will become the owner. You will become a manager and will no longer be able to delete the company.`, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Transfer ownership', style: 'destructive', onPress: submit },
    ]);
  }

  async function submit() {
    setLoading(true); setError('');
    try {
      const confirmation = passwordLogin ? { password } : { googleIdToken: await googleReauthenticationToken() };
      await transferOwnership(employee.id, confirmation);
      Alert.alert('Ownership transferred', `${employee.name} is now the company owner.`, [
        { text: 'OK', onPress: () => navigation.popToTop() },
      ]);
    } catch (e) { setError(e instanceof ApiError || e instanceof Error ? e.message : 'Could not transfer ownership.'); }
    finally { setLoading(false); }
  }

  return <Screen>
    <Text style={styles.title}>Transfer ownership</Text>
    <Text style={styles.body}>Use this when another person will be responsible for the ToolTrack company account.</Text>
    <Card><Text style={styles.name}>{employee.name}</Text><Text style={styles.meta}>{employee.email}</Text><Text style={styles.meta}>{employee.role}</Text></Card>
    <ErrorBanner message={error} />
    {passwordLogin ? <Input label="Confirm with your password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="current-password" />
      : <Text style={styles.body}>Google will ask you to confirm the account linked to ToolTrack.</Text>}
    <Button title={passwordLogin ? 'Transfer ownership' : 'Confirm with Google and transfer'} variant="danger" onPress={confirm} loading={loading} />
  </Screen>;
}

const styles = StyleSheet.create({ title: { color: colors.ink, fontSize: 29, fontWeight: '900' }, body: { color: colors.muted, lineHeight: 22 }, name: { color: colors.ink, fontSize: 20, fontWeight: '900' }, meta: { color: colors.muted, marginTop: 4 } });
