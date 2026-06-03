# Alien-Pharma Setup Guide

## ✅ Configuration Complete

The `.env` file has been created with the alien-pharma preset configuration.

## Current Configuration

**File:** `easyops-erp/frontend/.env`
```bash
VITE_MODULE_PRESET=alien-pharma
```

## Enabled Modules for Alien-Pharma

✅ **Enabled:**
- Dashboard
- Organizations
- Accounting
- Inventory
- HR
- Pharma
- Users
- Roles
- Permissions

❌ **Disabled:**
- Sales
- Purchase
- CRM
- Manufacturing

## Next Steps

### 1. Restart Development Server

The `.env` file has been created, but you need to **restart the development server** for the changes to take effect:

```bash
# Stop the current server (Ctrl+C)

# Start the server again
cd easyops-erp/frontend
npm run dev
```

### 2. Verify Configuration

After restarting, verify that:

1. **Sidebar Menu** - Only shows enabled modules:
   - Dashboard
   - Organizations
   - Accounting
   - Inventory
   - HR
   - Pharma
   - Users
   - Roles
   - Permissions

2. **Disabled Modules** - Should NOT appear:
   - Sales (hidden)
   - Purchase (hidden)
   - CRM (hidden)
   - Manufacturing (hidden)

3. **Routes** - Direct URLs to disabled modules should redirect or show access denied

### 3. Testing

Try accessing these URLs to verify they're disabled:
- `http://localhost:3000/sales/dashboard` - Should not be accessible
- `http://localhost:3000/purchase/dashboard` - Should not be accessible
- `http://localhost:3000/crm/dashboard` - Should not be accessible
- `http://localhost:3000/manufacturing/dashboard` - Should not be accessible

And these should work:
- `http://localhost:3000/inventory/products` - Should be accessible
- `http://localhost:3000/hr/dashboard` - Should be accessible
- `http://localhost:3000/accounting/dashboard` - Should be accessible
- `http://localhost:3000/pharma/territories` - Should be accessible

## Changing Configuration

To change the module configuration, edit the `.env` file:

### Option 1: Use a Different Preset

```bash
# Edit .env file
VITE_MODULE_PRESET=alien-pharma
```

### Option 2: Explicitly List Enabled Modules

```bash
# Edit .env file
VITE_ENABLE_MODULE=inventory,hr,accounting,pharma
```

> Note: `VITE_ENABLED_MODULES` is still supported for compatibility.

### Option 3: Enable All Modules (Default)

Remove or comment out the environment variables:
```bash
# VITE_MODULE_PRESET=alien-pharma
# VITE_ENABLE_MODULE=inventory,hr,accounting,pharma
```

**Remember:** After changing `.env`, you must restart the development server!

## Production Deployment

For production builds, set the environment variable in your deployment platform:

**Docker:**
```dockerfile
ENV VITE_MODULE_PRESET=alien-pharma
```

**CI/CD (GitHub Actions, etc.):**
```yaml
env:
  VITE_MODULE_PRESET: alien-pharma
```

**Note:** The environment variable is embedded at build time, so changes require a rebuild.

## Troubleshooting

### Modules Still Showing

1. **Check `.env` file exists** in `easyops-erp/frontend/` directory
2. **Verify file contents:** `cat .env` should show `VITE_MODULE_PRESET=alien-pharma`
3. **Restart dev server:** Environment variables are loaded at startup
4. **Check browser console** for any errors

### All Modules Still Visible

1. Verify `.env` file is in the correct location: `easyops-erp/frontend/.env`
2. Check for typos in environment variable name: `VITE_MODULE_PRESET` (not `VITE_MODULE_CONFIG`)
3. Ensure no quotes around the value: `VITE_MODULE_PRESET=alien-pharma` (not `VITE_MODULE_PRESET="alien-pharma"`)

### Routes Accessible But Menus Hidden

This shouldn't happen - both menus and routes are filtered. If it does:
1. Clear browser cache
2. Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
3. Check for any route redirects that bypass module checks

## Configuration Files

- **Module Configuration:** `src/config/modules.ts`
- **Environment Variables:** `.env` (created)
- **Menu Filtering:** `src/components/Layout/MainLayout.tsx`
- **Route Filtering:** `src/App.tsx`
- **Documentation:** `MODULE_CONFIGURATION.md`

## Support

For more details, see the [Module Configuration Guide](./MODULE_CONFIGURATION.md).

