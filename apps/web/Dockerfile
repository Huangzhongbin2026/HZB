FROM node:22-alpine AS builder

WORKDIR /app
COPY package.json tsconfig.json vite.config.ts index.html env.d.ts ./
COPY src ./src
RUN corepack enable && pnpm install && pnpm build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
