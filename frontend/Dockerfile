# ---------- build ----------
FROM node:24-alpine AS build
WORKDIR /build

# npm ci needs only the manifests, so dependency installs stay cached across
# source-only changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.27-alpine

# The entrypoint renders templates through envsubst before nginx starts.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /build/dist /usr/share/nginx/html

# Defaults match docker-compose.yml, so the local stack needs no extra
# configuration. Railway overrides both: PORT with the assigned port and
# BACKEND_URL with the backend's private address.
ENV PORT=80 \
    BACKEND_URL=http://backend:8080
# Substitute ONLY these two names, so nginx variables such as $uri and $host
# are not blanked out by envsubst.
ENV NGINX_ENVSUBST_FILTER="^(PORT|BACKEND_URL)$"

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- "http://localhost:${PORT}/health" >/dev/null 2>&1 || exit 1
