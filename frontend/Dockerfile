FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:stable-alpine
RUN addgroup -S futbol && adduser -S futbol -G futbol && \
    rm -rf /usr/share/nginx/html/* && \
    chown -R futbol:futbol /usr/share/nginx/html && \
    chown -R futbol:futbol /var/cache/nginx && \
    chown -R futbol:futbol /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown futbol:futbol /var/run/nginx.pid
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
USER futbol
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1
