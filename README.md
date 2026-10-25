# Uniflow Academic Service

Microservicio de datos académicos. Modelo: `USER (sub de Cognito) -> PERIOD -> COURSE -> TASK` (las tareas cuelgan de un curso y de su periodo).

- Java 21, Spring Boot 3.5.7, PostgreSQL, Flyway.
- Arquitectura hexagonal: ver [HEXAGONA_GUIDE.md](HEXAGONA_GUIDE.md).
- Autenticación: Cognito autentica en API Gateway; el servicio actúa como resource server y valida el JWT contra `COGNITO_ISSUER_URI` (solo access tokens de los clientes en `COGNITO_ALLOWED_CLIENT_IDS`). Las respuestas 401/403 usan el mismo cuerpo de error JSON (`UNAUTHORIZED` / `FORBIDDEN`). El id del usuario es el claim `sub` (`authentication.getName()`). No existe tabla de usuarios.

## Variables de entorno

| Variable | Requerida | Default | Descripción |
|---|---|---|---|
| `DB_HOST` | no | `localhost` | Host de PostgreSQL |
| `DB_PORT` | no | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | no | `uniflow_db` | Nombre de la base de datos |
| `DB_USER` | no | `postgres` | Usuario |
| `DB_PASSWORD` | no | `password` | Contraseña |
| `COGNITO_ISSUER_URI` | **sí** | - | Issuer del user pool, p. ej. `https://cognito-idp.<region>.amazonaws.com/<pool-id>` |
| `COGNITO_ALLOWED_CLIENT_IDS` | **sí** | - | App client ids permitidos (coma). Solo se aceptan access tokens (`token_use=access`) cuyo `client_id` esté en la lista; el arranque falla si está vacío |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:3000` | Orígenes permitidos, separados por coma |
| `SERVER_PORT` | no | `8080` (`3001` en la imagen Docker) | Puerto HTTP |
| `SERVER_SERVLET_CONTEXT_PATH` | no | `/` | Context path (el healthcheck de Docker lo respeta) |
| `LOGGING_LEVEL_ROOT` / `LOGGING_LEVEL_COM_UNIFLOW` | no | `INFO` | Niveles de log |
| `LOGGING_LEVEL_ORG_HIBERNATE_SQL` | no | `OFF` | Poner `DEBUG` para ver el SQL |
| `REMINDER_SCHEDULER` | no | `noop` | `noop` (sin recordatorios) o `eventbridge` (EventBridge Scheduler -> SQS) |
| `REMINDER_GROUP_NAME` | no | `default` | Grupo de EventBridge Scheduler donde se crean los schedules `task-reminder-{taskId}` |
| `REMINDER_QUEUE_ARN` | con `eventbridge` | - | ARN de la cola SQS destino (la consume la lambda `process_expiration`) |
| `REMINDER_ROLE_ARN` | con `eventbridge` | - | ARN del rol que EventBridge Scheduler asume para enviar a la cola |
| `REMINDER_LEAD_TIME_HOURS` | no | `24` | Horas de antelación del recordatorio respecto a `dueDate` (>= 0) |
| `REMINDER_API_TIMEOUT` | no | `5s` | Tiempo máximo total de una llamada a EventBridge Scheduler (incluye reintentos) |
| `REMINDER_API_ATTEMPT_TIMEOUT` | no | `2s` | Tiempo máximo de cada intento |
| `REMINDER_MAX_RETRIES` | no | `2` | Reintentos tras el primer intento (>= 0) |
| `REMINDER_CONNECT_TIMEOUT` / `REMINDER_SOCKET_TIMEOUT` | no | `1s` / `2s` | Timeouts de conexión y lectura del cliente HTTP |
| `SPRINGDOC_SWAGGER_UI_ENABLED` / `SPRINGDOC_API_DOCS_ENABLED` | no | `false` | Swagger/OpenAPI desactivado por defecto; para verlo en local: `SPRINGDOC_SWAGGER_UI_ENABLED=true SPRINGDOC_API_DOCS_ENABLED=true` |

Las variables también se pueden definir en un archivo `.env` local (spring-dotenv).

### Recordatorios de tareas

Con `REMINDER_SCHEDULER=eventbridge` el servicio crea un schedule de un solo uso por tarea (`at(...)` en UTC, `dueDate` menos `REMINDER_LEAD_TIME_HOURS`) que se borra solo al dispararse. El cliente del SDK de AWS toma región (`AWS_REGION`) y credenciales de la cadena por defecto (rol de la tarea en ECS). Si faltan los ARN, el arranque falla. Los recordatorios son de mejor esfuerzo: se programan/cancelan después del commit de la base de datos y en un pool pequeño y acotado (2-4 hilos, cola de 100), fuera del hilo de la petición y sin retener la conexión a la base de datos. Un fallo se registra a nivel `ERROR` (tarea y clase de la excepción) y no afecta la operación; si el pool está saturado el recordatorio se descarta con un `ERROR` (no hay reintento ni outbox, y no hay métricas porque el servicio no incluye Micrometer). Se reprograman al cambiar el título o la fecha de entrega (el mensaje lleva `taskTitle`) y se cancelan al pasar a `done`/`cancelled` o al borrar la tarea.

Se recomienda un grupo de schedules dedicado (`REMINDER_GROUP_NAME`) en lugar de `default`.

Permisos mínimos del rol de la tarea de ECS:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["scheduler:CreateSchedule", "scheduler:UpdateSchedule", "scheduler:DeleteSchedule"],
      "Resource": "arn:aws:scheduler:<region>:<account>:schedule/<group>/task-reminder-*"
    },
    {
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": "<REMINDER_ROLE_ARN>",
      "Condition": { "StringEquals": { "iam:PassedToService": "scheduler.amazonaws.com" } }
    }
  ]
}
```

