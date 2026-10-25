# Multi-stage Dockerfile for Team-11 Insurance Workflow Frontend SPA
# Stage 1: Build React SPA bundle with Node 20
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies using package-lock.json
COPY package*.json ./
RUN npm ci

# Copy source code and build production distribution
COPY . .
RUN npm run build

# Stage 2: Serve static files with lightweight Alpine Nginx
FROM nginx:1.27-alpine

LABEL maintainer="Team 11 - Software Engineering"
LABEL description="Insurance Workflow Frontend Web Application"

# Copy custom Nginx SPA configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Expose internal frontend port
EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
