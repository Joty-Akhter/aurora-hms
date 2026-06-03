import React, { useMemo, useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import {
  Box,
  Container,
  Paper,
  TextField,
  Button,
  Typography,
  Link,
  Alert,
  ToggleButtonGroup,
  ToggleButton,
  InputAdornment,
  IconButton,
} from "@mui/material";
import { Visibility, VisibilityOff, Lock, Person, ArrowBack, CheckCircle } from "@mui/icons-material";
import authService from "@services/authService";
import { useSnackbar } from "notistack";
import logo from "../assets/images/aurora-hms.png";
import appConfig from "@config";
import {
  evaluatePasswordPolicy,
  passwordMeetsPolicy,
  PASSWORD_REQUIREMENTS,
} from "@/utils/passwordPolicy";

type Channel = "EMAIL" | "SMS";

const ForgotPassword: React.FC = () => {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [eligible, setEligible] = useState<boolean | null>(null);
  const [channels, setChannels] = useState<Channel[]>([]);
  const [maskedEmail, setMaskedEmail] = useState<string | null>(null);
  const [maskedPhone, setMaskedPhone] = useState<string | null>(null);
  const [channel, setChannel] = useState<Channel | null>(null);

  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const passwordChecks = useMemo(() => evaluatePasswordPolicy(newPassword), [newPassword]);

  const lookupAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const opts = await authService.getPasswordResetOptions(usernameOrEmail.trim());
      setEligible(opts.eligible);
      setChannels((opts.channels || []) as Channel[]);
      setMaskedEmail(opts.maskedEmail ?? null);
      setMaskedPhone(opts.maskedPhone ?? null);
      if (!opts.eligible) {
        enqueueSnackbar(
          "Password reset is not available for this account (no email or phone on file). Contact an administrator.",
          { variant: "warning" },
        );
        return;
      }
      const first = (opts.channels?.[0] as Channel) || null;
      setChannel(first);
      setStep(2);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
        "Unable to look up account.";
      setError(msg);
      enqueueSnackbar(msg, { variant: "error" });
    } finally {
      setLoading(false);
    }
  };

  const sendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (!channel) {
      setError("Choose email or SMS.");
      return;
    }
    setLoading(true);
    try {
      await authService.resetPassword({
        usernameOrEmail: usernameOrEmail.trim(),
        channel,
      });
      enqueueSnackbar("Verification code sent.", { variant: "success" });
      setStep(3);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
        "Unable to send verification code.";
      setError(msg);
      enqueueSnackbar(msg, { variant: "error" });
    } finally {
      setLoading(false);
    }
  };

  const submitNewPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    if (!passwordMeetsPolicy(newPassword)) {
      setError("Password does not meet all requirements listed below.");
      return;
    }
    setLoading(true);
    try {
      await authService.confirmPasswordReset({
        token: otp.trim(),
        newPassword,
      });
      enqueueSnackbar("Password updated. You can sign in.", { variant: "success" });
      navigate("/login", { replace: true });
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
        "Could not reset password.";
      setError(msg);
      enqueueSnackbar(msg, { variant: "error" });
    } finally {
      setLoading(false);
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
              sx={{ width: 120, height: "auto" }}
            />
          </Box>

          <Typography component="h1" variant="h4" fontWeight="bold" gutterBottom color="text.primary">
            Reset password
          </Typography>

          <Typography variant="body2" color="text.secondary" sx={{ mb: 2, textAlign: "center" }}>
            {step === 1 && "Enter your username or email to see recovery options."}
            {step === 2 && "Choose how you want to receive your verification code."}
            {step === 3 && "Enter the code and your new password."}
          </Typography>

          {error && (
            <Alert severity="error" sx={{ width: "100%", mb: 2 }}>
              {error}
            </Alert>
          )}

          {step === 1 && (
            <Box component="form" onSubmit={lookupAccount} sx={{ width: "100%" }}>
              <TextField
                margin="normal"
                required
                fullWidth
                label="Username or Email"
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
              <Button
                type="submit"
                fullWidth
                variant="contained"
                disabled={loading || !usernameOrEmail.trim()}
                sx={{
                  mt: 2,
                  py: 1.5,
                  background: "linear-gradient(135deg, #7b2a90 0%, #05a79c 100%)",
                }}
              >
                {loading ? "Checking…" : "Continue"}
              </Button>
            </Box>
          )}

          {step === 2 && eligible && (
            <Box component="form" onSubmit={sendOtp} sx={{ width: "100%" }}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                Send code to:
              </Typography>
              <ToggleButtonGroup
                exclusive
                fullWidth
                value={channel}
                onChange={(_, v) => v && setChannel(v)}
                sx={{ mb: 2 }}
              >
                {channels.includes("EMAIL") && (
                  <ToggleButton value="EMAIL">Email {maskedEmail ? `(${maskedEmail})` : ""}</ToggleButton>
                )}
                {channels.includes("SMS") && (
                  <ToggleButton value="SMS">SMS {maskedPhone ? `(${maskedPhone})` : ""}</ToggleButton>
                )}
              </ToggleButtonGroup>
              <Box sx={{ display: "flex", gap: 1 }}>
                <Button
                  startIcon={<ArrowBack />}
                  onClick={() => {
                    setStep(1);
                    setError("");
                    setEligible(null);
                  }}
                >
                  Back
                </Button>
                <Button type="submit" variant="contained" disabled={loading || !channel} fullWidth>
                  {loading ? "Sending…" : "Send code"}
                </Button>
              </Box>
            </Box>
          )}

          {step === 3 && (
            <Box component="form" onSubmit={submitNewPassword} sx={{ width: "100%" }}>
              <TextField
                margin="normal"
                required
                fullWidth
                label="Verification code"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
                inputProps={{ inputMode: "numeric", maxLength: 6 }}
              />
              <TextField
                margin="normal"
                required
                fullWidth
                label="New password"
                type={showPassword ? "text" : "password"}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock color="action" />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword(!showPassword)} edge="end" aria-label="toggle password">
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
              <TextField
                margin="normal"
                required
                fullWidth
                label="Confirm new password"
                type={showPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock color="action" />
                    </InputAdornment>
                  ),
                }}
              />
              <Box sx={{ mt: 1, mb: 1, width: "100%" }}>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
                  Password requirements (same as administrator-created accounts):
                </Typography>
                {PASSWORD_REQUIREMENTS.map(({ key, label }) => (
                  <Box key={key} sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                    <CheckCircle
                      sx={{
                        fontSize: 16,
                        color: passwordChecks[key] ? "success.main" : "action.disabled",
                      }}
                    />
                    <Typography variant="caption">{label}</Typography>
                  </Box>
                ))}
              </Box>
              <Box sx={{ display: "flex", gap: 1, mt: 2 }}>
                <Button
                  startIcon={<ArrowBack />}
                  onClick={() => {
                    setStep(2);
                    setError("");
                  }}
                >
                  Back
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  disabled={
                    loading || otp.trim().length !== 6 || !passwordMeetsPolicy(newPassword)
                  }
                  fullWidth
                >
                  {loading ? "Saving…" : "Set new password"}
                </Button>
              </Box>
            </Box>
          )}

          <Box sx={{ mt: 2 }}>
            <Link component={RouterLink} to="/login" variant="body2" underline="hover">
              Back to sign in
            </Link>
          </Box>
        </Paper>
      </Container>
    </Box>
  );
};

export default ForgotPassword;
