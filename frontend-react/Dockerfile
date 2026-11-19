# Etapa 1: compila el bundle estático con Vite.
# VITE_API_URL se incrusta en el JS ya compilado en este paso: es la URL
# que usa el NAVEGADOR del visitante para llamar al backend, no una
# dirección interna del contenedor (por eso el valor por defecto es
# localhost:8080 y no el nombre del servicio "app").
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Etapa 2: sirve los archivos estáticos con nginx.
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
