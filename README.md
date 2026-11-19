# Plataforma de ventas multi-tienda (SaaS)

[![CI](https://github.com/giticex/Proyecto-herramientas-de-desarrollo/actions/workflows/ci.yml/badge.svg)](https://github.com/giticex/Proyecto-herramientas-de-desarrollo/actions/workflows/ci.yml)

Proyecto grupal del curso **Herramientas de Desarrollo** (Universidad Tecnológica del Perú). Es una plataforma donde cualquier negocio puede registrarse, crear su propia tienda y vender en línea, con catálogo, carrito, checkout, boletas y reportes en PDF.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot, Maven |
| Base de datos | MySQL 8 |
| Frontend | React + Vite (carpeta `frontend-react`) |
| Contenedores | Docker y Docker Compose |
| Integración continua | GitHub Actions |
| Gestión de tareas | Jira (tablero SCRUM) y GitHub Issues |

## Estructura del repositorio

```
.github/            Workflow de CI, plantillas de issues y de pull requests
database/           Scripts SQL (esquema y migraciones)
frontend-react/     Frontend actual (React + Vite)
frontend/           Primera versión del frontend (HTML, CSS y JS)
spring-backend/     API REST en Spring Boot
test/TDD/           Pruebas TDD iniciales
laboratorios/       Informes de los laboratorios del curso
docker-compose.yml  MySQL y backend en contenedores
```

## Cómo ejecutarlo en local

### Backend y base de datos con Docker

```bash
docker compose up --build
```

- API: http://localhost:8080
- MySQL (desde tu máquina): puerto `3307`, usuario `root`. La contraseña por defecto es `root` y se cambia con la variable `DB_PASSWORD`.

El esquema inicial se carga desde `database/script_mysql.sql`.

### Frontend

```bash
cd frontend-react
npm ci
npm run dev
```

Se abre en http://localhost:5173.

## Flujo de trabajo con Git

- `main`: versión estable.
- `develop`: integración del trabajo del equipo.
- `feature/SCRUM-n-descripcion`: una rama por tarea, creada desde `develop`.
- `release/vX.Y.Z`: preparación de versiones.

Cada cambio entra por **pull request** hacia `develop`, enlazado a su issue. Los detalles están en [CONTRIBUTING.md](CONTRIBUTING.md).

## Seguimiento de tareas

- Tablero de Jira: https://utp-team-factura.atlassian.net/jira/software/projects/SCRUM/boards/1
- Issues y pull requests: en este repositorio.

## Laboratorios

Los informes están en [laboratorios/](laboratorios/README.md).

## Equipo

| Integrante | Usuario de GitHub |
|---|---|
| Kiara Yuriko Padilla Riveros | KiaraPadilla |
| Anelhi Zharick Marcos Arias | AnelhiMarcosArias |
| Ghelytza Belu Arrieta Berrocal | giticex |
| Adrian Manuel Salcedo Vargas | SummaryPuppet |
