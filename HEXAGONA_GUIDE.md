# Arquitectura Hexagonal con Spring Boot - Guía Paso a Paso

## Introducción
Esta guía documenta la construcción de una arquitectura hexagonal (puertos y adaptadores) en Spring Boot 3.5.7 con PostgreSQL, Cognito (JWT resource server) y Hexagonal Architecture.

---

## 1. Capa de Dominio

### 1.1 Modelo de Dominio
Crear la clase que representa tu objeto de negocio.

**Annotations:**
- `@Getter` (Lombok)
- `@NoArgsConstructor` (Lombok)
- `@AllArgsConstructor` (Lombok)
- `@Builder(toBuilder = true)` (Lombok) - El `toBuilder = true` es crucial para actualizaciones

**Características:**
- Contiene lógica de negocio pura
- Métodos de validación (`validate()`)
- Métodos de fábrica (`create()`)
- Enums para valores permitidos
- Sin dependencias de Spring o JPA

### 1.2 Excepciones de Dominio
Crear excepciones personalizadas para casos de error del negocio.

**Estructura:**
```
domain/
├── MiObjeto.java
└── exception/
    ├── MiObjetoNotFoundException.java   // extiende NotFoundException (404)
    ├── InvalidMiObjetoException.java    // extiende InvalidDomainException (400)
    └── MiObjetoAlreadyExistsException.java // extiende ConflictException (409)
```

---

## 2. Capa de Puertos (Application)

### 2.1 Puertos de Entrada (Commands & Queries)
Define contratos para casos de uso que modifican (Commands) y consultan (Queries) datos.

**Annotations:**
- Interfaces sin anotaciones de Spring (puertos puros)

**Ejemplo de estructura:**
```
application/ports/in/
├── CreateMiObjetoCommand.java       // Crear
├── UpdateMiObjetoCommand.java       // Actualizar
├── DeleteMiObjetoCommand.java       // Eliminar
├── GetMiObjetoByIdQuery.java        // Obtener por ID
├── GetAllMiObjetosQuery.java        // Listar con paginación
└── GetMiObjetoStatsQuery.java       // Estadísticas
```

**DTOs para solicitudes:**
- `CreateMiObjetoRequest` - Todos los campos requeridos
- `UpdateMiObjetoRequest` - Todos los campos opcionales

### 2.2 Puerto de Salida (Repository)
Define el contrato para persistencia de datos.

**Annotations:**
- Interfaz sin anotaciones de Spring

**Métodos típicos:**
- `save(MiObjeto)` - Guardar nuevo
- `update(MiObjeto)` - Actualizar existente
- `findById(id, userId)` - Obtener por ID con isolamiento de datos
- `findAll(userId, filters)` - Listar con filtros
- `delete(id, userId)` - Eliminar
- Métodos de validación (`existsByIdAndUserId()`, `hasAssociatedResources()`)

**DTOs para repository:**
- `PaginationParams` - Parámetros de paginación seguros
- `MiObjetoFilter` - Filtros opcionales

---

## 3. Capa de Aplicación (Services)

### 3.1 Servicios de Caso de Uso
Implementan los puertos de entrada (Commands y Queries).

**Annotations:**
- `@Service` (Spring)
- `@RequiredArgsConstructor` (Lombok) - Inyección de dependencias
- `@Transactional` (Spring) - Para modificaciones
- `@Transactional(readOnly = true)` - Para consultas
- `@Slf4j` (Lombok) - Logging

**Responsabilidades:**
- Orquestar la lógica de casos de uso
- Validar datos usando el repositorio
- Llamar al dominio para operaciones
- Persistir cambios

**Estructura:**
```
application/services/
├── CreateMiObjetoService.java
├── UpdateMiObjetoService.java
├── DeleteMiObjetoService.java
├── GetMiObjetoByIdService.java
├── GetAllMiObjetosService.java
└── GetMiObjetoStatsService.java
```

