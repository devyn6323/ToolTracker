import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { ApiError } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { Button, Card, ErrorBanner, Input, Screen } from '../../components/ui';
import { colors } from '../../theme';
import { AuthStackParams } from '../../types';

export function GoogleCompanyScreen({ route, navigation }: NativeStackScreenProps<AuthStackParams, 'GoogleCompany'>) {
  const { googleCreateCompany, changeGoogleAccount } = useAuth();
  const [companyName, setCompanyName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function createCompany() {
    if (companyName.trim().length < 2) return setError('Enter your company name.');
    setLoading(true); setError('');
    try { await googleCreateCompany(companyName.trim()); }
    catch (e) { setError(e instanceof ApiError || e instanceof Error ? e.message : 'Could not create the company.'); }
    finally { setLoading(false); }
  }

  async function useAnotherAccount() {
    await changeGoogleAccount();
    navigation.goBack();
  }

  return <Screen>
    <Text style={styles.eyebrow}>GOOGLE ACCOUNT VERIFIED</Text>
    <Text style={styles.title}>Set up your company</Text>
    <Text style={styles.body}>No ToolTrack account currently uses this email. Create a company to become its owner.</Text>
    <Card><Text style={styles.name}>{route.params.name}</Text><Text style={styles.email}>{route.params.email}</Text></Card>
    <ErrorBanner message={error} />
    <View style={styles.form}><Input label="Company name" value={companyName} onChangeText={setCompanyName} placeholder="Flightline Construction" autoCapitalize="words" /><Button title="Create company with Google" onPress={createCompany} loading={loading} /><Button title="Use another Google account" variant="ghost" onPress={useAnotherAccount} disabled={loading} /></View>
    <Text style={styles.help}>Joining an existing crew? Ask its owner or manager to add this exact Google email as an employee, then sign in again.</Text>
  </Screen>;
}

const styles = StyleSheet.create({ eyebrow: { color: colors.orangeDark, fontWeight: '900', fontSize: 11, letterSpacing: 1.5 }, title: { color: colors.ink, fontWeight: '900', fontSize: 31 }, body: { color: colors.muted, lineHeight: 22 }, name: { color: colors.ink, fontWeight: '900', fontSize: 18 }, email: { color: colors.muted, marginTop: 4 }, form: { gap: 13 }, help: { color: colors.muted, fontSize: 12, lineHeight: 19, textAlign: 'center' } });
