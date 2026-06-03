import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import api from '../services/api';
import type { ModuleKey } from '../config/modules';
import {
  getModuleFromResource,
  loadStaticModuleConfig,
  type ModuleConfig,
} from '../config/modules';

interface ModuleConfigContextValue {
  /** Whether config has been loaded from API */
  loaded: boolean;
  /** Check if a module is enabled */
  isModuleEnabled: (module: ModuleKey) => boolean;
  /** Get module from resource name (for menu) */
  getModuleFromResource: (resource: string) => ModuleKey | null;
  /** Full config for advanced use */
  moduleConfig: Record<ModuleKey, ModuleConfig>;
}

const ModuleConfigContext = createContext<ModuleConfigContextValue | null>(null);

export function ModuleConfigProvider({ children }: { children: React.ReactNode }) {
  const [loaded, setLoaded] = useState(false);
  const [enabledModules, setEnabledModules] = useState<string[] | null>(null);
  const [staticFallback] = useState(() => loadStaticModuleConfig());

  const fetchConfig = useCallback(async () => {
    try {
      const res = await api.get<{ enabledModules: string[] }>('/api/config/modules');
      setEnabledModules(res.data.enabledModules?.map((m) => m.toLowerCase()) ?? []);
    } catch (err) {
      console.warn('Failed to fetch module config from API, using fallback:', err);
      setEnabledModules(null); // Use static fallback
    } finally {
      setLoaded(true);
    }
  }, []);

  useEffect(() => {
    fetchConfig();
  }, [fetchConfig]);

  const isModuleEnabled = useCallback(
    (module: ModuleKey): boolean => {
      if (!loaded) {
        return staticFallback[module]?.enabled ?? false;
      }
      if (enabledModules !== null) {
        return enabledModules.includes(module);
      }
      return staticFallback[module]?.enabled ?? false;
    },
    [loaded, enabledModules, staticFallback]
  );

  const moduleConfig = React.useMemo(() => {
    if (!loaded || enabledModules === null) {
      return staticFallback;
    }
    const config = { ...staticFallback };
    (Object.keys(config) as ModuleKey[]).forEach((key) => {
      config[key] = {
        ...config[key],
        enabled: enabledModules.includes(key),
      };
    });
    return config;
  }, [loaded, enabledModules, staticFallback]);

  const value: ModuleConfigContextValue = {
    loaded,
    isModuleEnabled,
    getModuleFromResource,
    moduleConfig,
  };

  return (
    <ModuleConfigContext.Provider value={value}>
      {children}
    </ModuleConfigContext.Provider>
  );
}

export function useModuleConfig(): ModuleConfigContextValue {
  const ctx = useContext(ModuleConfigContext);
  if (!ctx) {
    throw new Error('useModuleConfig must be used within ModuleConfigProvider');
  }
  return ctx;
}