---

## 4. Capa de Infraestructura - Persistencia

### 4.1 Entidad JPA
Mapeo de la tabla de base de datos.

**Annotations:**
- `@Entity` (JPA)
- `@Table(name = "...", schema = "...")` (JPA)
- `@Id` (JPA)
- `@Column` (JPA)
- `@CreationTimestamp` (Hibernate)
- `@UpdateTimestamp` (Hibernate)
- `@Getter`, `@Setter` (Lombok)
- `@NoArgsConstructor`, `@AllArgsConstructor` (Lombok)
- `@Builder` (Lombok)

**Características:**
- Espejo exacto del modelo de dominio
- Nombres en snake_case
- Índices para consultas frecuentes
- Restricciones de base de datos

### 4.2 Repositorio JPA Spring Data
Interfaz de queries a base de datos.

**Annotations:**
- `@Repository` (Spring) - Opcional, Spring la detecta automáticamente

**Métodos:**
- Métodos simples por nombre (`findByIdAndUserId()`)
- Métodos complejos con `@Query` (JPQL/SQL)
- Retornan `Optional<>` para opcionalidad segura
- Soportan `Page<>` para paginación

### 4.3 Mapper Entidad-Dominio
Convierte entre JPA Entity y Dominio.

**Annotations:**
- `@Component` (Spring)

**Métodos:**
- `toEntity(domainObject)` - Dominio a JPA
- `toDomain(jpaEntity)` - JPA a Dominio

### 4.4 Adaptador de Persistencia (Repository Adapter)
Implementa el puerto de salida usando JPA.

**Annotations:**
- `@Component` (Spring) o `@Repository`
- `@RequiredArgsConstructor` (Lombok)
- `@Slf4j` (Lombok)

**Responsabilidades:**
- Implementar todas las operaciones del puerto
- Usar el mapper para conversiones
- Manejar la lógica de paginación
- Convertir excepciones JPA a excepciones de dominio

---

## 5. Capa de Infraestructura - Web

### 5.1 DTOs HTTP
Modelos para solicitudes y respuestas REST.

**Annotations para Request DTOs:**
- `@NotBlank`, `@NotNull` (Jakarta Validation)
- `@Size`, `@Min`, `@Max`, `@Pattern` (Jakarta Validation)
- `@JsonFormat(pattern = "yyyy-MM-dd")` (Jackson)
- `@JsonProperty` (Jackson)
- `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` (Lombok)
- `@Schema` (SpringDoc OpenAPI)

**Annotations para Response DTOs:**
- `@JsonProperty` (Jackson)
- `@Schema` (SpringDoc OpenAPI)
- `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` (Lombok)
- **Nota:** NO usar `@JsonFormat` en responses, confunde a Jackson

### 5.2 Mapper HTTP
Convierte entre DTOs HTTP y modelos de aplicación.

**Annotations:**
- `@Component` (Spring)

**Métodos:**
- `toCreateCommandRequest(httpRequest)`
- `toUpdateCommandRequest(httpRequest)`
- `toHttpResponse(domainObject)`
- `toPagedResponse(pagedResult)` - usa `PagedResult.map`

### 5.3 Controlador REST
Expone endpoints HTTP.

**Annotations:**
- `@RestController` (Spring)
- `@RequestMapping("/path")` (Spring)
- `@RequiredArgsConstructor` (Lombok)
- `@Slf4j` (Lombok)
- `@Tag` (SpringDoc OpenAPI)
- `@SecurityRequirement(name = "bearer-jwt")` (SpringDoc OpenAPI)

**Métodos de endpoint:**
- `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` (Spring)
- `@PathVariable`, `@RequestParam`, `@RequestBody` (Spring)
- `@Valid` (Jakarta Validation)
- `@Parameter`, `@Operation`, `@ApiResponse`, `@ApiResponses` (SpringDoc OpenAPI)

