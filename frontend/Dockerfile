# syntax=docker/dockerfile:1
# -----------------------------------------------------------------------------
# Service: frontend (React / Node 20 + Nginx)
#
# React/Vite storefront served by Nginx.
#
# Build context = ./frontend
#   docker build -t ecommerce/frontend ./frontend
# -----------------------------------------------------------------------------

# ------------------------- Build stage ---------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

# 1) Install dependencies first (cached unless package-lock.json changes).
COPY package*.json ./
RUN npm install

# 2) Copy source and produce the static bundle.
COPY . .
RUN npm run build

# ------------------------- Runtime stage -------------------------------------
# nginx-unprivileged runs as a non-root user (nginx) by default.
FROM nginxinc/nginx-unprivileged:1.27-alpine

# Optional: drop a small nginx config that serves the SPA with history fallback.
# COPY infra/nginx/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

USER nginx
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1
