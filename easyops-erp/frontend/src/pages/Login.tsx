import React, { useState } from "react";
import { useNavigate, Link as RouterLink } from "react-router-dom";
import {
  Box,
  Container,
  Paper,
  TextField,
  Button,
  Typography,
  Link,
  Alert,
  InputAdornment,
  IconButton,
} from "@mui/material";
import { Visibility, VisibilityOff, Lock, Person } from "@mui/icons-material";
import { useAuth } from "@contexts/AuthContext";
import authService from "@services/authService";
import { useSnackbar } from "notistack";
import logo from "../assets/images/aurora-hms.png";
import appConfig from "@config";
import { getDefaultHomePathFromStoredPermissions } from "@utils/defaultHomePath";

const Login: React.FC = () => {
  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();
  const { login } = useAuth();
  const { enqueueSnackbar } = useSnackbar();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      await login({ usernameOrEmail, password });
      enqueueSnackbar("Login successful!", { variant: "success" });
      if (authService.hasPendingOrganizationSelection()) {
        navigate("/select-organization", { replace: true });
      } else {
        navigate(getDefaultHomePathFromStoredPermissions(), { replace: true });
      }
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.error ||
        "Login failed. Please check your credentials.";
      setError(errorMessage);
      enqueueSnackbar(errorMessage, { variant: "error" });
    } finally {
      setIsLoading(false);
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
            alignItems: "center",
            borderRadius: 3,
            background: "#ffffff",
            border: "1px solid rgba(148, 163, 184, 0.3)",
            boxShadow: "0 18px 45px rgba(15,23,42,0.08)",
          }}
        >
          <Box sx={{ mb: 2 }}>
            <Box
              component="img"
              src={logo}
              alt={appConfig.appName}
              sx={{
                width: 120,
                height: "auto",
              }}
            />
          </Box>

          <Typography
            component="h1"
            variant="h4"
            fontWeight="bold"
            gutterBottom
            color="text.primary"
          >
            {appConfig.appName}
          </Typography>

          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Sign in to your account
          </Typography>

          {error && (
            <Alert severity="error" sx={{ width: "100%", mb: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} sx={{ width: "100%" }}>
            <TextField
              margin="normal"
              required
              fullWidth
              id="usernameOrEmail"
              label="Username or Email"
              name="usernameOrEmail"
              autoComplete="username"
              autoFocus
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Person color="action" />
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type={showPassword ? "text" : "password"}
              id="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Lock color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowPassword(!showPassword)}
                      edge="end"
                      aria-label="toggle password visibility"
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <Box sx={{ mt: 1, mb: 2, textAlign: "right" }}>
              <Link
                component={RouterLink}
                to="/forgot-password"
                variant="body2"
                underline="hover"
              >
                Forgot password?
              </Link>
            </Box>

            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={isLoading}
              sx={{
                mt: 2,
                mb: 2,
                py: 1.5,
                background: "linear-gradient(135deg, #7b2a90 0%, #05a79c 100%)",
                "&:hover": {
                  background:
                    "linear-gradient(135deg, #6b2480 0%, #048f86 100%)",
                },
              }}
            >
              {isLoading ? "Signing in..." : "Sign In"}
            </Button>
          </Box>
        </Paper>

        <Box sx={{ mt: 2, textAlign: "center" }}>
          <Typography variant="body2" color="text.secondary">
            © {appConfig.appYear} {appConfig.appName}. All rights reserved.
          </Typography>
        </Box>
      </Container>
    </Box>
  );
};

export default Login;