**Responsabilidades:**
- Recibir y validar solicitudes HTTP
- Extraer contexto de autenticación
- Mapear a comandos/queries
- Mapear respuestas a DTOs HTTP
- Retornar códigos HTTP apropiados

### 5.4 Manejador Global de Excepciones
Existe UN solo `@RestControllerAdvice` en `shared/infrastructure/web/GlobalExceptionHandler`. Los módulos NO definen handlers propios.

- Las excepciones de dominio extienden la jerarquía de `shared/domain/exception`: `NotFoundException` (404), `InvalidDomainException` (400), `ConflictException` (409). Cada una define su `code` (p. ej. `PERIOD_NOT_FOUND`, `COURSE_CODE_ALREADY_EXISTS`).
- Cuerpo de error único: `{ "error": { "code", "message", "details": [ { "field", "message" } ] } }` (campos nulos omitidos).
- Además maneja validación de beans (`VALIDATION_ERROR`, 400), JSON mal formado (`MALFORMED_REQUEST`, 400), violaciones de unicidad/FK (`CONFLICT`, 409; otros errores de integridad son 500), acceso denegado (`FORBIDDEN`, 403) y el fallback `INTERNAL_ERROR` (500).

---

## 6. Capa de Infraestructura - Seguridad

Cognito autentica en API Gateway y el servicio valida el JWT como OAuth2 resource server. No hay filtros ni validadores propios.

### 6.1 Identidad del usuario
- El id del usuario es el claim `sub` del JWT: `authentication.getName()` en los controllers.
- No existe tabla de usuarios; cada tabla guarda `user_id` y todo acceso a datos se filtra por `user_id` (un recurso ajeno responde 404).

### 6.2 Configuración de Spring Security (`config/SecurityConfig`)
- Stateless, CSRF deshabilitado, `oauth2ResourceServer().jwt()` con `spring.security.oauth2.resourceserver.jwt.issuer-uri=${COGNITO_ISSUER_URI}`.
- `config/JwtDecoderConfig` (perfil distinto de `test`) construye el `JwtDecoder` con `CognitoTokenValidators`: issuer + vigencia, `token_use=access` y `client_id` en `COGNITO_ALLOWED_CLIENT_IDS` (obligatoria; el arranque falla si está vacía). Los tests definen su propio `JwtDecoder` mock.
- 401/403 devuelven el cuerpo de error JSON compartido (`UNAUTHORIZED` / `FORBIDDEN`) mediante `JsonAuthenticationEntryPoint` / `JsonAccessDeniedHandler`, sin detalles del token.
- Públicos: `/health` (y swagger / `/v3/api-docs/**` solo si springdoc está habilitado, desactivado por defecto). Todo lo demás requiere autenticación.
- CORS desde `CORS_ALLOWED_ORIGINS` (lista separada por comas).

---

## 7. Configuración General

### 7.1 Application.yml
Centralizar todas las configuraciones.

**Secciones principales:**
```yaml
spring:
  datasource:          # Conexión a DB
  jpa:                 # Configuración JPA/Hibernate
  flyway:              # Migraciones de base de datos
  jackson:             # Serialización JSON
  security.oauth2.resourceserver.jwt:  # issuer de Cognito

server:               # Puerto y contexto
logging:              # Niveles de log
```

### 7.2 Migraciones Flyway
SQL para crear tablas y esquemas.

**Estructura:**
```
src/main/resources/db/migration/
├── V1__baseline.sql        # esquema academic, periods, courses
└── V2__...sql              # cambios posteriores (nunca editar V1)
```

**Naming:** `V{version}__{description}.sql`

---

## 8. Flujo Completo de una Solicitud

