import { Component, ErrorInfo, PropsWithChildren } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from '../theme';

interface State { failed: boolean }

export class AppErrorBoundary extends Component<PropsWithChildren, State> {
  state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    if (__DEV__) console.error('ToolTrack render failure', error, info.componentStack);
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return <View style={styles.container}>
      <Text style={styles.mark}>!</Text>
      <Text style={styles.title}>ToolTrack hit a snag.</Text>
      <Text style={styles.message}>Your data is still safe. Try reopening this screen. If the problem continues, contact support.</Text>
      <Pressable style={styles.button} onPress={() => this.setState({ failed: false })}>
        <Text style={styles.buttonText}>Try again</Text>
      </Pressable>
      <Text style={styles.credit}>Created by Flightline Software</Text>
    </View>;
  }
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.paper, alignItems: 'center', justifyContent: 'center', padding: 30, gap: 14 },
  mark: { width: 58, height: 58, borderRadius: 18, textAlign: 'center', textAlignVertical: 'center', backgroundColor: '#FDEBE8', color: colors.danger, fontSize: 30, fontWeight: '900' },
  title: { color: colors.ink, fontSize: 24, fontWeight: '900', textAlign: 'center' },
  message: { color: colors.muted, lineHeight: 22, textAlign: 'center', maxWidth: 360 },
  button: { marginTop: 8, minHeight: 50, minWidth: 160, borderRadius: 14, backgroundColor: colors.orange, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 22 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '800' },
  credit: { position: 'absolute', bottom: 42, color: colors.muted, fontSize: 12, fontWeight: '600' },
});
