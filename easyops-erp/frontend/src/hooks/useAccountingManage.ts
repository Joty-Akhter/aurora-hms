import { useAuth } from '../contexts/AuthContext';

/** True when the user may create, update, post, or delete accounting data. */
export function useAccountingManage(): boolean {
  const { canManageResource } = useAuth();
  return canManageResource('accounting');
}
