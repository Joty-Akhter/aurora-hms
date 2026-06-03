import axios, { AxiosResponse } from 'axios';
import api from './api';
import type { DoctorEPConfig, EpRecentRxEntry, PrescriptionTemplate } from './hospitalService';
import {
  EP_DOCTOR_CONFIG_STORAGE_KEY,
  EP_DOCTOR_CONFIG_UPDATED_EVENT,
  epConfigService,
  epRecentRxService,
  epTemplateService,
} from './hospitalService';

const EP_TEMPLATES_KEY = 'ep_prescription_templates';
const EP_RECENT_RX_KEY = 'ep_recent_prescriptions';
const EP_QUICK_PREFIX = 'ep_quick_mode_default';
const EP_DRAFT_PREFIX = 'ep_draft_';

/** localStorage key that persists the last acknowledged server optimistic-lock version. */
const EP_WORKSPACE_VERSION_KEY = 'ep_workspace_server_version';

/**
 * Opaque blob for top-level keys from a newer schema version that this client does not
 * understand (EP-10 forward-compatibility).  Persisted and re-emitted verbatim on every
 * push so the server never loses data written by a newer client that this client loaded.
 */
const EP_WORKSPACE_EXTENSIONS_KEY = 'ep_workspace_unknown_extensions';

// ---------------------------------------------------------------------------
// Schema versioning (EP-10)
// ---------------------------------------------------------------------------

/**
 * The payload shape this client natively understands.
 * Increment when the shape changes and add a matching entry to MIGRATIONS below.
 * Every structural change (new key, renamed key, changed nested structure) requires
 * a bump; additive-only changes may be handled via extensions instead.
 */
export const CURRENT_SCHEMA_VERSION = 1;

/**
 * Exhaustive set of top-level keys that belong to schemaVersion 1.
 * Any key arriving from the server that is NOT in this set is an extension from
 * a newer schema and must be preserved, not dropped.
 */
const KNOWN_PAYLOAD_KEYS: ReadonlySet<string> = new Set([
  'schemaVersion',
  'doctorConfig',
  'doctorTemplates',
  'recentRx',
  'drafts',
  'epQuickModeDefault',
]);

// ---------------------------------------------------------------------------
// Read-time migration chain (EP-10)
// ---------------------------------------------------------------------------

type RawPayload = Record<string, unknown>;

interface Migration {
  /** The schemaVersion number this entry upgrades FROM. */
  fromVersion: number;
  /** Pure transform: returns a new object at fromVersion + 1 (must set schemaVersion). */
  migrate(payload: RawPayload): RawPayload;
}

/**
 * Ordered migration chain.  Each entry upgrades one schema version to the next.
 * RULES:
 *   - Never remove or reorder existing entries.
 *   - Each migrate() must set schemaVersion to fromVersion + 1.
 *   - Keep entries pure (no side-effects, no localStorage access).
 *
 * v0 → v1:  Payloads written before the schemaVersion field was introduced have
 *            the same field set as v1; the only change is injecting the version tag.
 */
const MIGRATIONS: Migration[] = [
  {
    fromVersion: 0,
    migrate(p) {
      return { ...p, schemaVersion: 1 };
    },
  },
  // Template for the next migration when schemaVersion 2 is introduced:
  // {
  //   fromVersion: 1,
  //   migrate(p) {
  //     // Example: rename 'epQuickModeDefault' → 'quickModeDefault'
  //     const { epQuickModeDefault, ...rest } = p;
  //     return { ...rest, quickModeDefault: epQuickModeDefault, schemaVersion: 2 };
  //   },
  // },
];

/**
 * Walks the migration chain until the payload reaches CURRENT_SCHEMA_VERSION.
 * Returns the transformed payload and whether any migration actually ran.
 * Migrations are safe to re-run on an already-current payload (no-op).
 */
function migratePayload(raw: RawPayload): { payload: RawPayload; migrated: boolean } {
  let p = { ...raw };
  let version = typeof p.schemaVersion === 'number' ? p.schemaVersion : 0;
  let migrated = false;

  for (const m of MIGRATIONS) {
    if (version === m.fromVersion && version < CURRENT_SCHEMA_VERSION) {
      p = m.migrate(p);
      migrated = true;
      version = typeof p.schemaVersion === 'number' ? p.schemaVersion : version + 1;
    }
  }

  return { payload: p, migrated };
}