El rol que asume EventBridge Scheduler (`REMINDER_ROLE_ARN`, confía en `scheduler.amazonaws.com`) solo necesita `sqs:SendMessage` sobre la cola `REMINDER_QUEUE_ARN`.

## Ejecutar

```bash
export COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example
export COGNITO_ALLOWED_CLIENT_IDS=your-app-client-id
./mvnw spring-boot:run
```

Flyway aplica `db/migration/V1__baseline.sql` y `V2__tasks.sql` (esquema `academic`) al arrancar; Hibernate solo valida (`ddl-auto: validate`).

## Desarrollo local con Docker

`docker-compose.dev.yml` levanta el servicio completo en local: la app, PostgreSQL y un emisor OIDC falso que reemplaza a Cognito. Solo para desarrollo.

Requisitos: Docker con Compose v2 (BuildKit) y `curl`.

| Servicio | Qué es | Puerto en el host (variable) |
|---|---|---|
| `app` | Este servicio, construido con el `Dockerfile` | `3001` (`APP_PORT`) |
| `postgres` | PostgreSQL 16; Flyway crea el esquema `academic` al arrancar | `5434` (`POSTGRES_PORT`) |
| `mock-oauth` | [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server) configurado en `docker/mock-oauth2/config.json` | `8081` (`MOCK_OAUTH_PORT`) |
| `token` | Utilidad (perfil `tools`) que imprime un access token | - |

Los puertos se cambian con variables, p. ej. `APP_PORT=3002 POSTGRES_PORT=5440 docker compose -f docker-compose.dev.yml up -d --build`. Las credenciales de la base de datos (`DB_NAME`, `DB_USER`, `DB_PASSWORD`) también se pueden sobreescribir.

```bash
# Levantar todo (la primera vez construye la imagen)
docker compose -f docker-compose.dev.yml up -d --build
docker compose -f docker-compose.dev.yml logs -f app

# Obtener tokens; el sub es el nombre pasado: user-a, user-b o dev-user (por defecto)
TOKEN_A=$(scripts/dev-token.sh user-a)
TOKEN_B=$(scripts/dev-token.sh user-b)
# Equivalente sin el script:
# TOKEN_A=$(docker compose -f docker-compose.dev.yml run --rm -q -e TOKEN_USER=user-a token)

# Parar y borrar contenedores y datos
docker compose -f docker-compose.dev.yml down -v
```

Ejemplo del flujo periodo -> curso -> tarea (los ids van en las respuestas):

```bash
H="Authorization: Bearer $TOKEN_A"; J="Content-Type: application/json"
curl -s localhost:3001/health
curl -s -H "$H" -H "$J" -d '{"name":"II Semestre","type":"second-semester","year":2026,"startDate":"2026-08-03","endDate":"2026-12-12"}' localhost:3001/periods
curl -s -H "$H" -H "$J" -d '{"name":"Diseño","code":"IC-5402","professor":"M. R.","credits":3,"color":"#3b82f6","periodId":"<periodId>"}' localhost:3001/courses
curl -s -H "$H" -H "$J" -d '{"title":"Ensayo","courseId":"<courseId>","periodId":"<periodId>","dueDate":"2026-11-01T10:00:00Z","priority":"high","type":"essay","estimatedTimeHours":4}' localhost:3001/tasks
curl -s -H "$H" localhost:3001/tasks
curl -s -H "$H" localhost:3001/tasks/stats

# Aislamiento entre usuarios: user-b recibe 404 en el recurso de user-a y listas vacías
curl -s -H "Authorization: Bearer $TOKEN_B" localhost:3001/tasks/<taskId>
curl -s -H "Authorization: Bearer $TOKEN_B" localhost:3001/tasks
```

Swagger UI (habilitado solo en este compose): <http://localhost:3001/swagger-ui/index.html>. La especificación OpenAPI no declara un esquema de seguridad, por lo que Swagger UI no ofrece el botón **Authorize** y sus peticiones "Try it out" responden 401: úsalo para consultar el contrato y ejecuta las llamadas con `curl` y el token. Los tokens duran 1 hora.

