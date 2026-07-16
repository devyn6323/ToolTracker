export const passwordRequirements = 'Use 8+ characters with uppercase, lowercase, a number, and a special character.';

export function validatePassword(password: string) {
  if (password.length < 8 || password.length > 72) return passwordRequirements;
  if (!/[a-z]/.test(password) || !/[A-Z]/.test(password)
      || !/\d/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
    return passwordRequirements;
  }
  return undefined;
}
