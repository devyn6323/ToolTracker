import { DefaultTheme } from '@react-navigation/native';

export const colors = {
  ink: '#14212B',
  navy: '#17384F',
  blue: '#23648A',
  orange: '#F28C28',
  orangeDark: '#C9630E',
  paper: '#F4F6F4',
  surface: '#FFFFFF',
  line: '#DDE3E2',
  muted: '#6D7B83',
  success: '#25825A',
  danger: '#C8463A',
  warning: '#D98316',
  paleBlue: '#E8F1F5',
  paleOrange: '#FFF0DE',
};

export const navigationTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    primary: colors.orange,
    background: colors.paper,
    card: colors.surface,
    text: colors.ink,
    border: colors.line,
    notification: colors.danger,
  },
};

export const shadow = {
  shadowColor: '#0F2635',
  shadowOffset: { width: 0, height: 4 },
  shadowOpacity: 0.08,
  shadowRadius: 12,
  elevation: 3,
};