Cómo funciona la autenticación local: los tokens traen `token_use=access`, `client_id=local-dev-client` (el valor de `COGNITO_ALLOWED_CLIENT_IDS` en el compose) y `iss=http://mock-oauth:8080/default`. El emisor deriva `iss` del host con el que se pidió el token, por eso los tokens se piden desde dentro de la red de Compose (servicio `token` / `scripts/dev-token.sh`) y no con `localhost:8081`. Un token pedido a `localhost:8081` tiene otro `iss` y la app del compose lo rechaza con 401.

> **Importante:** el emisor falso firma con claves efímeras y entrega tokens a cualquiera sin autenticar. Es solo para desarrollo local; nunca lo uses (ni `COGNITO_ISSUER_URI` apuntando a él) fuera de tu máquina.

Para activar recordatorios reales en el compose cambia `REMINDER_SCHEDULER=eventbridge` y define `REMINDER_QUEUE_ARN`, `REMINDER_ROLE_ARN`, `AWS_REGION` y credenciales de AWS en el servicio `app`.

### Solo Postgres y mock-oauth, con la app en el host

```bash
docker compose -f docker-compose.dev.yml up -d postgres mock-oauth

# En el host el emisor es localhost:8081, así que el issuer y los tokens deben usar ese mismo host
export DB_PORT=5434 DB_USER=postgres DB_PASSWORD=password
export COGNITO_ISSUER_URI=http://localhost:8081/default
export COGNITO_ALLOWED_CLIENT_IDS=local-dev-client
./mvnw spring-boot:run

# Token pedido directamente al puerto publicado (el scope define el sub)
TOKEN=$(curl -s localhost:8081/default/token -d grant_type=client_credentials \
  -d client_id=local-dev-client -d client_secret=dev --data-urlencode scope=user-a \
  | sed -n 's/.*"access_token" *: *"\([^"]*\)".*/\1/p')
curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/periods
```

La app en el host escucha en `8080` por defecto. No mezcles ambos modos: los tokens de `scripts/dev-token.sh` (`iss` con `mock-oauth:8080`) no sirven para la app del host, y viceversa.

## Endpoints

Públicos: `/health` (y `/swagger-ui/**`, `/v3/api-docs/**` solo si springdoc está habilitado). El resto requiere `Authorization: Bearer <jwt>`.

- `/periods` (CRUD, `/periods/current`, `/periods/stats`, `PATCH /periods/{periodId}/activate`, `GET /periods/{periodId}/courses`)
- `/courses` (CRUD, `POST /courses/validate-code`), `GET /stats/courses`. Borrar un curso con tareas responde 409 `COURSE_HAS_TASKS`.
- `/tasks`: `POST /tasks`, `GET /tasks` (filtros `status`, `priority`, `courseId`, `periodId`, `dueDateFrom`, `dueDateTo`, `isOverdue`, `isDueSoon`, `search`; `sortBy` = `dueDate|priority|status|createdAt`, `sortOrder` = `asc|desc`, `page`, `limit` por defecto 20), `GET /tasks/stats`, `GET /tasks/{taskId}`, `PATCH /tasks/{taskId}` (parcial), `PATCH /tasks/{taskId}/status`, `DELETE /tasks/{taskId}`. Una tarea ajena o inexistente responde 404 `TASK_NOT_FOUND`; una escritura concurrente sobre la misma tarea responde 409 `TASK_CONFLICT` (bloqueo optimista con `version`; recargar y reintentar); las tareas `done`/`cancelled` no se pueden modificar ni cambiar de estado (409 `TASK_NOT_MODIFIABLE`), pero sí se pueden borrar (en cualquier estado).

Formato de error: `{ "error": { "code", "message", "details": [ { "field", "message" } ] } }`.
Formato de listas paginadas: `{ "items": [...], "pageInfo": { "total", "page", "limit", "totalPages", "hasNext", "hasPrev" } }`.

### Límites de las peticiones

`title` <= 255 caracteres, `description` <= 5000, `tags`/`groupMembers`/`attachments` <= 50 elementos (cada elemento no vacío; <= 100 caracteres, <= 2048 en `attachments`), `courseId`/`periodId`/`taskId` <= 36, `search` <= 100. Los `attachments` deben ser URLs `http`/`https`. Spring no limita el tamaño del cuerpo JSON: hace falta un límite de cuerpo en el API Gateway / balanceador.

## Tests

```bash
./mvnw test
```

Los tests de integración usan Testcontainers (PostgreSQL 16) y requieren Docker; sin Docker se omiten (`disabledWithoutDocker`). El JWT se simula con `spring-security-test` (`jwt()`), sin proveedor de identidad.

## Despliegue

La imagen se construye con el `Dockerfile` y se publica en ECR con `.github/workflows/deploy-ecr.yml`. La infraestructura está en `terraform/`.