// ---------------------------------------------------------------------------
// Unknown-key (extension) persistence
// ---------------------------------------------------------------------------

/** Reads the persisted extension blob from localStorage (empty object if absent/corrupt). */
function loadExtensions(): RawPayload {
  try {
    const raw = localStorage.getItem(EP_WORKSPACE_EXTENSIONS_KEY);
    if (!raw) return {};
    const parsed: unknown = JSON.parse(raw);
    return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as RawPayload)
      : {};
  } catch {
    return {};
  }
}

/** Writes the extension blob.  Removes the key when there are no extensions. */
function saveExtensions(extensions: RawPayload): void {
  try {
    if (Object.keys(extensions).length === 0) {
      localStorage.removeItem(EP_WORKSPACE_EXTENSIONS_KEY);
    } else {
      localStorage.setItem(EP_WORKSPACE_EXTENSIONS_KEY, JSON.stringify(extensions));
    }
  } catch {
    // ignore — extension loss is acceptable; data loss is not
  }
}

/**
 * Splits the raw server payload into known and unknown top-level keys.
 * Unknown keys are persisted so the next buildEpWorkspacePayload() re-includes them,
 * preventing silent data loss when a newer server writes fields this client cannot read.
 */
function persistExtensions(raw: RawPayload): void {
  const extensions: RawPayload = {};
  for (const [key, value] of Object.entries(raw)) {
    if (!KNOWN_PAYLOAD_KEYS.has(key)) {
      extensions[key] = value;
    }
  }
  saveExtensions(extensions);
}

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

let applyingRemote = false;

export interface EpWorkspacePayload {
  schemaVersion: number;
  doctorConfig?: DoctorEPConfig;
  doctorTemplates?: PrescriptionTemplate[];
  recentRx?: EpRecentRxEntry[];
  /** patientId → draft JSON string */
  drafts?: Record<string, string>;
  epQuickModeDefault?: string | null;
}

// ---------------------------------------------------------------------------
// Server optimistic-lock version helpers
// ---------------------------------------------------------------------------

function getStoredVersion(): number {
  try {
    return parseInt(localStorage.getItem(EP_WORKSPACE_VERSION_KEY) ?? '0', 10) || 0;
  } catch {
    return 0;
  }
}

function storeVersion(version: number): void {
  try {
    localStorage.setItem(EP_WORKSPACE_VERSION_KEY, String(version));
  } catch {
    // ignore
  }
}

function extractVersionHeader(headers: Record<string, string> | undefined): number | null {
  if (!headers) return null;
  const raw = headers['x-ep-workspace-version'] ?? headers['X-EP-Workspace-Version'];
  if (raw === undefined || raw === null) return null;
  const parsed = parseInt(String(raw), 10);
  return isNaN(parsed) ? null : parsed;
}

// ---------------------------------------------------------------------------
// Draft collection
// ---------------------------------------------------------------------------

function collectDraftKeys(): Record<string, string> {
  const out: Record<string, string> = {};
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (!k || !k.startsWith(EP_DRAFT_PREFIX)) continue;
      const pid = k.slice(EP_DRAFT_PREFIX.length);
      const v = localStorage.getItem(k);
      if (v) out[pid] = v;
    }
  } catch {
    // ignore
  }
  return out;
}

// ---------------------------------------------------------------------------
// Payload building
// ---------------------------------------------------------------------------

/**
 * Assembles the workspace blob to push to the server.
 *
 * Extensions (unknown keys preserved from a newer server schema) are spread first
 * so that the explicitly known fields always shadow any collision, and so that
 * CURRENT_SCHEMA_VERSION can never be silently overwritten by an extension blob.
 */
export function buildEpWorkspacePayload(): EpWorkspacePayload & Record<string, unknown> {
  const extensions = loadExtensions();

  const payload: EpWorkspacePayload & Record<string, unknown> = {
    // Extensions from a newer schema version come first — known fields below override them.
    ...extensions,
    schemaVersion: CURRENT_SCHEMA_VERSION,
    doctorConfig: epConfigService.get(),
    doctorTemplates: epTemplateService.getDoctorOwned(),
    recentRx: epRecentRxService.list(),
    drafts: collectDraftKeys(),
    epQuickModeDefault: (() => {
      try {
        return localStorage.getItem(EP_QUICK_PREFIX);
      } catch {
        return null;
      }
    })(),
  };

  return payload;
}

