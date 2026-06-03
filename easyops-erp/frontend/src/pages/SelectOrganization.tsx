import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Button,
  Container,
  FormControl,
  FormControlLabel,
  Paper,
  Radio,
  RadioGroup,
  Typography,
} from "@mui/material";
import { Business, Public } from "@mui/icons-material";
import { useAuth } from "@contexts/AuthContext";
import authService from "@services/authService";
import logo from "../assets/images/aurora-hms.png";
import appConfig from "@config";
import { getDefaultHomePathFromStoredPermissions } from "@utils/defaultHomePath";

const GLOBAL_VALUE = "__global__";

const SelectOrganization: React.FC = () => {
  const navigate = useNavigate();
  const { setCurrentOrganization, setGlobalWorkspace, reloadRbacState } =
    useAuth();
  const [value, setValue] = useState<string>("");
  const [error, setError] = useState("");

  const [pending] = useState(() =>
    authService.getPendingOrganizationSelection(),
  );

  useEffect(() => {
    if (!pending) {
      navigate(getDefaultHomePathFromStoredPermissions(), { replace: true });
    }
  }, [navigate, pending]);

  useEffect(() => {
    if (!pending) return;
    if (pending.organizations.length === 1) {
      const org = pending.organizations[0];
      setCurrentOrganization(org.id, org.name, org.logo ?? null, {
        themeMode: org.themeMode ?? undefined,
        themePrimaryColor: org.themePrimaryColor ?? undefined,
        themeSecondaryColor: org.themeSecondaryColor ?? undefined,
        themeAccentColor: org.themeAccentColor ?? undefined,
        themeSidebarColor: org.themeSidebarColor ?? undefined,
        themeSidebarTextColor: org.themeSidebarTextColor ?? undefined,
      });
      authService.clearPendingOrganizationSelection();
      void reloadRbacState().then(() => {
        navigate(getDefaultHomePathFromStoredPermissions(), { replace: true });
      });
      return;
    }
    if (pending.organizations.length === 0 && pending.hasGlobalOption) {
      setValue(GLOBAL_VALUE);
    }
  }, [navigate, pending, reloadRbacState, setCurrentOrganization]);

  if (!pending) {
    return null;
  }

  const handleContinue = async () => {
    setError("");
    if (!value) {
      setError("Select where you want to work.");
      return;
    }
    try {
      if (value === GLOBAL_VALUE) {
        setGlobalWorkspace();
      } else {
        const org = pending.organizations.find((o) => o.id === value);
        if (!org) {
          setError("Invalid selection.");
          return;
        }
        setCurrentOrganization(org.id, org.name, org.logo ?? null, {
          themeMode: org.themeMode ?? undefined,
          themePrimaryColor: org.themePrimaryColor ?? undefined,
          themeSecondaryColor: org.themeSecondaryColor ?? undefined,
          themeAccentColor: org.themeAccentColor ?? undefined,
          themeSidebarColor: org.themeSidebarColor ?? undefined,
          themeSidebarTextColor: org.themeSidebarTextColor ?? undefined,
        });
      }
      authService.clearPendingOrganizationSelection();
      await reloadRbacState();
      navigate(getDefaultHomePathFromStoredPermissions(), { replace: true });
    } catch (e) {
      console.error(e);
      setError("Could not apply your selection. Try again.");
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background:
          "linear-gradient(135deg, rgba(123, 42, 144, 0.10) 0%, #f9fafb 40%, rgba(5, 167, 156, 0.12) 100%)",
      }}
    >
      <Container maxWidth="sm">
        <Paper
          elevation={24}
          sx={{
            p: 4,
            display: "flex",
            flexDirection: "column",
            alignItems: "stretch",
            borderRadius: 3,
            background: "#ffffff",
            border: "1px solid rgba(148, 163, 184, 0.3)",
            boxShadow: "0 18px 45px rgba(15,23,42,0.08)",
          }}
        >
          <Box sx={{ mb: 2, textAlign: "center" }}>
            <Box
              component="img"
              src={logo}
              alt={appConfig.appName}
              sx={{ width: 120, height: "auto" }}
            />
          </Box>
          <Typography
            component="h1"
            variant="h5"
            fontWeight="bold"
            gutterBottom
            textAlign="center"
          >
            Where are you working?
          </Typography>
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mb: 3, textAlign: "center" }}
          >
            Your roles span more than one context. Choose an organization or
            global access for this session.
          </Typography>

          <FormControl component="fieldset" sx={{ width: "100%" }}>
            <RadioGroup
              value={value}
              onChange={(e) => setValue(e.target.value)}
            >
              {pending.organizations.map((org) => (
                <FormControlLabel
                  key={org.id}
                  value={org.id}
                  control={<Radio />}
                  label={
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Business fontSize="small" color="action" />
                      <span>
                        {org.name}
                        {org.code ? (
                          <Typography
                            component="span"
                            variant="caption"
                            color="text.secondary"
                            sx={{ ml: 1 }}
                          >
                            ({org.code})
                          </Typography>
                        ) : null}
                      </span>
                    </Box>
                  }
                />
              ))}
              {pending.hasGlobalOption && (
                <FormControlLabel
                  value={GLOBAL_VALUE}
                  control={<Radio />}
                  label={
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Public fontSize="small" color="action" />
                      <span>All organizations (global roles)</span>
                    </Box>
                  }
                />
              )}
            </RadioGroup>
          </FormControl>

          {error && (
            <Typography color="error" variant="body2" sx={{ mt: 2 }}>
              {error}
            </Typography>
          )}

          <Button
            variant="contained"
            fullWidth
            size="large"
            sx={{ mt: 3, py: 1.5 }}
            onClick={handleContinue}
            disabled={!value}
          >
            Continue
          </Button>
        </Paper>
      </Container>
    </Box>
  );
};

export default SelectOrganization;
