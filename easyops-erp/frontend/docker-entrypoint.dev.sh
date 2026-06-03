#!/bin/sh
set -e
cd /app
# Bind mount replaces /app with the host tree, but node_modules stays on the anonymous volume.
# When package.json / lockfile gains new deps, refresh so Vite can resolve them (e.g. html2pdf.js).
npm install
exec "$@"
