import { useState } from 'react';
import { Alert, Linking, StyleSheet, Text, View } from 'react-native';
import { API_URL, request } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBanner, Input, Screen, SectionTitle } from '../components/ui';
import { colors } from '../theme';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AppStackParams } from '../types';

const privacyUrl = process.env.EXPO_PUBLIC_PRIVACY_URL || (API_URL ? `${API_URL}/privacy` : undefined);
const deletionUrl = process.env.EXPO_PUBLIC_DELETE_ACCOUNT_URL || (API_URL ? `${API_URL}/delete-account` : undefined);
const supportEmail = process.env.EXPO_PUBLIC_SUPPORT_EMAIL;

export function SettingsScreen({ navigation }: NativeStackScreenProps<AppStackParams, 'Settings'>) {
  const { session, logout, googleReauthenticationToken } = useAuth();
  const [password, setPassword] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const owner = session?.user.role === 'OWNER';
  const passwordLoginEnabled = session?.passwordLoginEnabled !== false;

  async function open(url?: string) {
    if (!url) {
      setError('This release is missing its public policy URL. Contact the app administrator.');
      return;
    }
    await Linking.openURL(url);
  }

  function confirmDeletion() {
    setError('');
    if (passwordLoginEnabled && !password) {
      setError('Enter your password to confirm deletion.');
      return;
    }
    Alert.alert(
      owner ? 'Delete company and account?' : 'Delete account?',
      owner
        ? 'This permanently deletes the company, tools, employee accounts, photos references, and activity history. This cannot be undone.'
        : 'Your account will be disabled and your personal details removed. This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Delete permanently', style: 'destructive', onPress: deleteAccount },
      ],
    );
  }

  async function deleteAccount() {
    if (!session) return;
    setDeleting(true);
    try {
      const confirmation = passwordLoginEnabled
        ? { password }
        : { googleIdToken: await googleReauthenticationToken() };
      await request<void>('/api/auth/account', {
        method: 'DELETE',
        body: JSON.stringify(confirmation),
      }, session.token);
      await logout();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not delete the account.');
    } finally {
      setDeleting(false);
    }
  }

  return <Screen>
    <Card>
      <Text style={styles.name}>{session?.user.name}</Text>
      <Text style={styles.meta}>{session?.user.email}</Text>
      <Text style={styles.meta}>{session?.companyName} · {session?.user.role}</Text>
      <Button title="Sign out" variant="secondary" onPress={logout} style={styles.cardButton} />
      {passwordLoginEnabled && <Button title="Change password" variant="ghost" onPress={() => navigation.navigate('ChangePassword')} />}
    </Card>

    <SectionTitle title="Privacy & support" />
    <Card style={styles.linkCard}>
      <Text style={styles.body}>ToolTrack stores account details, company inventory, checkout history, and tool photos to provide the service. It does not sell personal data or use it for advertising.</Text>
      <Button title="Read privacy policy" variant="secondary" onPress={() => open(privacyUrl)} />
      <Button title="Web account deletion" variant="ghost" onPress={() => open(deletionUrl)} />
      {!!supportEmail && <Button title="Contact support" variant="ghost" onPress={() => open(`mailto:${supportEmail}`)} />}
    </Card>

    <SectionTitle title="Danger zone" />
    <ErrorBanner message={error} />
    <Card style={styles.dangerCard}>
      <Text style={styles.dangerTitle}>{owner ? 'Delete company and account' : 'Delete my account'}</Text>
      <Text style={styles.body}>{owner
        ? 'As the owner, deleting your account also permanently removes the company and all of its ToolTrack data.'
        : 'Your login will be disabled and identifying account details will be removed. Historical tool records retain an anonymous “Deleted user” label.'}</Text>
      {passwordLoginEnabled
        ? <Input label="Confirm with your password" value={password} onChangeText={setPassword} secureTextEntry autoCapitalize="none" />
        : <Text style={styles.body}>You will be asked to confirm the Google account linked to ToolTrack.</Text>}
      <Button title={passwordLoginEnabled ? 'Delete permanently' : 'Confirm with Google and delete'} variant="danger" loading={deleting} onPress={confirmDeletion} />
    </Card>
  </Screen>;
}

const styles = StyleSheet.create({
  name: { color: colors.ink, fontSize: 20, fontWeight: '900' },
  meta: { color: colors.muted, marginTop: 4 },
  cardButton: { marginTop: 16 },
  linkCard: { gap: 10 },
  body: { color: colors.muted, lineHeight: 21 },
  dangerCard: { gap: 14, borderColor: '#F3C1BA' },
  dangerTitle: { color: colors.danger, fontSize: 17, fontWeight: '900' },
});
