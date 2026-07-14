import { NavigationContainer } from '@react-navigation/native';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider, useAuth } from './src/auth/AuthContext';
import { LoadingScreen } from './src/components/LoadingScreen';
import { AppErrorBoundary } from './src/components/AppErrorBoundary';
import { AppNavigator } from './src/navigation/AppNavigator';
import { AuthNavigator } from './src/navigation/AuthNavigator';
import { navigationTheme } from './src/theme';

function Root() {
  const { session, restoring } = useAuth();

  if (restoring) return <LoadingScreen label="Opening ToolTrack" />;

  return (
    <NavigationContainer theme={navigationTheme}>
      {session ? <AppNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <AppErrorBoundary>
        <AuthProvider>
          <StatusBar style="dark" />
          <Root />
        </AuthProvider>
      </AppErrorBoundary>
    </SafeAreaProvider>
  );
}
