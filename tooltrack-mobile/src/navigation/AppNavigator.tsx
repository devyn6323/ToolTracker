import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { StyleSheet, Text } from 'react-native';
import { ActivityScreen } from '../screens/ActivityScreen';
import { CheckoutScreen } from '../screens/CheckoutScreen';
import { DashboardScreen } from '../screens/DashboardScreen';
import { EmployeesScreen } from '../screens/EmployeesScreen';
import { MyToolsScreen } from '../screens/MyToolsScreen';
import { ReturnScreen } from '../screens/ReturnScreen';
import { ScannerScreen } from '../screens/ScannerScreen';
import { ToolDetailScreen } from '../screens/ToolDetailScreen';
import { ToolFormScreen } from '../screens/ToolFormScreen';
import { ToolListScreen } from '../screens/ToolListScreen';
import { TransferScreen } from '../screens/TransferScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { colors } from '../theme';
import { AppStackParams, TabParams } from '../types';

const Stack = createNativeStackNavigator<AppStackParams>();
const Tabs = createBottomTabNavigator<TabParams>();
const symbols: Record<keyof TabParams, string> = { Home: '⌂', Inventory: '▦', Scan: '⌗', MyTools: '✓', Activity: '↻' };

function MainTabs() {
  return <Tabs.Navigator screenOptions={({ route }) => ({
    headerStyle: { backgroundColor: colors.paper }, headerShadowVisible: false, headerTitleStyle: { fontWeight: '900', color: colors.ink },
    tabBarActiveTintColor: colors.orangeDark, tabBarInactiveTintColor: colors.muted,
    tabBarStyle: { height: 70, paddingTop: 8, paddingBottom: 9, borderTopColor: colors.line, backgroundColor: colors.surface },
    tabBarLabelStyle: { fontSize: 10, fontWeight: '700' },
    tabBarIcon: ({ color }) => <Text style={[styles.tabIcon, { color }]}>{symbols[route.name]}</Text>,
  })}>
    <Tabs.Screen name="Home" component={DashboardScreen} options={{ title: 'Dashboard' }} />
    <Tabs.Screen name="Inventory" component={ToolListScreen} options={{ title: 'Tools' }} />
    <Tabs.Screen name="Scan" component={ScannerScreen} options={{ title: 'Scan QR' }} />
    <Tabs.Screen name="MyTools" component={MyToolsScreen} options={{ title: 'My tools' }} />
    <Tabs.Screen name="Activity" component={ActivityScreen} />
  </Tabs.Navigator>;
}

export function AppNavigator() {
  return <Stack.Navigator screenOptions={{ headerStyle: { backgroundColor: colors.paper }, headerShadowVisible: false, headerTintColor: colors.navy, headerTitleStyle: { fontWeight: '900' } }}>
    <Stack.Screen name="Main" component={MainTabs} options={{ headerShown: false }} />
    <Stack.Screen name="ToolDetail" component={ToolDetailScreen} options={{ title: 'Tool details' }} />
    <Stack.Screen name="ToolForm" component={ToolFormScreen} options={({ route }) => ({ title: route.params?.toolId ? 'Edit tool' : 'Add tool' })} />
    <Stack.Screen name="Checkout" component={CheckoutScreen} options={{ title: 'Check out tool' }} />
    <Stack.Screen name="Return" component={ReturnScreen} options={{ title: 'Return tool' }} />
    <Stack.Screen name="Transfer" component={TransferScreen} options={{ title: 'Transfer tool' }} />
    <Stack.Screen name="Scanner" component={ScannerScreen} options={{ title: 'Scan QR' }} />
    <Stack.Screen name="Employees" component={EmployeesScreen} options={{ title: 'Employees' }} />
    <Stack.Screen name="Settings" component={SettingsScreen} options={{ title: 'Account & privacy' }} />
  </Stack.Navigator>;
}

const styles = StyleSheet.create({ tabIcon: { fontSize: 23, fontWeight: '800', lineHeight: 25 } });
