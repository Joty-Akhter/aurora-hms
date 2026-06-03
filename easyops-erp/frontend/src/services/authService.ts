import api, { API_GATEWAY_BASE_URL } from "./api";
import axios from "axios";
import organizationService from "./organizationService";
import rbacService from "./rbacService";
import type { Organization } from "@/types/organization";
import {
  LoginRequest,
  LoginResponse,
  PasswordResetRequest,
  PasswordResetConfirmRequest,
  PasswordResetOptionsResponse,
  ChangePasswordRequest,
} from "@/types/index";

const POST_LOGIN_ORG_SELECTION = "easops_postLoginOrgSelection";
const WORKSPACE_SCOPE_KEY = "workspaceScope";

/** Per-user last selected org (survives refresh; used after login). */
function preferredOrgStorageKey(userId: string): string {
  return `preferredOrganizationId:${userId}`;
}

export interface PendingOrg {
  id: string;
  name: string;
  code?: string | null;
  logo?: string | null;
  themeMode?: string | null;
  themePrimaryColor?: string | null;
  themeSecondaryColor?: string | null;
  themeAccentColor?: string | null;
  themeSidebarColor?: string | null;
  themeSidebarTextColor?: string | null;
}

export interface PendingOrganizationSelection {
  organizations: PendingOrg[];
  hasGlobalOption: boolean;
}

export interface OrgTheme {
  mode: "light" | "dark";
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  sidebarColor: string;
  sidebarTextColor: string;
}

export const DEFAULT_THEME: OrgTheme = {
  mode: "light",
  primaryColor: "#7b2a90",
  secondaryColor: "#05a79c",
  accentColor: "#05a79c",
  sidebarColor: "#7b2a90",
  sidebarTextColor: "#f1f5f9",
};

function applyOrganizationBranding(org: {
  id: string;
  name: string;
  logo?: string | null;
  themeMode?: string;
  themePrimaryColor?: string;
  themeSecondaryColor?: string;
  themeAccentColor?: string;
  themeSidebarColor?: string;
  themeSidebarTextColor?: string;
}) {
  localStorage.setItem("currentOrganizationId", org.id);
  localStorage.setItem("currentOrganizationName", org.name);
  localStorage.removeItem(WORKSPACE_SCOPE_KEY);
  if (org.logo) {
    localStorage.setItem("currentOrganizationLogo", org.logo);
  } else {
    localStorage.removeItem("currentOrganizationLogo");
  }
  const theme: OrgTheme = {
    mode: (org.themeMode as "light" | "dark") || DEFAULT_THEME.mode,
    primaryColor: org.themePrimaryColor || DEFAULT_THEME.primaryColor,
    secondaryColor: org.themeSecondaryColor || DEFAULT_THEME.secondaryColor,
    accentColor: org.themeAccentColor || DEFAULT_THEME.accentColor,
    sidebarColor: org.themeSidebarColor || DEFAULT_THEME.sidebarColor,
    sidebarTextColor:
      org.themeSidebarTextColor || DEFAULT_THEME.sidebarTextColor,
  };
  localStorage.setItem("currentOrganizationTheme", JSON.stringify(theme));
}

function clearOrganizationBranding() {
  localStorage.removeItem("currentOrganizationId");
  localStorage.removeItem("currentOrganizationName");
  localStorage.removeItem("currentOrganizationLogo");
  localStorage.removeItem("currentOrganizationTheme");
  localStorage.removeItem(WORKSPACE_SCOPE_KEY);
}

