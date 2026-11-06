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

`docker-compose.dev.yml` levanta el servicio completo en local sobre [Floci](https://floci.io/aws/), un emulador local de AWS (MIT). Floci reemplaza a los servicios de AWS que usa este servicio: Cognito (emisor OIDC/JWKS), RDS (un PostgreSQL real que Floci arranca en un contenedor) y, con el perfil `reminders`, SQS + EventBridge Scheduler + Lambda + SNS. Solo para desarrollo.

Requisitos: Docker con Compose v2 (BuildKit) y `curl`. La imagen de Floci está fijada en `floci/floci:2.3.0`.

| Servicio | Qué es | Puerto en el host (variable) |
|---|---|---|
| `floci` | Emulador de AWS (`floci/floci:2.3.0`); monta `/var/run/docker.sock` para arrancar el contenedor de RDS (y los de Lambda) | `4566` (`FLOCI_PORT`), `7001-7010` RDS proxy |
| `floci-init` | Tarea de un solo uso e idempotente (`docker/floci/init.sh`, imagen `amazon/aws-cli`): crea el user pool de Cognito, el app client, los usuarios `user-a` y `user-b` y la instancia RDS PostgreSQL 16 | - |
| `app` | Este servicio, construido con el `Dockerfile` | `3001` (`APP_PORT`) |
| `token` | Utilidad (perfil `tools`) que imprime un access token de Cognito | - |
| `floci-init-reminders` | Perfil `reminders`: cola SQS, grupo y rol de Scheduler, Lambda `process_expiration` (ver más abajo) | - |

Recursos que crea `floci-init` (valores fijos, no hace falta ningún archivo de entorno):

| Recurso | Valor |
|---|---|
| User pool | `us-east-1_UniflowDev` (como `terraform/auth.tf`: login por email, política de contraseñas de 10+ caracteres) |
| App client | `uniflow-spa` (público, sin secret; además de SRP y refresh habilita `USER_PASSWORD_AUTH`, solo para el helper de tokens) |
| Usuarios | `user-a@uniflow.local` (`UserA-Dev-Passw0rd`) y `user-b@uniflow.local` (`UserB-Dev-Passw0rd`); se pueden cambiar con `USER_A_PASSWORD` / `USER_B_PASSWORD` |
| RDS | instancia `uniflow-db`, PostgreSQL 16, base `uniflow_db`, usuario `postgres` / contraseña `password` (`DB_NAME`, `DB_USER`, `DB_PASSWORD`); Flyway crea el esquema `academic` al arrancar la app |

Los puertos se cambian con variables, p. ej. `APP_PORT=3002 docker compose -f docker-compose.dev.yml up -d --build`. No cambies `FLOCI_PORT` si la app corre en el host: el emisor usa el puerto `4566` (ver abajo).

```bash
# Levantar todo (la primera vez construye la imagen y descarga Floci, el PostgreSQL de RDS y aws-cli)
docker compose -f docker-compose.dev.yml up -d --build
docker compose -f docker-compose.dev.yml logs -f app
docker compose -f docker-compose.dev.yml logs floci-init   # bootstrap de Cognito y RDS

# Obtener access tokens de Cognito; el sub es el UUID del usuario (user-a y user-b tienen sub distinto)
TOKEN_A=$(scripts/dev-token.sh user-a)
TOKEN_B=$(scripts/dev-token.sh user-b)

# Parar y borrar contenedores, red y datos (también los contenedores de RDS/Lambda que arrancó Floci)
docker compose -f docker-compose.dev.yml --profile reminders --profile tools down -v
```

Ejemplo del flujo periodo -> curso -> tarea (los ids van en las respuestas; los `userId` son UUID):

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

Base de datos: la app usa la instancia RDS de Floci (`floci:<puerto>` dentro de la red de Compose, `localhost:<puerto>` desde el host). El puerto normal es `7001`, pero Floci no reutiliza el puerto de un `CreateDBInstance` fallido (p. ej. por disco lleno), asi que puede ser `7002`, `7003`... `floci-init` lee el puerto real, lo imprime y lo escribe en `/out/db.env`, que el entrypoint de la app aplica como `DB_PORT`. Para verlo: `docker compose -f docker-compose.dev.yml logs floci-init | tail -1`. Para inspeccionarla: `docker run --rm -it --network uniflow-academic-dev -e PGPASSWORD=password postgres:16-alpine psql -h floci -p <puerto> -U postgres -d uniflow_db`.

Estado persistente de Floci: el volumen anonimo montado en `/app/data` del contenedor `floci` conserva pool, client, usuarios e instancia RDS entre `up`/`down` sin `-v`. Los cambios en `docker/floci/init.sh` se aplican igualmente porque el script actualiza el client existente (flujos OAuth, scopes, callback/logout URLs). Si quieres empezar de cero (tambien libera el puerto 7001): `docker compose -f docker-compose.dev.yml down -v` (borra los volumenes de ESTE proyecto, incluido el de estado de Floci), y despues elimina los volumenes huerfanos `floci-aws-rds-db-*` que dejo Floci (`docker volume ls | grep floci-aws-rds-db`; borra solo los que `docker ps -a` no use).

Login del frontend (`UniFlow-Frontend`, Hosted UI + Authorization Code/PKCE): `floci-init` crea el dominio Hosted UI y deja el client `uniflow-spa` con `code`, scopes `openid email profile`, proveedor `COGNITO`, callback `${FRONTEND_ORIGIN}/auth/callback` y logout `${FRONTEND_ORIGIN}` (por defecto `http://localhost:3000`, el puerto de `pnpm dev`). Para otro origen: `FRONTEND_ORIGIN=http://localhost:5173 docker compose -f docker-compose.dev.yml up -d` (tambien ajusta CORS de Floci y de la app). El frontend usa `VITE_COGNITO_DOMAIN=http://localhost.floci.io:4566/cognito-idp` y `VITE_COGNITO_CLIENT_ID=uniflow-spa`.

### El emisor (issuer) y por qué funciona desde el host y desde Compose

La app exige `iss == COGNITO_ISSUER_URI` y descarga el discovery/JWKS de esa misma URL. Floci construye `iss` como `FLOCI_BASE_URL/<poolId>`, así que hay que lograr que **una sola URL** sea válida para la app dentro de Compose y para quien llame desde el host. El compose usa:

- `FLOCI_BASE_URL=http://localhost.floci.io:4566` en `floci`. `localhost.floci.io` es un nombre DNS público que resuelve a `127.0.0.1`/`::1`, así que en el host llega al puerto publicado de Floci.
- Un alias de red `localhost.floci.io` en el servicio `floci`: dentro de la red de Compose el mismo nombre resuelve al contenedor de Floci, por lo que la app, `floci-init` y `token` usan exactamente la misma URL.
- El user pool y el client tienen id fijo (etiquetas `floci:override-id` y `floci:override-cognito-client-id=use-name`), por eso `COGNITO_ISSUER_URI=http://localhost.floci.io:4566/us-east-1_UniflowDev` y `COGNITO_ALLOWED_CLIENT_IDS=uniflow-spa` van directamente en el compose.

Consecuencia: el `iss` de los tokens es siempre `http://localhost.floci.io:4566/us-east-1_UniflowDev`, pidas el token desde donde lo pidas. Si cambias `FLOCI_PORT`, el puerto del host deja de coincidir con el `4566` del emisor: no lo cambies si vas a ejecutar la app en el host. Se necesita resolución DNS pública para `localhost.floci.io` en el host; sin ella añade `127.0.0.1 localhost.floci.io` a `/etc/hosts`.

Los tokens traen `token_use=access`, `client_id=uniflow-spa` y `sub` = UUID del usuario de Cognito, igual que Cognito real.

### App en el host (`./mvnw spring-boot:run`) contra el mismo stack

```bash
docker compose -f docker-compose.dev.yml up -d floci floci-init   # sin el contenedor de la app
docker compose -f docker-compose.dev.yml logs floci-init          # espera a "done"

export DB_PORT=7001 DB_USER=postgres DB_PASSWORD=password   # use the port printed by floci-init if it is not 7001
export COGNITO_ISSUER_URI=http://localhost.floci.io:4566/us-east-1_UniflowDev
export COGNITO_ALLOWED_CLIENT_IDS=uniflow-spa
./mvnw spring-boot:run

TOKEN=$(scripts/dev-token.sh user-a)    # mismo iss que usa la app del host
curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/periods
```

La app en el host escucha en `8080` por defecto y se conecta a la base por `localhost:<puerto RDS>` (normalmente `7001`). No ejecutes a la vez la app del host y la del compose si no quieres compartir la misma base.

### Recordatorios con Lambda, SQS y EventBridge Scheduler (perfil `reminders`)

```bash
docker compose -f docker-compose.dev.yml --profile reminders up -d --build
```

`floci-init-reminders` (`docker/floci/reminders.sh`) replica `terraform/messaging.tf`: colas `uniflow-expirations`, `uniflow-in-app-notifications` y `uniflow-email-notifications` (cada una con su DLQ), topic SNS `uniflow-notifications` suscrito a las dos últimas, grupo de Scheduler `uniflow`, rol `uniflow-scheduler`, y la Lambda real `process_expiration` (Node.js 22, copia de `lambdas/process_expiration/index.mjs` del repo raíz en `docker/floci/lambdas/`) conectada a `uniflow-expirations` con *report batch item failures*. Escribe `/out/reminders.env`, que el entrypoint de desarrollo de `app` carga para usar `REMINDER_SCHEDULER=eventbridge`, `REMINDER_GROUP_NAME`, `REMINDER_QUEUE_ARN`, `REMINDER_ROLE_ARN` y `REMINDER_LEAD_TIME_HOURS=0` (el SDK de AWS lee `AWS_ENDPOINT_URL` y las credenciales `test`/`test`). Si ya tenías el stack levantado sin el perfil, añade `--force-recreate app` para que la app tome la configuración.

Con antelación 0, un recordatorio se dispara a la hora de `dueDate`. Prueba: crea una tarea con `dueDate` unos 2 minutos en el futuro, comprueba que existe el schedule y, tras la hora, que el mensaje llegó a las colas de notificaciones y a los logs de la Lambda:

```bash
AWS="docker compose -f docker-compose.dev.yml run --rm -q --no-deps -e AWS_ENDPOINT_URL=http://localhost.floci.io:4566 --entrypoint aws floci-init-reminders"
$AWS scheduler list-schedules --group-name uniflow
$AWS sqs receive-message --queue-url http://localhost.floci.io:4566/000000000000/uniflow-in-app-notifications
$AWS logs get-log-events --log-group-name /aws/lambda/uniflow-process-expiration \
  --log-stream-name "$($AWS logs describe-log-streams --log-group-name /aws/lambda/uniflow-process-expiration --query 'logStreams[0].logStreamName' --output text)"
```

Floci despacha los schedules cada ~10 s y la primera invocación de la Lambda arranca un contenedor, así que el mensaje llega unos 20-30 s después de la hora.

### Precauciones

- `floci` monta `/var/run/docker.sock`: equivale a acceso root sobre el host. Úsalo solo en tu máquina de desarrollo.
- Todas las credenciales y contraseñas son valores de desarrollo (`test`/`test`, `password`, contraseñas de los usuarios). Nunca apuntes `COGNITO_ISSUER_URI` de un entorno real a Floci.
- Floci es un emulador: puede diferir de AWS real (p. ej. Cognito añade `USER_PASSWORD_AUTH` solo aquí; el access token trae el scope `aws.cognito.signin.user.admin`).
- El estado vive en memoria: si reinicias solo el contenedor `floci`, se pierden usuarios, pool y base de datos. Ejecuta `docker compose -f docker-compose.dev.yml up -d` para que `floci-init` lo recree y reinicia `app` (`restart app`); los usuarios nuevos tienen un `sub` distinto.
- `docker compose ... down -v` elimina también los contenedores de RDS y Lambda que arrancó Floci. Si por algún motivo quedan contenedores `floci-aws-*` (tras `down -v` no quedó ninguno en las pruebas), bórralos con `docker rm -f $(docker ps -aq --filter name=floci-aws-)`.

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

La imagen se construye con el `Dockerfile` y `.github/workflows/deploy-ecr.yml` la publica en ECR autenticándose por OIDC con GitHub Actions (sin llaves estáticas); luego fuerza un nuevo despliegue del servicio ECS, que usa la etiqueta `latest`. El servicio corre en ECS Fargate detrás de un ALB interno, y API Gateway lo expone al exterior mediante un VPC link. El servicio, el repositorio ECR y el rol de CI se definen en `terraform/`; la red, el clúster ECS, la base de datos y Cognito pertenecen al stack del repositorio central (https://github.com/UniFlowProject/uniflow), que los publica como parámetros SSM.