// ---------------------------------------------------------------------------
// Payload applying
// ---------------------------------------------------------------------------

/**
 * Applies a server-fetched workspace payload to localStorage.
 *
 * Schema-evolution rules (EP-10):
 *
 *  schemaVersion < CURRENT  →  read-time migration.
 *    Run the migration chain in memory, apply the migrated result, return
 *    { migrated: true } so the caller can write the upgraded blob back to the
 *    server (keeps the server blob in sync with the current schema).
 *
 *  schemaVersion === CURRENT  →  normal apply.
 *    Clear any stale extension data; apply all known fields.
 *
 *  schemaVersion > CURRENT  →  forward-compatibility (newer server).
 *    Persist unknown top-level keys into EP_WORKSPACE_EXTENSIONS_KEY so the next
 *    push re-includes them verbatim.  Apply only fields this client understands.
 *    Return { migrated: false } — no write-back needed.
 */
export function applyEpWorkspacePayload(
  payload: EpWorkspacePayload | Record<string, unknown>,
): { migrated: boolean } {
  applyingRemote = true;
  let migrated = false;

  try {
    const raw = payload as RawPayload;
    const incomingVersion: number =
      typeof raw.schemaVersion === 'number' ? raw.schemaVersion : 0;

    let working: RawPayload;

    if (incomingVersion > CURRENT_SCHEMA_VERSION) {
      // Newer server: extract and persist unknown keys before applying.
      persistExtensions(raw);
      working = raw;
    } else if (incomingVersion < CURRENT_SCHEMA_VERSION) {
      // Older blob: run read-time migration chain.
      const result = migratePayload(raw);
      working = result.payload;
      migrated = result.migrated;
      saveExtensions({});
    } else {
      // Same version: no migration, no extensions expected.
      saveExtensions({});
      working = raw;
    }

    const p = working as EpWorkspacePayload;

    if (p.doctorConfig && typeof p.doctorConfig === 'object') {
      try {
        localStorage.setItem(EP_DOCTOR_CONFIG_STORAGE_KEY, JSON.stringify(p.doctorConfig));
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent(EP_DOCTOR_CONFIG_UPDATED_EVENT));
        }
      } catch {
        // ignore
      }
    }
    if (Array.isArray(p.doctorTemplates)) {
      try {
        localStorage.setItem(EP_TEMPLATES_KEY, JSON.stringify(p.doctorTemplates));
      } catch {
        // ignore
      }
    }
    if (Array.isArray(p.recentRx)) {
      try {
        const valid = p.recentRx
          .slice(0, 20)
          .filter(
            (e): e is EpRecentRxEntry =>
              Boolean(
                e &&
                  typeof e === 'object' &&
                  typeof (e as EpRecentRxEntry).prescriptionId === 'string' &&
                  typeof (e as EpRecentRxEntry).patientId === 'string' &&
                  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
                    (e as EpRecentRxEntry).patientId,
                  ),
              ),
          );
        localStorage.setItem(EP_RECENT_RX_KEY, JSON.stringify(valid));
      } catch {
        // ignore
      }
    }
    if (p.drafts && typeof p.drafts === 'object') {
      for (const [patientId, draftValue] of Object.entries(p.drafts)) {
        if (!draftValue || typeof draftValue !== 'string') continue;
        try {
          localStorage.setItem(`${EP_DRAFT_PREFIX}${patientId}`, draftValue);
        } catch {
          // ignore
        }
      }
    }
    if (p.epQuickModeDefault !== undefined && p.epQuickModeDefault !== null) {
      try {
        localStorage.setItem(EP_QUICK_PREFIX, String(p.epQuickModeDefault));
      } catch {
        // ignore
      }
    }
    try {
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('ep-doctor-config-updated'));
      }
    } catch {
      // ignore
    }
  } finally {
    applyingRemote = false;
  }

  return { migrated };
}

