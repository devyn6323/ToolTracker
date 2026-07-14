import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { RegisterScreen } from '../screens/auth/RegisterScreen';
import { colors } from '../theme';
import { AuthStackParams } from '../types';

const Stack = createNativeStackNavigator<AuthStackParams>();

export function AuthNavigator() {
  return <Stack.Navigator screenOptions={{ headerShadowVisible: false, headerStyle: { backgroundColor: colors.paper }, headerTintColor: colors.navy }}>
    <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
    <Stack.Screen name="Register" component={RegisterScreen} options={{ title: 'Create company' }} />
  </Stack.Navigator>;
}
