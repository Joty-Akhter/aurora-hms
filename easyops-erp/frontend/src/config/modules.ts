/**
 * Module Configuration
 * 
 * Controls which modules are enabled/disabled in the frontend.
 * This allows different deployments (e.g., alien-pharma) to show only relevant modules.
 * 
 * Configuration can be set via:
 * 1. Environment variable: VITE_ENABLE_MODULE (comma-separated list, e.g., "inventory,hr,accounting,pharma")
 *    - For backward compatibility, VITE_ENABLED_MODULES is also supported.
 * 2. Default configuration in this file
 * 
 * Available modules:
 * - dashboard (always enabled)
 * - organizations (always enabled)
 * - accounting
 * - sales
 * - inventory
 * - purchase
 * - pharma
 * - hr
 * - crm
 * - manufacturing
 * - hospital
 * - users (always enabled)
 * - roles (always enabled)
 * - permissions (always enabled)
 */

export type ModuleKey = 
  | 'dashboard'
  | 'organizations'
  | 'accounting'
  | 'sales'
  | 'inventory'
  | 'purchase'
  | 'pharma'
  | 'hr'
  | 'crm'
  | 'manufacturing'
  | 'hospital'
  | 'users'
  | 'roles'
  | 'permissions';

export interface ModuleConfig {
  enabled: boolean;
  name: string;
}

const defaultModules: Record<ModuleKey, ModuleConfig> = {
  dashboard: { enabled: true, name: 'Dashboard' }, // Always enabled
  organizations: { enabled: true, name: 'Organizations' }, // Always enabled
  accounting: { enabled: true, name: 'Accounting' },
  sales: { enabled: true, name: 'Sales' },
  inventory: { enabled: true, name: 'Inventory' },
  purchase: { enabled: true, name: 'Purchase' },
  pharma: { enabled: true, name: 'Pharma' },
  hr: { enabled: true, name: 'HR' },
  crm: { enabled: true, name: 'CRM' },
  manufacturing: { enabled: true, name: 'Manufacturing' },
  hospital: { enabled: true, name: 'Hospital' },
  users: { enabled: true, name: 'Users' }, // Always enabled
  roles: { enabled: true, name: 'Roles' }, // Always enabled
  permissions: { enabled: true, name: 'Permissions' }, // Always enabled
};

/**
 * Alien-Pharma configuration (Hospital-focused deployment)
 * Enables: inventory, hr, accounting, hospital (Pharma disabled)
 */
const alienPharmaModules: Record<ModuleKey, ModuleConfig> = {
  dashboard: { enabled: true, name: 'Dashboard' },
  organizations: { enabled: true, name: 'Organizations' },
  accounting: { enabled: true, name: 'Accounting' },
  sales: { enabled: false, name: 'Sales' },
  inventory: { enabled: true, name: 'Inventory' },
  purchase: { enabled: false, name: 'Purchase' },
  pharma: { enabled: false, name: 'Pharma' },
  hr: { enabled: true, name: 'HR' },
  crm: { enabled: false, name: 'CRM' },
  manufacturing: { enabled: false, name: 'Manufacturing' },
  hospital: { enabled: true, name: 'Hospital' },
  users: { enabled: true, name: 'Users' },
  roles: { enabled: true, name: 'Roles' },
  permissions: { enabled: true, name: 'Permissions' },
};

/**
 * Get module configuration from environment variable or preset.
 * Used as fallback when API config is unavailable.
 * 
 * Environment variable format: VITE_ENABLE_MODULE=inventory,hr,accounting,pharma
 * Preset formats: VITE_MODULE_PRESET=alien-pharma
 */
export function loadStaticModuleConfig(): Record<ModuleKey, ModuleConfig> {
  // Check for preset configuration
  const preset = import.meta.env.VITE_MODULE_PRESET;
  if (preset === 'alien-pharma') {
    return alienPharmaModules;
  }

  // Check for explicit enabled modules list
  const enabledModulesStr =
    import.meta.env.VITE_ENABLE_MODULE || import.meta.env.VITE_ENABLED_MODULES;
  if (enabledModulesStr) {
    const enabledModules = enabledModulesStr.split(',').map((m: string) => m.trim().toLowerCase()) as ModuleKey[];
    const config = { ...defaultModules };
    
    // Disable all modules by default, then enable only specified ones
    Object.keys(config).forEach(key => {
      const moduleKey = key as ModuleKey;
      // Always keep core modules enabled
      if (moduleKey === 'dashboard' || moduleKey === 'organizations' || 
          moduleKey === 'users' || moduleKey === 'roles' || moduleKey === 'permissions') {
        config[moduleKey].enabled = true;
      } else {
        config[moduleKey].enabled = enabledModules.includes(moduleKey);
      }
    });
    
    return config;
  }

  // Default: enable all modules
  return defaultModules;
}

/**
 * Module configuration instance (static fallback - use ModuleConfigContext for runtime DB config)
 */
export const moduleConfig = loadStaticModuleConfig();

/**
 * Check if a module is enabled
 */
export function isModuleEnabled(module: ModuleKey): boolean {
  return moduleConfig[module]?.enabled ?? false;
}

/**
 * Get all enabled module keys
 */
export function getEnabledModules(): ModuleKey[] {
  return Object.entries(moduleConfig)
    .filter(([_, config]) => config.enabled)
    .map(([key, _]) => key as ModuleKey);
}

/**
 * Map menu resource names to module keys
 */
export function getModuleFromResource(resource: string): ModuleKey | null {
  const resourceToModule: Record<string, ModuleKey> = {
    dashboard: 'dashboard',
    organizations: 'organizations',
    accounting: 'accounting',
    sales: 'sales',
    inventory: 'inventory',
    purchase: 'purchase',
    pharma: 'pharma',
    hr: 'hr',
    crm: 'crm',
    manufacturing: 'manufacturing',
    hospital: 'hospital',
    users: 'users',
    roles: 'roles',
    permissions: 'permissions',
  };
  
  return resourceToModule[resource.toLowerCase()] || null;
}