// ---------------------------------------------------------------------------
// Push scheduling (EP-8)
//
// Two-timer model:
//   debounceTimer – reset on every scheduleEpWorkspacePush() call;
//                   fires 1.8 s after the last call in a burst.
//   ceilingTimer  – started on the FIRST call of a new burst, never reset;
//                   fires 30 s later as a hard upper bound so data is never
//                   withheld for more than 30 s while the user is still typing.
//
// Whichever fires first clears the other and triggers pushEpWorkspaceToServer().
// Both are cleared after each successful push.
//
// Payload diff: buildEpWorkspacePayload() is serialised and compared to the
// last successfully pushed JSON.  A byte-identical payload is silently skipped,
// preventing database writes when the auto-save timer fires with no local change.
// ---------------------------------------------------------------------------

const DEBOUNCE_MS = 1_800;
const CEILING_MS = 30_000;

let debounceTimer: ReturnType<typeof setTimeout> | null = null;
let ceilingTimer: ReturnType<typeof setTimeout> | null = null;

/** JSON of the last payload we successfully pushed, used for diff-skip. */
let lastPushedPayloadJson: string | null = null;

function clearAllPushTimers(): void {
  if (debounceTimer !== null) { clearTimeout(debounceTimer); debounceTimer = null; }
  if (ceilingTimer !== null)  { clearTimeout(ceilingTimer);  ceilingTimer  = null; }
}

export function scheduleEpWorkspacePush(): void {
  if (applyingRemote) return;

  // Reset the debounce on every call so it always fires 1.8 s after the last change.
  if (debounceTimer !== null) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    debounceTimer = null;
    // Cancel the ceiling — the debounce won the race.
    if (ceilingTimer !== null) { clearTimeout(ceilingTimer); ceilingTimer = null; }
    void pushEpWorkspaceToServer();
  }, DEBOUNCE_MS);

  // Start the ceiling only once per burst (when no ceiling is already running).
  if (ceilingTimer === null) {
    ceilingTimer = setTimeout(() => {
      ceilingTimer = null;
      // Cancel the debounce — the ceiling is forcing a flush now.
      if (debounceTimer !== null) { clearTimeout(debounceTimer); debounceTimer = null; }
      void pushEpWorkspaceToServer();
    }, CEILING_MS);
  }
}

// ---------------------------------------------------------------------------
// Pull
// ---------------------------------------------------------------------------

export async function pullEpWorkspaceFromServer(): Promise<boolean> {
  try {
    const org = localStorage.getItem('currentOrganizationId');
    if (!org) return false;
    const res = await api.get<Record<string, unknown>>('/api/hospital/easy-prescription/workspace');
    const data = res.data;
    if (!data || typeof data !== 'object') return false;
    if (Object.keys(data).length === 0) return false;

    const version = extractVersionHeader(res.headers as Record<string, string>);
    if (version !== null) {
      storeVersion(version);
    }

    const { migrated } = applyEpWorkspacePayload(data as unknown as EpWorkspacePayload);

    // EP-10 read-time migration write-back: if migration ran the server still holds the
    // old schema blob.  Schedule a push to upgrade the server copy immediately.
    // applyingRemote is already false here (set in applyEpWorkspacePayload's finally block).
    if (migrated) {
      scheduleEpWorkspacePush();
    }

    return true;
  } catch {
    return false;
  }
}

// ---------------------------------------------------------------------------
// Push
// ---------------------------------------------------------------------------

export async function pushEpWorkspaceToServer(): Promise<boolean> {
  try {
    const org = localStorage.getItem('currentOrganizationId');
    if (!org) return false;

    const body = buildEpWorkspacePayload();

    // ── Payload diff check ───────────────────────────────────────────────────
    // Skip the PUT entirely when the payload is byte-identical to the last
    // successful push.  This prevents unnecessary writes when the auto-save
    // ceiling fires but no local state has actually changed.
    const bodyJson = JSON.stringify(body);
    if (bodyJson === lastPushedPayloadJson) {
      return true;
    }
    // ─────────────────────────────────────────────────────────────────────────

    const clientVersion = getStoredVersion();
    const res = await api.put('/api/hospital/easy-prescription/workspace', body, {
      headers: { 'X-EP-Client-Version': String(clientVersion) },
    });

    const version = extractVersionHeader(res.headers as Record<string, string>);
    if (version !== null) {
      storeVersion(version);
    }
    lastPushedPayloadJson = bodyJson;
    clearAllPushTimers();
    return true;
  } catch (err: unknown) {
    if (axios.isAxiosError(err) && err.response?.status === 409) {
      return handleWorkspaceConflict409(err.response);
    }
    if (axios.isAxiosError(err) && err.response?.status === 429) {
      return handleWorkspaceRateLimit429(err.response);
    }
    return false;
  }
}