```
1. HTTP Request con Bearer JWT (Cognito)
   ↓
2. Resource server valida el JWT (firma/issuer) -> Authentication (name = sub)
   ↓
3. Controller recibe request (userId = authentication.getName())
   ↓
4. Mapper HTTP convierte a Command/Query
   ↓
5. Service ejecuta el caso de uso
   ↓
6. Repository (puerto) persiste/recupera datos filtrando por user_id
   ↓
7. Mapper Entity <-> Domain
   ↓
8. Controller mapea a DTO HTTP y responde
   ↓
9. GlobalExceptionHandler convierte cualquier error al cuerpo único de error
```

---

## 9. Convenciones globales

- **Identidad**: `userId` = `sub` del JWT; columnas `user_id`.
- **Dominio**: el recurso hijo de `PERIOD` es `COURSE` ; las tareas (`TASK`) cuelgan de `COURSE`. `CourseRepository.hasAssociatedTasks` es el gancho para el dominio de tareas.
- **Errores**: un único cuerpo `{ "error": { "code", "message", "details": [ { "field", "message" } ] } }`; excepciones de dominio extienden `NotFoundException` / `InvalidDomainException` / `ConflictException`. Sin handlers por módulo.
- **Periodo ajeno o inexistente**: crear/listar/validar cursos con un `periodId` que no pertenece al usuario (o no existe) responde 404 `PERIOD_NOT_FOUND`, igual que los endpoints de periodos.
- **Listas**: las listas paginadas responden `{ "items": [...], "pageInfo": { "total", "page", "limit", "totalPages", "hasNext", "hasPrev" } }` (`shared/application/PagedResult<T>`); las listas sin paginar (`GET /courses`, `GET /periods/{id}/courses`) responden `{ "items": [...] }`.
- **Paginación**: `PaginationParams` es inmutable y valida una sola vez (page >= 1, limit 1..100, defaults 1 y 10); valores inválidos responden 400 `VALIDATION_ERROR` (no se corrigen en silencio).
- **Un periodo activo por usuario**: índice único parcial `uk_period_one_active_per_user` (`WHERE is_active`). Al activar, desactivar primero y hacer `flush` antes de activar (ver `ActivatePeriodService` / `PeriodPostgresAdapter.update`).
- **Migraciones**: Flyway, esquema `academic`; `V1__baseline.sql` es el baseline de Postgres. Nuevos cambios en `V2+`.
- **Tests**: integración con Testcontainers (`support/IntegrationTestBase` + `PostgresTestConfig`, `@ServiceConnection`), JWT simulado con `jwt()` de spring-security-test (`sub` = id del usuario). Cada test usa un `userId` aleatorio para aislarse.
- **Logging**: nunca registrar tokens ni cabeceras de autorización; sin `System.out`.

## 10. Piezas compartidas y convención de propiedad

Dónde viven las piezas compartidas (cualquier módulo nuevo, p. ej. tareas, debe reutilizarlas):

- `shared/application`: `PagedResult`, `PaginationParams`.
- `shared/domain/exception`: `DomainException` y la jerarquía `NotFoundException` / `InvalidDomainException` / `ConflictException`.
- `shared/infrastructure/web`: `GlobalExceptionHandler`, `ApiErrorResponse`, `JsonAuthenticationEntryPoint`, `JsonAccessDeniedHandler`.
- `shared/infrastructure/security`: `CognitoTokenValidators`.

Convención de propiedad (aislamiento por usuario):

- Cada repositorio expone `findById(id, userId)` y un `default requireOwned(id, userId)` que lanza la excepción `NotFound` del módulo (`PeriodRepository`, `CourseRepository`). Los services usan `requireOwned`; nunca consultan por id sin `userId`.
- Para validar un recurso padre (p. ej. el curso de una tarea) usar `courseRepository.requireOwned(courseId, userId)` y trabajar con el id canónico devuelto (los ids se normalizan con `trim`). Un recurso ajeno o inexistente siempre responde 404.
- Los borrados son por `id` + `user_id` (`delete(id, userId)` devuelve `boolean`); un módulo no depende de la persistencia de otro: usa puertos (`CourseRepository.existsByPeriodId`, `hasAssociatedTasks`).
