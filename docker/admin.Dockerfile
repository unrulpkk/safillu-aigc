# syntax=docker/dockerfile:1
FROM node:22-alpine AS build
WORKDIR /source
ENV ROLLDOWN_WORKER_THREADS=2 RAYON_NUM_THREADS=2
RUN corepack enable && corepack prepare pnpm@10.6.3 --activate
COPY zaomeng-admin/package.json zaomeng-admin/pnpm-lock.yaml zaomeng-admin/pnpm-workspace.yaml ./
ARG NPM_REGISTRY=https://registry.npmjs.org
RUN --mount=type=cache,target=/pnpm/store \
    node -e "const fs=require('fs'); const p='pnpm-lock.yaml'; fs.writeFileSync(p,fs.readFileSync(p,'utf8').replaceAll('https://registry.npmmirror.com',process.argv[1].replace(/\/$/,'')))" "$NPM_REGISTRY" \
    && pnpm install --frozen-lockfile --registry="$NPM_REGISTRY" --store-dir /pnpm/store --fetch-timeout=60000 --fetch-retries=2
COPY zaomeng-admin/ ./
RUN node --max-old-space-size=2048 ./node_modules/vite/bin/vite.js build --mode prod

FROM nginx:1.28-alpine
COPY --from=build /source/dist-prod/ /usr/share/nginx/html/
COPY zaomeng-backend/deploy/admin-nginx.conf /etc/nginx/conf.d/default.conf
