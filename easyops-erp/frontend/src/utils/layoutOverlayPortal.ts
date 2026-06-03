import { createPortal } from 'react-dom';
import type { ReactNode } from 'react';

/** Above MainLayout AppBar (~1298) / drawer (~1297–1299); use with portal to document.body */
export const LAYOUT_OVERLAY_ROOT_Z = 1400;

/** For dialogs opened on top of another portaled overlay (e.g. link-medication over view modal) */
export const LAYOUT_OVERLAY_NESTED_Z = 1500;

/** MainLayout MutationObserver treats this like MUI Dialog for nav pointer-events */
export const LAYOUT_OVERLAY_DETECT_CLASS = 'hospital-layout-overlay';

export function portalLayoutOverlay(node: ReactNode) {
  return createPortal(node, document.body);
}
