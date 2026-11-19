# Guía de contribución

## Ramas

| Rama | Uso |
|---|---|
| `main` | Versión estable. Solo recibe cambios desde `develop` o `release/*`. |
| `develop` | Integración del trabajo del equipo. |
| `feature/SCRUM-n-descripcion` | Una tarea de Jira. Se crea desde `develop`. |
| `release/vX.Y.Z` | Preparación de una versión. |

Ejemplo: `feature/SCRUM-36-docker-frontend`.

## Commits

Se usa el formato `tipo: descripción corta`, con estos tipos:

- `feat`: nueva funcionalidad.
- `fix`: corrección de un error.
- `docs`: documentación.
- `ci`: cambios en GitHub Actions.
- `refactor`: cambio interno sin alterar el comportamiento.

Ejemplo: `feat: agregar filtro por código SKU en el catálogo`.

## Issues

1. Crea el issue con la plantilla que corresponda (error o funcionalidad).
2. Ponle **etiqueta**, **responsable** y, si aplica, un **hito**.
3. Menciona la tarea de Jira en el título o en el texto (por ejemplo, SCRUM-30).

Etiquetas del proyecto: `bug`, `enhancement`, `documentation`, `ci`, `frontend`, `backend`.

## Pull requests

1. Abre el PR hacia `develop` desde tu rama `feature/*`.
2. Usa el título `SCRUM-n: descripción`.
3. En la descripción escribe `Closes #n` para que el issue se cierre al fusionar.
4. Pide revisión a otro integrante antes de fusionar.
5. Revisa que el workflow de CI no tenga errores.
