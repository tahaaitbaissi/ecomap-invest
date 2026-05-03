# Étape 1 : Build
FROM node:20-alpine AS builder
WORKDIR /app
# Next.js server proxies /api → Spring (same-origin in browser = no CORS issues).
ARG BACKEND_PROXY_URL=http://backend:8080
ENV BACKEND_PROXY_URL=${BACKEND_PROXY_URL}
# Leave empty so the browser calls /api/... on port 3000 (proxied). Override only for special setups.
ARG NEXT_PUBLIC_API_URL=
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm test
RUN npm run build

# Étape 2 : Production
FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
EXPOSE 3000
ENV PORT 3000
CMD ["node", "server.js"]