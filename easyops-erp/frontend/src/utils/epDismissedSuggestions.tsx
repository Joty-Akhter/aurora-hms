import type React from 'react';

/** Per-user, per-organization lines hidden from EP advice / investigation autocomplete dropdowns. */

export type EpDismissedStore = {
  advice: string[];
  tests: string[];
};

const STORAGE_PREFIX = 'ep-dismissed-suggestions';

function storageKey(userId: string, organizationId?: string): string {
  const org = organizationId?.trim() || '_';
  return `${STORAGE_PREFIX}:${userId}:${org}`;
}

/** Legacy key before organization scoping (user id only). */
function legacyStorageKey(userId: string): string {
  return `${STORAGE_PREFIX}:${userId}`;
}

function normalizeKey(raw: string): string {
  return raw.trim().replace(/\s+/g, ' ').toLowerCase();
}

function parseStore(raw: string | null): EpDismissedStore {
  if (!raw) return { advice: [], tests: [] };
  try {
    const parsed = JSON.parse(raw) as Partial<EpDismissedStore>;
    return {
      advice: Array.isArray(parsed.advice) ? parsed.advice.filter((s): s is string => typeof s === 'string') : [],
      tests: Array.isArray(parsed.tests) ? parsed.tests.filter((s): s is string => typeof s === 'string') : [],
    };
  } catch {
    return { advice: [], tests: [] };
  }
}

export function loadEpDismissedSuggestions(
  userId: string | undefined,
  organizationId?: string | undefined,
): EpDismissedStore {
  if (!userId) return { advice: [], tests: [] };
  const key = storageKey(userId, organizationId);
  let store = parseStore(localStorage.getItem(key));
  if (store.advice.length === 0 && store.tests.length === 0) {
    const legacy = parseStore(localStorage.getItem(legacyStorageKey(userId)));
    if (legacy.advice.length > 0 || legacy.tests.length > 0) {
      store = legacy;
      localStorage.setItem(key, JSON.stringify(store));
    }
  }
  return store;
}

function save(userId: string, organizationId: string | undefined, store: EpDismissedStore): void {
  localStorage.setItem(storageKey(userId, organizationId), JSON.stringify(store));
}

export function isEpSuggestionDismissed(
  store: EpDismissedStore,
  kind: 'advice' | 'tests',
  line: string,
): boolean {
  const key = normalizeKey(line);
  if (!key) return false;
  return store[kind].some(x => normalizeKey(x) === key);
}

export function dismissEpSuggestionLocally(
  userId: string | undefined,
  organizationId: string | undefined,
  kind: 'advice' | 'tests',
  line: string,
): EpDismissedStore {
  if (!userId) return { advice: [], tests: [] };
  const canon = line.trim().replace(/\s+/g, ' ');
  if (!canon) return loadEpDismissedSuggestions(userId, organizationId);
  const store = loadEpDismissedSuggestions(userId, organizationId);
  const key = normalizeKey(canon);
  const list = store[kind];
  if (!list.some(x => normalizeKey(x) === key)) {
    store[kind] = [...list, canon];
    save(userId, organizationId, store);
  }
  return store;
}

export function filterEpAutocompleteOptions(
  options: string[],
  store: EpDismissedStore,
  kind: 'advice' | 'tests',
): string[] {
  return options.filter(o => !isEpSuggestionDismissed(store, kind, o));
}

/** MUI Autocomplete option row with dismiss control for EP suggestion lists. */
export function renderEpDismissibleOption(
  props: React.HTMLAttributes<HTMLLIElement>,
  option: string,
  onDismiss: (line: string) => void,
  canDismiss = true,
) {
  const { key, ...liProps } = props as React.HTMLAttributes<HTMLLIElement> & { key?: React.Key };
  return (
    <li
      key={key ?? option}
      {...liProps}
      style={{
        ...(liProps.style ?? {}),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 8,
      }}
    >
      <span style={{ flex: 1, minWidth: 0 }}>{option}</span>
      {canDismiss ? (
        <button
          type="button"
          title="Remove from suggestions"
          aria-label={`Remove ${option} from suggestions`}
          onMouseDown={e => e.preventDefault()}
          onClick={e => {
            e.stopPropagation();
            onDismiss(option);
          }}
          style={{
            flexShrink: 0,
            background: 'none',
            border: 'none',
            color: '#9ca3af',
            cursor: 'pointer',
            fontSize: '16px',
            lineHeight: 1,
            padding: '0 2px',
          }}
        >
          ×
        </button>
      ) : null}
    </li>
  );
}
