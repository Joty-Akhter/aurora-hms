# Multi-stage production build
# Usage:
#   docker build --build-arg BUILD_MODE=production -t aurora-frontend .
#   docker build --build-arg BUILD_MODE=staging    -t aurora-frontend:staging .
#   docker build --build-arg BUILD_MODE=uat        -t aurora-frontend:uat .

ARG BUILD_MODE=production

# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
ARG BUILD_MODE
WORKDIR /app

COPY package*.json ./
RUN npm ci --prefer-offline

COPY . .
RUN npm run build:${BUILD_MODE}

# ── Stage 2: serve ──────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runner

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