// ---------------------------------------------------------------------------
// Rate-limit handling (429)
//
// The server enforces max 1 write per 2 seconds per user.  When the client is
// rate limited it schedules a single delayed retry after the server's
// Retry-After window expires (+100 ms buffer).  The push timer state is left
// clean — the retry itself acts as the flush.
// ---------------------------------------------------------------------------

function parseRetryAfterMs(
  headers: Record<string, string> | undefined,
): number {
  if (!headers) return WorkspaceRateLimiter.DEFAULT_RETRY_AFTER_MS;
  const raw = headers['retry-after'] ?? headers['Retry-After'];
  if (raw === undefined || raw === null) return WorkspaceRateLimiter.DEFAULT_RETRY_AFTER_MS;
  const seconds = parseFloat(String(raw));
  return isNaN(seconds) ? WorkspaceRateLimiter.DEFAULT_RETRY_AFTER_MS : seconds * 1_000;
}

/** Client-side mirror of the server's rate-limit constant (kept in sync manually). */
const WorkspaceRateLimiter = {
  DEFAULT_RETRY_AFTER_MS: 2_000,
} as const;

async function handleWorkspaceRateLimit429(
  response: AxiosResponse<{ retryAfterMs?: number }>,
): Promise<boolean> {
  const retryAfterMs =
    response.data?.retryAfterMs ??
    parseRetryAfterMs(response.headers as Record<string, string>);

  // Schedule a single retry after the window expires.  We deliberately do NOT
  // start the ceiling timer here — this retry is authoritative and should fire once.
  setTimeout(() => {
    void pushEpWorkspaceToServer();
  }, retryAfterMs + 100);

  return false;
}

// ---------------------------------------------------------------------------
// Conflict resolution (409)
//
// Strategy: server wins for everything except the client's own per-patient
// drafts, which are overlaid on top of the server state.  This ensures:
//   - Global settings (config, templates, recentRx) converge to server truth.
//   - The active doctor's in-flight draft for a patient is never silently lost.
// After merging, one automatic retry is attempted.  If the retry also fails
// (e.g. a third concurrent writer), the function returns false and the next
// scheduled push will try again with the updated version.
// ---------------------------------------------------------------------------

async function handleWorkspaceConflict409(
  response: AxiosResponse<{ serverVersion?: number; conflict?: boolean }>,
): Promise<boolean> {
  const serverVersion = response.data?.serverVersion;
  if (typeof serverVersion === 'number') {
    storeVersion(serverVersion);
  }

  // Snapshot local drafts before pulling (pull will overwrite localStorage).
  const localDrafts = collectDraftKeys();

  const pulled = await pullEpWorkspaceFromServer();
  if (!pulled) return false;

  // Re-apply local drafts on top of the freshly-pulled server state.
  // Local draft wins per patientId so the doctor doesn't lose in-progress work.
  try {
    for (const [patientId, draft] of Object.entries(localDrafts)) {
      localStorage.setItem(`${EP_DRAFT_PREFIX}${patientId}`, draft);
    }
  } catch {
    // ignore — worst case drafts come from server
  }

  // Notify the UI so a toast / banner can be shown.
  if (typeof window !== 'undefined') {
    window.dispatchEvent(
      new CustomEvent('ep-workspace-conflict', {
        detail: {
          message:
            'Your prescription workspace was refreshed because another session saved changes. ' +
            'Your in-progress drafts have been preserved.',
        },
      }),
    );
  }

  // Single retry with the merged state and the updated server version.
  try {
    const body = buildEpWorkspacePayload();
    const bodyJson = JSON.stringify(body);
    const clientVersion = getStoredVersion();
    const res = await api.put('/api/hospital/easy-prescription/workspace', body, {
      headers: { 'X-EP-Client-Version': String(clientVersion) },
    });
    const version = extractVersionHeader(res.headers as Record<string, string>);
    if (version !== null) {
      storeVersion(version);
    }
    lastPushedPayloadJson = bodyJson;
    clearAllPushTimers();
    return true;
  } catch {
    // If the retry fails the next scheduled push will try again.
    return false;
  }
}
