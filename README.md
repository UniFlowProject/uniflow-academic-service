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
