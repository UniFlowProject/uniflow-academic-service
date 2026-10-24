# Uniflow Academic Service

Microservicio de datos académicos. Modelo: `USER (sub de Cognito) -> PERIOD -> COURSE` (las tareas se agregarán sobre `COURSE`).

- Java 21, Spring Boot 3.5.7, PostgreSQL, Flyway.
- Arquitectura hexagonal: ver [HEXAGONA_GUIDE.md](HEXAGONA_GUIDE.md).
- Autenticación: Cognito autentica en API Gateway; el servicio actúa como resource server y valida el JWT contra `COGNITO_ISSUER_URI`. El id del usuario es el claim `sub` (`authentication.getName()`). No existe tabla de usuarios.

## Variables de entorno

| Variable | Requerida | Default | Descripción |
|---|---|---|---|
| `DB_HOST` | no | `localhost` | Host de PostgreSQL |
| `DB_PORT` | no | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | no | `uniflow_db` | Nombre de la base de datos |
| `DB_USER` | no | `postgres` | Usuario |
| `DB_PASSWORD` | no | `password` | Contraseña |
| `COGNITO_ISSUER_URI` | **sí** | - | Issuer del user pool, p. ej. `https://cognito-idp.<region>.amazonaws.com/<pool-id>` |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:3000` | Orígenes permitidos, separados por coma |
| `SERVER_PORT` | no | `8080` (`3001` en la imagen Docker) | Puerto HTTP |
| `SERVER_SERVLET_CONTEXT_PATH` | no | `/` | Context path (el healthcheck de Docker lo respeta) |
| `LOGGING_LEVEL_ROOT` / `LOGGING_LEVEL_COM_UNIFLOW` | no | `INFO` | Niveles de log |
| `LOGGING_LEVEL_ORG_HIBERNATE_SQL` | no | `OFF` | Poner `DEBUG` para ver el SQL |
| `SPRINGDOC_SWAGGER_UI_ENABLED` / `SPRINGDOC_API_DOCS_ENABLED` | no | `true` | Desactivar en producción si no se necesita |

Las variables también se pueden definir en un archivo `.env` local (spring-dotenv).

## Ejecutar

```bash
export COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example
./mvnw spring-boot:run
```

Flyway aplica `db/migration/V1__baseline.sql` (esquema `academic`) al arrancar; Hibernate solo valida (`ddl-auto: validate`).

## Endpoints

Públicos: `/health`, `/swagger-ui/**`, `/v3/api-docs/**`. El resto requiere `Authorization: Bearer <jwt>`.

- `/periods` (CRUD, `/periods/current`, `/periods/stats`, `PATCH /periods/{periodId}/activate`, `GET /periods/{periodId}/courses`)
- `/courses` (CRUD, `POST /courses/validate-code`), `GET /stats/courses`

Formato de error: `{ "error": { "code", "message", "details": [ { "field", "message" } ] } }`.
Formato de listas paginadas: `{ "items": [...], "pageInfo": { "total", "page", "limit", "totalPages", "hasNext", "hasPrev" } }`.

## Tests

```bash
./mvnw test
```

Los tests de integración usan Testcontainers (PostgreSQL 16) y requieren Docker; sin Docker se omiten (`disabledWithoutDocker`). El JWT se simula con `spring-security-test` (`jwt()`), sin proveedor de identidad.

## Despliegue

La imagen se construye con el `Dockerfile` y se publica en ECR con `.github/workflows/deploy-ecr.yml`. La infraestructura está en `terraform/`.