// Same base as ApiService (`api.ts`) so login/logout/refresh hit the same gateway as `/api/organizations/**`.
const authApi = axios.create({
  baseURL: API_GATEWAY_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

class AuthService {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await authApi.post<LoginResponse>(
      "/api/auth/login",
      credentials,
    );
    const data = response.data;

    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);
    const user = this.enrichUserFromAccessToken({
      id: data.userId,
      username: data.username,
      email: data.email,
      firstName: data.firstName,
      lastName: data.lastName,
    });
    localStorage.setItem("user", JSON.stringify(user));

    try {
      await this.resolveWorkspaceAfterLogin(String(data.userId));
    } catch (error) {
      console.error("Failed to resolve organization after login:", error);
    }

    return data;
  }

  /**
   * Tenant context from RBAC role assignments (distinct organization_id on user_roles), with
   * membership as a supplement. Multiple orgs or org + global roles → pending selection screen.
   */
  private async resolveWorkspaceAfterLogin(userId: string): Promise<void> {
    this.clearPendingOrganizationSelection();

    // Do not call GET /api/organizations here: that listing requires org/system RBAC (organizations:view).
    // Workspace resolution uses RBAC org context + GET /api/organizations/me (memberships) only.

    let ctx = { organizationIds: [] as string[], hasGlobalRoles: false };
    try {
      ctx = await rbacService.getUserOrganizationContext(userId);
    } catch (e) {
      console.warn(
        "[Auth] RBAC organization context unavailable; using memberships only",
        e,
      );
    }

    const [membershipOrgs] = await Promise.all([
      organizationService
        .getMyOrganizations(userId)
        .catch(() => [] as Organization[]),
    ]);

    const byId = new Map<string, Organization>();

    await Promise.all(
      (ctx.organizationIds || []).map(async (id) => {
        try {
          const org = await organizationService.getOrganizationById(id);
          byId.set(org.id, org);
        } catch {
          console.warn("[Auth] Unknown organization id from RBAC roles", id);
        }
      }),
    );

    for (const m of membershipOrgs) {
      if (!byId.has(m.id)) {
        byId.set(m.id, m);
      }
    }

    const candidates = Array.from(byId.values());
    const hasGlobal = ctx.hasGlobalRoles;
    const choiceCount = candidates.length + (hasGlobal ? 1 : 0);

    if (choiceCount === 0) {
      clearOrganizationBranding();
      console.warn(
        "No organization context from roles or memberships; pick an org in settings when assigned.",
      );
      return;
    }

    // Product rule: when there is exactly one organization in the system/context,
    // skip org selection and auto-enter that organization even if global roles exist.
    if (candidates.length === 1) {
      applyOrganizationBranding(candidates[0]);
      return;
    }

    if (choiceCount === 1) {
      // Only global context exists, no org membership.
      this.setGlobalWorkspace();
      return;
    }

    const pending: PendingOrganizationSelection = {
      organizations: candidates.map((o) => ({
        id: o.id,
        name: o.name,
        code: o.code,
        logo: o.logo,
        themeMode: o.themeMode,
        themePrimaryColor: o.themePrimaryColor,
        themeSecondaryColor: o.themeSecondaryColor,
        themeAccentColor: o.themeAccentColor,
        themeSidebarColor: o.themeSidebarColor,
        themeSidebarTextColor: o.themeSidebarTextColor,
      })),
      hasGlobalOption: hasGlobal,
    };
    sessionStorage.setItem(POST_LOGIN_ORG_SELECTION, JSON.stringify(pending));
    clearOrganizationBranding();
  }

  getPendingOrganizationSelection(): PendingOrganizationSelection | null {
    const raw = sessionStorage.getItem(POST_LOGIN_ORG_SELECTION);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as PendingOrganizationSelection;
    } catch {
      return null;
    }
  }

  clearPendingOrganizationSelection(): void {
    sessionStorage.removeItem(POST_LOGIN_ORG_SELECTION);
  }

  hasPendingOrganizationSelection(): boolean {
    return sessionStorage.getItem(POST_LOGIN_ORG_SELECTION) != null;
  }

  /**
   * Tenant for RBAC API: org id, or null for explicit global workspace, or undefined when no
   * tenant is chosen yet (do not call RBAC with null — that would return all roles).
   */
  getEffectiveOrganizationIdForRbac(): string | null | undefined {
    if (localStorage.getItem(WORKSPACE_SCOPE_KEY) === "global") {
      return null;
    }
    const id = localStorage.getItem("currentOrganizationId");
    return id ?? undefined;
  }

  setGlobalWorkspace(): void {
    localStorage.removeItem("currentOrganizationId");
    localStorage.setItem(
      "currentOrganizationName",
      "All organizations (global roles)",
    );
    localStorage.removeItem("currentOrganizationLogo");
    localStorage.setItem(WORKSPACE_SCOPE_KEY, "global");
  }

  async logout(): Promise<void> {
    try {
      await authApi.post("/api/auth/logout");
    } finally {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      localStorage.removeItem("currentOrganizationId");
      localStorage.removeItem("currentOrganizationName");
      localStorage.removeItem("currentOrganizationLogo");
      localStorage.removeItem(WORKSPACE_SCOPE_KEY);
      sessionStorage.removeItem(POST_LOGIN_ORG_SELECTION);
    }
  }

  async refreshToken(refreshToken: string): Promise<LoginResponse> {
    const response = await authApi.post<LoginResponse>("/api/auth/refresh", {
      refreshToken,
    });
    return response.data;
  }

  async getPasswordResetOptions(usernameOrEmail: string): Promise<PasswordResetOptionsResponse> {
    const response = await api.post<PasswordResetOptionsResponse>(
      "/api/auth/password/reset/options",
      { usernameOrEmail },
    );
    return response.data;
  }

  async resetPassword(data: PasswordResetRequest): Promise<void> {
    await api.post("/api/auth/password/reset", data);
  }

  async confirmPasswordReset(data: PasswordResetConfirmRequest): Promise<void> {
    await api.post("/api/auth/password/reset/confirm", data);
  }

  async changePassword(
    userId: string,
    data: ChangePasswordRequest,
  ): Promise<void> {
    await api.post(`/api/auth/password/change/${userId}`, data);
  }

  async validateToken(): Promise<boolean> {
    try {
      const response = await api.get<{ valid: boolean }>("/api/auth/validate");
      return response.data.valid;
    } catch {
      return false;
    }
  }

  getAccessToken(): string | null {
    return localStorage.getItem("accessToken");
  }

  getRefreshToken(): string | null {
    return localStorage.getItem("refreshToken");
  }

  getCurrentUser(): any | null {
    const userStr = localStorage.getItem("user");
    if (!userStr) return null;
    const user = JSON.parse(userStr);
    return this.enrichUserFromAccessToken(user);
  }

  /** Fill missing name fields from JWT claims (session restore / older stored user blobs). */
  private enrichUserFromAccessToken(user: Record<string, unknown>): Record<string, unknown> {
    if (user?.firstName || user?.lastName) return user;
    const token = this.getAccessToken();
    if (!token) return user;
    try {
      const payload = JSON.parse(atob(token.split(".")[1] ?? ""));
      const firstName = payload.firstName ?? payload.first_name;
      const lastName = payload.lastName ?? payload.last_name;
      if (!firstName && !lastName) return user;
      const enriched = { ...user, firstName, lastName };
      localStorage.setItem("user", JSON.stringify(enriched));
      return enriched;
    } catch {
      return user;
    }
  }

  getCurrentOrganizationId(): string | null {
    return localStorage.getItem("currentOrganizationId");
  }

  getCurrentOrganizationName(): string | null {
    return localStorage.getItem("currentOrganizationName");
  }

  getCurrentOrganizationLogo(): string | null {
    return localStorage.getItem("currentOrganizationLogo");
  }

  getCurrentOrganizationTheme(): OrgTheme {
    try {
      const raw = localStorage.getItem("currentOrganizationTheme");
      if (raw) return JSON.parse(raw) as OrgTheme;
    } catch {
      // ignore
    }
    return DEFAULT_THEME;
  }

  setCurrentOrganization(
    organizationId: string,
    organizationName: string,
    organizationLogo?: string | null,
  ): void {
    localStorage.setItem("currentOrganizationId", organizationId);
    localStorage.setItem("currentOrganizationName", organizationName);
    localStorage.removeItem(WORKSPACE_SCOPE_KEY);
    const user = this.getCurrentUser();
    if (user?.id) {
      localStorage.setItem(
        preferredOrgStorageKey(String(user.id)),
        organizationId,
      );
    }
    if (organizationLogo === null) {
      localStorage.removeItem("currentOrganizationLogo");
    } else if (organizationLogo !== undefined) {
      if (organizationLogo) {
        localStorage.setItem("currentOrganizationLogo", organizationLogo);
      } else {
        localStorage.removeItem("currentOrganizationLogo");
      }
    }
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}

export default new AuthService();
