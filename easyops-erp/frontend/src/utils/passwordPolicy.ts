export type PasswordPolicyChecks = {
  minLength: boolean;
  uppercase: boolean;
  lowercase: boolean;
  digit: boolean;
  special: boolean;
};

export function evaluatePasswordPolicy(password: string): PasswordPolicyChecks {
  return {
    minLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    digit: /\d/.test(password),
    special: /[^A-Za-z0-9]/.test(password),
  };
}

export function passwordMeetsPolicy(password: string): boolean {
  const p = evaluatePasswordPolicy(password);
  return p.minLength && p.uppercase && p.lowercase && p.digit && p.special;
}

export const PASSWORD_REQUIREMENTS: { key: keyof PasswordPolicyChecks; label: string }[] = [
  { key: 'minLength', label: 'At least 8 characters' },
  { key: 'uppercase', label: 'One uppercase letter' },
  { key: 'lowercase', label: 'One lowercase letter' },
  { key: 'digit', label: 'One number' },
  { key: 'special', label: 'One special character' },
];
