import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from "react";
import { createTheme, Theme } from "@mui/material/styles";
import authService, { OrgTheme, DEFAULT_THEME } from "@services/authService";

interface ThemeContextType {
  orgTheme: OrgTheme;
  muiTheme: Theme;
  applyTheme: (theme: OrgTheme) => void;
  resetTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

function buildMuiTheme(orgTheme: OrgTheme): Theme {
  return createTheme({
    palette: {
      mode: orgTheme.mode,
      primary: { main: orgTheme.primaryColor },
      secondary: { main: orgTheme.secondaryColor },
      background: {
        default: orgTheme.mode === "dark" ? "#0f172a" : "#f3f4f6",
        paper: orgTheme.mode === "dark" ? "#1e293b" : "#ffffff",
      },
      text: {
        primary: orgTheme.mode === "dark" ? "#f1f5f9" : "#0f172a",
        // Slightly darker than slate-600 for better contrast on colorful backgrounds (e.g. gradients)
        secondary: orgTheme.mode === "dark" ? "#94a3b8" : "#334155",
      },
    },
    typography: {
      fontFamily:
        'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: "none",
            borderRadius: 10,
            fontWeight: 500,
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            borderRadius: 16,
            boxShadow:
              "0 10px 25px rgba(15, 23, 42, 0.06), 0 4px 6px rgba(15, 23, 42, 0.04)",
          },
        },
      },
    },
  });
}

export const ThemeProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [orgTheme, setOrgTheme] = useState<OrgTheme>(() =>
    authService.getCurrentOrganizationTheme(),
  );
  const [muiTheme, setMuiTheme] = useState<Theme>(() =>
    buildMuiTheme(authService.getCurrentOrganizationTheme()),
  );

  const applyTheme = useCallback((theme: OrgTheme) => {
    setOrgTheme(theme);
    setMuiTheme(buildMuiTheme(theme));
    localStorage.setItem("currentOrganizationTheme", JSON.stringify(theme));
  }, []);

  const resetTheme = useCallback(() => {
    applyTheme(DEFAULT_THEME);
    localStorage.removeItem("currentOrganizationTheme");
  }, [applyTheme]);

  // Re-read theme from storage when localStorage changes (e.g. org switch)
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === "currentOrganizationTheme") {
        const theme = authService.getCurrentOrganizationTheme();
        setOrgTheme(theme);
        setMuiTheme(buildMuiTheme(theme));
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  return (
    <ThemeContext.Provider
      value={{ orgTheme, muiTheme, applyTheme, resetTheme }}
    >
      {children}
    </ThemeContext.Provider>
  );
};

export const useOrgTheme = (): ThemeContextType => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useOrgTheme must be used within ThemeProvider");
  return ctx;
};

export default ThemeContext;
