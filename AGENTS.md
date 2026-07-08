# AGENTS.md

## Propósito

Este documento contiene las reglas permanentes para cualquier IA o herramienta automatizada que trabaje en el repositorio Parkio.

Antes de modificar código, la IA debe inspeccionar el estado actual del proyecto y diferenciar claramente entre:

- Funcionalidad implementada.
- Código incompleto.
- Arquitectura documentada como objetivo.
- Recomendaciones todavía no implementadas.

No se debe presentar como existente ninguna funcionalidad que aparezca solamente en `docs/`.

## Propósito del Proyecto

Parkio es un backend en desarrollo para administrar:

- Usuarios.
- Roles.
- Estacionamientos.
- Asignaciones de usuarios a estacionamientos.
- Cajones pertenecientes a un estacionamiento.
- Estado y tipo de los cajones.

El proyecto contiene actualmente el modelo persistente, DTOs, repositorios, contratos de servicio, migraciones y documentación de arquitectura. Los módulos Rol, Estacionamiento, Cajón y Usuario cuentan además con mapper, servicio transaccional, controlador REST y pruebas unitarias. El módulo Auth implementa login y emisión de JWT.

La API REST está implementada para Auth, Rol, Estacionamiento, Cajón y Usuario. Usuario permite asignar y retirar roles y estacionamientos. La autenticación JWT está implementada. La autorización granular por roles ya inició en Rol, Usuario, Estacionamiento y Cajón: `/api/roles` requiere rol `ADMIN`; `/api/usuarios` distingue entre operaciones administrativas de `ADMIN` y operaciones propias de `USER` u `OPERADOR`; `/api/estacionamientos` permite consulta a `ADMIN`, `OPERADOR` y `USER`, y escritura solo a `ADMIN`; `/api/cajones` permite consulta a `ADMIN`, `OPERADOR` y `USER`, cambios de estado a `ADMIN` y `OPERADOR`, y escritura administrativa solo a `ADMIN`.

## Estado Actual

Antes de realizar cambios, considerar lo siguiente:

- `RolController`, `EstacionamientoController`, `CajonController` y `UsuarioController` exponen los recursos `/api/roles`, `/api/estacionamientos`, `/api/cajones` y `/api/usuarios`.
- Existe Spring Security HTTP con OAuth2 Resource Server para proteger endpoints mediante JWT.
- Existen `AuthController`, `AuthService`, `AuthServiceImpl`, `JwtService`, `JwtProperties` y `SecurityConfig`.
- No existe `JwtFilter` propio; la validación del token se delega a Spring Security OAuth2 Resource Server.
- La autorización granular por roles está implementada inicialmente en `RolController` mediante `@PreAuthorize("hasRole('ADMIN')")`.
- `UsuarioController` utiliza `@PreAuthorize` para permitir operaciones administrativas a `ADMIN` y operaciones propias a `USER` u `OPERADOR`.
- `EstacionamientoController` utiliza `@PreAuthorize` para permitir consultas a `ADMIN`, `OPERADOR` y `USER`, y modificaciones solo a `ADMIN`.
- `CajonController` utiliza `@PreAuthorize` para permitir consultas a `ADMIN`, `OPERADOR` y `USER`; cambios de estado a `ADMIN` y `OPERADOR`; y creación, actualización o eliminación solo a `ADMIN`.
- `UsuarioSecurity` compara el `usuarioId` de la ruta con el claim `usuarioId` del JWT.
- Los roles del claim `roles` del JWT se convierten a authorities de Spring Security con prefijo `ROLE_`.
- `RolMapper`, `EstacionamientoMapper`, `CajonMapper` y `UsuarioMapper` están implementados.
- El manejo global de excepciones está implementado mediante `GlobalExceptionHandler` y `ApiError`.
- `RolRequest`, `EstacionamientoRequest`, `CajonRequest`, `CajonEstadoRequest`, `UsuarioCreateRequest`, `UsuarioUpdateRequest`, `UsuarioPasswordRequest`, `UsuarioRolRequest` y `UsuarioEstacionamientoRequest` tienen validaciones Jakarta Validation.
- `RolServiceImpl`, `EstacionamientoServiceImpl`, `CajonServiceImpl` y `UsuarioServiceImpl` están registrados como beans y usan transacciones.
- `UsuarioServiceImpl` valida correos duplicados y genera hashes BCrypt mediante `PasswordEncoder`.
- Existen pruebas unitarias para mapper, servicio y controlador de Rol, Estacionamiento, Cajón y Usuario, pruebas de Auth/JWT/seguridad, además de la prueba de carga del contexto.
- Usuario permite asignar y retirar roles y estacionamientos mediante `usuario_rol` y `usuario_estacionamiento`. `UsuarioResponse` representa estas relaciones mediante nombres de roles e identificadores de estacionamientos. La creación pública de usuarios asigna automáticamente el rol base `USER`. Creación, actualización general y cambio de contraseña utilizan DTOs y operaciones separadas.
- La documentación describe parcialmente una arquitectura futura.
- Todos los repositorios utilizan `Long` como identificador, en concordancia con `BaseEntity`.

Una IA no debe ocultar estas limitaciones ni asumir que ya fueron corregidas.

## Arquitectura General

El proyecto utiliza una organización modular por dominio:

```text
com.kasaca.parkio
├── audit
├── cajon
├── estacionamiento
├── rol
├── shared
└── usuario
```

Cada módulo existente puede contener:

```text
modulo/
├── controller/
├── dto/
├── entity/
├── mapper/
├── repository/
└── service/
```

La arquitectura objetivo documentada es:

```text
Controller → Service → Repository → PostgreSQL
```

Las responsabilidades deben permanecer separadas:

- Controller: protocolo HTTP, validación de entrada y códigos de respuesta.
- Service: reglas de negocio y coordinación de operaciones.
- Repository: acceso a datos.
- Entity: representación persistente.
- DTO: contrato de entrada o salida.
- Mapper: conversión entre DTOs y entidades.

No se deben saltar capas sin una justificación explícita.

## Tecnologías Utilizadas

- Java 21.
- Spring Boot 3.5.15.
- Spring Web.
- Spring Data JPA.
- Hibernate.
- PostgreSQL.
- Flyway.
- Jakarta Validation.
- Lombok.
- Maven.
- Maven Wrapper 3.9.16.
- JUnit 5 y Spring Boot Test.
- PlantUML para documentación técnica.

Existen dependencias de Spring Security, Spring Security OAuth2 Resource Server, Spring Security Test y `spring-security-crypto`. BCrypt se usa para contraseñas y OAuth2 Resource Server valida tokens JWT.

## Estructura de Paquetes

El paquete base obligatorio es:

```text
com.kasaca.parkio
```

El código nuevo debe ubicarse dentro del módulo de dominio correspondiente.

Ejemplos existentes:

```text
com.kasaca.parkio.usuario.entity
com.kasaca.parkio.usuario.dto
com.kasaca.parkio.usuario.repository
com.kasaca.parkio.usuario.service
```

Los componentes compartidos deben colocarse en `shared` únicamente cuando sean realmente reutilizables por varios módulos.

No se debe crear un paquete genérico para código que pertenece claramente a un dominio.

Los paquetes `auth` y `security` existen actualmente. El paquete `config` contiene `PasswordEncoderConfig`; los paquetes `controller` y `mapper` existen dentro de Rol, Estacionamiento, Cajón y Usuario, y las excepciones compartidas se encuentran en `shared.exception`. El paquete `common` aparece como propuesta, pero no existe actualmente. Las capacidades pendientes solo deben crearse cuando una tarea autorizada requiera implementarlas.

## Convenciones de Nomenclatura

### Java

- Clases, interfaces y records: `PascalCase`.
- Métodos, atributos y variables: `camelCase`.
- Constantes: `UPPER_SNAKE_CASE`.
- Paquetes: minúsculas.
- Entidades: nombre singular en español.
- Repositorios: `<Entidad>Repository`.
- Servicios: `<Entidad>Service`.
- Implementaciones: `<Entidad>ServiceImpl`.
- DTOs de entrada: `<Entidad>Request`.
- DTOs de salida: `<Entidad>Response`.

### Base de datos

- Tablas y columnas en español.
- Nombres en `snake_case`.
- Tablas principales en singular: `usuario`, `rol`, `estacionamiento`, `cajon`.
- Tablas intermedias con los nombres de las entidades relacionadas: `usuario_rol`, `usuario_estacionamiento`.
- Claves foráneas con nombres descriptivos iniciados por `fk_`.
- Restricciones únicas con nombres descriptivos iniciados por `uk_`.

### Métodos

Los servicios actuales no siguen una nomenclatura completamente uniforme: existen nombres como `getAllUsers`, `getRoles`, `getEstacionamientos` y `getCajones`.

No se deben renombrar contratos públicos de manera incidental. Cualquier normalización debe realizarse como una refactorización explícita y actualizar todas las referencias y pruebas.

Para código nuevo, se debe respetar el vocabulario del módulo y evitar mezclar idiomas dentro de una misma API.

## Convenciones para Entidades JPA

Las entidades persistentes existentes:

- Extienden `BaseEntity`.
- Utilizan `@Entity`.
- Declaran explícitamente `@Table`.
- Usan Lombok para getters, setters, constructores y builders.
- Declaran nombres y restricciones de columnas mediante `@Column`.
- Modelan relaciones con anotaciones JPA.
- Utilizan identificadores `Long`.

`BaseEntity` proporciona:

```text
id
activo
fechaCreacion
fechaActualizacion
```

Reglas obligatorias:

- Las entidades nuevas deben extender `BaseEntity` cuando necesiten el mismo ciclo de vida y auditoría.
- El tipo de identificador debe ser `Long`.
- Las columnas deben mantener coherencia con las migraciones Flyway.
- Las longitudes, nulabilidad, precisión y unicidad deben coincidir entre JPA y PostgreSQL.
- Las relaciones bidireccionales deben definir claramente el lado propietario.
- Se debe evitar cargar relaciones innecesariamente.
- No se deben agregar `CascadeType` u `orphanRemoval` sin analizar sus consecuencias.
- No se deben exponer entidades directamente como contratos HTTP.
- No se debe almacenar una contraseña en texto plano. `Usuario.passwordHash` representa un hash.
- No se debe incluir `passwordHash` en DTOs de respuesta, logs o mensajes de error.

El proyecto utiliza auditoría JPA mediante:

```java
@EnableJpaAuditing
```

y:

```java
@EntityListeners(AuditingEntityListener.class)
```

No se debe duplicar manualmente la administración de fechas salvo que exista una razón técnica comprobada.

### Relaciones actuales

- `Usuario` y `Rol`: muchos a muchos.
- `Usuario` y `Estacionamiento`: muchos a muchos.
- `Estacionamiento` y `Cajon`: uno a muchos.
- `Cajon` y `Estacionamiento`: muchos a uno.

`tipo` y `estado` de `Cajon` utilizan `TipoCajon` y `EstadoCajon`, persistidos mediante `EnumType.STRING`. El estado inicial es `LIBRE` y se actualiza mediante una operación específica con `CajonEstadoRequest`.

## Convenciones para DTOs

Los DTOs actuales están implementados como records de Java:

```java
public record EntidadRequest(...) {
}
```

```java
public record EntidadResponse(...) {
}
```

Reglas obligatorias:

- Usar records para DTOs nuevos mientras se mantenga la convención actual.
- Separar DTOs de entrada y salida.
- No devolver entidades JPA como respuesta de un endpoint.
- No incluir información sensible en respuestas.
- Mantener los DTOs dentro del paquete `dto` del módulo.
- Los DTOs de respuesta pueden incluir identificadores y datos de auditoría cuando el contrato lo requiera.
- Las relaciones deben representarse mediante identificadores o estructuras explícitas, evitando serializar grafos JPA completos.
- Los cambios en un DTO documentado deben reflejarse en `docs/api/parkio-api-v1.md`.

`RolRequest`, `EstacionamientoRequest`, `CajonRequest`, `CajonEstadoRequest`, `UsuarioCreateRequest`, `UsuarioUpdateRequest`, `UsuarioPasswordRequest`, `UsuarioRolRequest` y `UsuarioEstacionamientoRequest` utilizan Jakarta Validation.

## Convenciones para Repositories

Los repositorios:

- Deben ser interfaces.
- Deben extender `JpaRepository`.
- Deben ubicarse en el paquete `repository` del módulo.
- Deben utilizar la entidad y el tipo real de su identificador.

Patrón esperado:

```java
public interface EntidadRepository extends JpaRepository<Entidad, Long> {
}
```

Todos los repositorios existentes utilizan `Long`, en concordancia con el identificador heredado por las entidades desde `BaseEntity`.

El código nuevo también debe utilizar `Long` y mantener esta consistencia en todas sus referencias.

Las consultas derivadas deben usar nombres que representen exactamente el criterio aplicado. `UsuarioRepository` comprueba duplicados mediante `existsByEmail` y `existsByEmailAndIdNot`, y busca usuarios para autenticación mediante `findByEmail`.

## Convenciones para Services

Los servicios actuales se dividen en:

- Una interfaz `<Entidad>Service`.
- Una clase `<Entidad>ServiceImpl`.

Reglas obligatorias:

- La interfaz debe declarar el contrato.
- La implementación debe contener la lógica de negocio.
- Los controladores no deben acceder directamente a repositorios.
- Los servicios deben validar la existencia de entidades relacionadas.
- Las operaciones que modifiquen varias entidades deben definir límites transaccionales adecuados.
- No se deben devolver valores ficticios para aparentar una implementación.
- No se deben mantener `UnsupportedOperationException` en operaciones que se declaren como completadas.
- Las dependencias de las implementaciones de servicio deben declararse como campos `final` e inyectarse por constructor mediante Lombok `@RequiredArgsConstructor`.
- No se deben escribir constructores manuales en las implementaciones de servicio cuando `@RequiredArgsConstructor` pueda generarlos.
- No se debe usar inyección mediante campos.
- Una implementación que deba ser administrada por Spring debe registrarse explícitamente como bean, normalmente con `@Service`.

`RolServiceImpl`, `EstacionamientoServiceImpl`, `CajonServiceImpl` y `UsuarioServiceImpl` son las implementaciones de referencia actuales: utilizan `@Service`, transacciones, dependencias `final` y Lombok `@RequiredArgsConstructor`.

No se deben modificar las firmas existentes sin evaluar el impacto en documentación, pruebas y futuros controladores.

## Convenciones para Controllers

Actualmente existen `RolController`, `EstacionamientoController`, `CajonController` y `UsuarioController`.

La documentación propone una API con base:

```text
/api
```

y recursos como:

```text
/api/auth
/api/roles
/api/usuarios
/api/estacionamientos
/api/cajones
```

Cuando una tarea solicite implementar controladores:

- Usar `@RestController`.
- Mantener los controladores dentro del módulo correspondiente.
- Recibir y devolver DTOs.
- Aplicar validación a las solicitudes.
- Delegar reglas de negocio al servicio.
- Utilizar códigos HTTP coherentes con el resultado.
- No acceder directamente a repositorios.
- No devolver entidades JPA.
- No incluir lógica de persistencia.
- No implementar endpoints que no estén solicitados o documentados.
- Verificar primero que el contrato documentado coincida con el modelo vigente.

Los códigos HTTP documentados son:

- `200` para operaciones exitosas.
- `201` para creación de recursos.
- `400` para datos inválidos.
- `401` para falta de autenticación.
- `403` para falta de permisos.
- `404` para recursos inexistentes.
- `409` para conflictos.
- `500` para errores internos.

Estos códigos forman parte del contrato común. El módulo Rol ya implementa respuestas `200`, `201`, `204`, `400`, `403`, `404`, `409` y `500` según corresponda; los demás endpoints continúan como objetivo.

## Manejo de Excepciones

Existe un mecanismo global basado en `GlobalExceptionHandler` y el record `ApiError`.

Mapeo HTTP implementado:

- `ResourceNotFoundException`: `404 Not Found`.
- `ConflictException`: `409 Conflict`.
- `AccessDeniedException` y `AuthorizationDeniedException`: `403 Forbidden`.
- `MethodArgumentNotValidException`: `400 Bad Request`.
- `HttpMessageNotReadableException`: `400 Bad Request`.
- `DataIntegrityViolationException`: `409 Conflict`.
- Excepciones no controladas: `500 Internal Server Error`.

Cuando sea requerido:

- Crear excepciones específicas para situaciones de negocio.
- Evitar lanzar excepciones genéricas sin contexto.
- Traducir errores de negocio a respuestas HTTP en una capa centralizada.
- No revelar trazas, consultas SQL, credenciales ni detalles internos.
- Diferenciar entre recurso inexistente, conflicto, validación y error inesperado.
- Mantener respuestas de error consistentes.

El formato estándar actual incluye fecha y hora, estado HTTP, descripción, mensaje seguro, ruta y errores de validación por campo.

## Validaciones

El proyecto incluye `spring-boot-starter-validation`. Los DTOs de entrada de Rol, Estacionamiento, Cajón y Usuario tienen restricciones declarativas acordes con sus contratos actuales.

Cuando se implemente validación:

- Aplicar restricciones a DTOs de entrada, no como sustituto de restricciones de base de datos.
- Usar `@Valid` en los puntos de entrada HTTP.
- Mantener alineadas las restricciones entre DTO, entidad y migración.
- Validar reglas de negocio en los servicios.
- No confiar únicamente en validaciones del cliente.
- No aceptar valores nulos para columnas declaradas `NOT NULL`.
- Validar formato de correo y campos obligatorios del usuario.
- Respetar las longitudes declaradas en las migraciones.
- Validar que las entidades relacionadas existan antes de persistir relaciones.

Las reglas exactas que no estén en las entidades, migraciones o documentación deben confirmarse antes de implementarse.

## Flyway y Migraciones

Flyway está habilitado en `application.yaml`.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration
```

Migraciones existentes:

```text
V1__create_rol.sql
V2__create_usuario.sql
V3__create_usuario_rol.sql
V4__create_estacionamiento.sql
V5__create_usuario_estacionamiento.sql
V6__create_cajon.sql
V7__insert_roles_base.sql
```

Reglas obligatorias:

- No modificar una migración aplicada sin autorización explícita.
- Crear una nueva migración para cualquier cambio de esquema.
- Mantener el formato `V{número}__{descripción}.sql`.
- Usar el siguiente número disponible.
- Mantener nombres SQL en español y `snake_case`.
- Declarar claves primarias, foráneas, índices y restricciones de forma explícita.
- Mantener sincronizadas las migraciones con las entidades JPA.
- Verificar compatibilidad con PostgreSQL.
- No depender de Hibernate para crear o actualizar el esquema.

La configuración actual utiliza:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Por tanto, Flyway controla el esquema y Hibernate solamente lo valida.

Existe una migración de datos iniciales para los roles base `ADMIN`, `OPERADOR` y `USER`. La migración utiliza `ON CONFLICT (nombre) DO NOTHING`, por lo que no falla si alguno de esos roles ya existe.

No se debe asumir la existencia de usuarios, estacionamientos u otros registros predeterminados.

## Seguridad y Autenticación

La autenticación JWT aparece en:

- `docs/api/parkio-api-v1.md`.
- `docs/architecture/parkio-jwt-flow.puml`.
- `docs/sequence/parkio-login-sequence.puml`.
- `docs/uml/parkio-use-cases.puml`.

Estado actual:

- Existe `spring-security-crypto` para BCrypt.
- Existe `spring-boot-starter-security` para seguridad HTTP.
- Existe `spring-boot-starter-oauth2-resource-server` para validar JWT.
- Existe `SecurityConfig`.
- Existe `JwtService`.
- Existe `AuthController`.
- Existe `AuthService` y `AuthServiceImpl`.
- Existe un bean `PasswordEncoder` basado en BCrypt para almacenar hashes de contraseñas.
- Existe endpoint de login en `POST /api/auth/login`.
- No existe `JwtFilter` propio; Spring Security valida el token mediante OAuth2 Resource Server.
- Los endpoints distintos al login y `POST /api/usuarios` requieren JWT válido. `POST /api/usuarios` queda público para permitir registro inicial y asigna automáticamente el rol base `USER`.
- Existe autorización granular inicial por roles: `RolController` requiere `ADMIN`, `UsuarioController` protege operaciones con `ADMIN`, `USER` y `OPERADOR`, `EstacionamientoController` permite lectura a `ADMIN`/`OPERADOR`/`USER` y escritura a `ADMIN`, y `CajonController` permite lectura a `ADMIN`/`OPERADOR`/`USER`, cambio de estado a `ADMIN`/`OPERADOR` y escritura administrativa a `ADMIN`.
- `SecurityConfig` habilita `@EnableMethodSecurity`.
- `SecurityConfig` convierte el claim `roles` del JWT en authorities `ROLE_*`.

Reglas obligatorias:

- No declarar autorización por roles como completa mientras solo existan reglas explícitas para una parte de los módulos.
- No implementar criptografía propia.
- No guardar ni registrar contraseñas en texto plano.
- No exponer `passwordHash`.
- No incluir secretos JWT en el repositorio.
- Externalizar claves y credenciales para entornos reales.
- Mantener credenciales de base de datos, secretos JWT y valores sensibles mediante variables de entorno, no como valores fijos obligatorios en `application.yaml`.
- Mantener perfiles Spring separados para configuración por ambiente: `dev`, `test` y `prod`.
- No presentar la autenticación JWT como autorización completa por roles.
- Verificar autorización por rol en operaciones que la documentación restrinja.
- Tratar la implementación de JWT y Spring Security como un cambio explícito de alcance.

## Documentación

La documentación técnica se encuentra en `docs/`.

Incluye:

- Contrato implementado de Auth, Rol, Estacionamiento, Cajón y Usuario.
- Arquitectura por capas.
- Estructura objetivo de paquetes.
- Flujo JWT.
- Diagrama entidad-relación.
- Modelo de dominio.
- Casos de uso.
- Diagramas de secuencia.
- Casos de uso del MVP.

Reglas obligatorias:

- Mantener el `README.md` alineado con el estado real.
- Actualizar la documentación API cuando cambien DTOs, endpoints o códigos HTTP.
- Actualizar diagramas cuando cambien relaciones o componentes.
- Marcar claramente lo que sea propuesta, roadmap o funcionalidad futura.
- No describir como implementado algo que solo exista en PlantUML o Markdown.
- No eliminar documentación existente sin autorización.
- Conservar PlantUML como formato fuente de los diagramas actuales.

## Buenas Prácticas Obligatorias

Toda IA que modifique este repositorio debe:

- Leer primero el código relacionado y su documentación.
- Limitar los cambios al alcance solicitado.
- Preservar cambios existentes del usuario.
- Mantener compatibilidad con Java 21 y Spring Boot 3.5.15.
- Utilizar `Long` para identificadores JPA.
- Mantener separadas las responsabilidades por capa.
- Usar inyección por constructor y Lombok `@RequiredArgsConstructor` en las implementaciones de servicio con dependencias `final`.
- Evitar exponer entidades JPA.
- Proteger datos sensibles.
- Mantener entidades y migraciones sincronizadas.
- Crear pruebas para nueva lógica cuando el alcance lo permita.
- Ejecutar las verificaciones relevantes después de modificar código.
- Informar si una prueba requiere PostgreSQL u otra condición externa.
- Informar claramente cualquier limitación o funcionalidad pendiente.
- Mantener la documentación consistente con el código.
- Usar nuevas migraciones para cambios de esquema.
- Revisar el impacto de relaciones bidireccionales y carga diferida.
- Mantener cambios pequeños, cohesionados y fáciles de revisar.

## Prácticas Prohibidas

No está permitido:

- Inventar funcionalidades, endpoints, reglas o credenciales.
- Presentar la arquitectura documentada como implementada.
- Modificar migraciones históricas sin autorización explícita.
- Usar `ddl-auto: create`, `create-drop` o `update` como sustituto de Flyway.
- Guardar contraseñas en texto plano.
- Exponer `passwordHash`.
- Introducir secretos nuevos en el repositorio.
- Acceder a repositorios directamente desde controladores.
- Devolver entidades JPA desde endpoints.
- Colocar lógica de negocio en controladores.
- Ignorar inconsistencias entre tipos de identificador.
- Usar inyección de dependencias mediante campos.
- Agregar cascadas JPA sin evaluar sus efectos.
- Capturar excepciones para ocultarlas silenciosamente.
- Devolver datos ficticios como implementación final.
- Marcar una tarea como completada si permanecen operaciones no implementadas dentro de su alcance.
- Hacer refactorizaciones masivas no solicitadas.
- Renombrar contratos públicos de forma incidental.
- Modificar archivos ajenos al alcance.
- Eliminar documentación o pruebas para hacer pasar una compilación.
- Suponer que la base de datos contiene datos iniciales.

## Reglas para Refactorización

Antes de refactorizar:

1. Identificar todos los usos del componente.
2. Revisar entidades, DTOs, repositorios, servicios, migraciones y documentación relacionada.
3. Determinar si el cambio altera contratos públicos o persistencia.
4. Mantener el comportamiento existente salvo que el objetivo sea cambiarlo.
5. Evitar combinar refactorización estructural con funcionalidad no relacionada.

Durante la refactorización:

- Mantener cambios atómicos.
- Preservar nombres de tablas y columnas salvo que exista una migración.
- Actualizar tipos de manera consistente en todas las capas.
- No cambiar relaciones JPA sin revisar el esquema.
- No renombrar endpoints o campos documentados silenciosamente.
- Agregar o actualizar pruebas.

Después de la refactorización:

- Compilar el proyecto.
- Ejecutar las pruebas aplicables.
- Verificar la validación JPA contra Flyway cuando PostgreSQL esté disponible.
- Revisar que la documentación siga siendo correcta.
- Reportar cualquier verificación que no haya podido ejecutarse.

## Reglas para Generación de Código Nuevo

Antes de crear código:

- Confirmar que la funcionalidad fue solicitada.
- Buscar componentes equivalentes en el repositorio.
- Revisar la documentación correspondiente.
- Determinar si la documentación representa el estado actual o una propuesta.

Para un módulo nuevo, seguir cuando aplique:

```text
modulo/
├── controller/
├── dto/
├── entity/
├── mapper/
├── repository/
└── service/
```

Solo se deben crear las capas realmente necesarias para la tarea.

El código nuevo debe:

- Usar el paquete base `com.kasaca.parkio`.
- Seguir la organización por dominio.
- Utilizar DTOs como records.
- Utilizar `Long` como identificador.
- Usar repositorios basados en `JpaRepository`.
- Separar interfaz e implementación de servicio si se conserva el patrón actual.
- Declarar las dependencias del servicio como campos `final` e inyectarlas mediante Lombok `@RequiredArgsConstructor`.
- Incluir validaciones coherentes.
- Incluir manejo explícito de errores.
- No filtrar información sensible.
- Incluir una nueva migración cuando cambie el esquema.
- Incorporar pruebas relevantes.
- Actualizar la documentación afectada.

Si una decisión no puede deducirse del código o de `docs/`, la IA debe indicarlo explícitamente y solicitar confirmación cuando la elección tenga impacto funcional o arquitectónico significativo.

## Verificación Mínima

Después de un cambio en código Java, ejecutar cuando el entorno lo permita:

```powershell
.\mvnw.cmd test
```

En Linux o macOS:

```bash
./mvnw test
```

Debe considerarse que la prueba de contexto utiliza la configuración PostgreSQL local. Si la base de datos no está disponible, la IA debe reportarlo y no afirmar que las pruebas pasaron.

Para cambios de persistencia también se debe verificar:

- Coherencia entre entidad y migración.
- Tipo de identificador.
- Nulabilidad.
- Longitud y precisión.
- Restricciones únicas.
- Claves foráneas.
- Relaciones JPA.

## Principio Final

El código fuente es la referencia principal del estado implementado.

Las migraciones son la referencia principal del esquema de base de datos.

La documentación describe tanto elementos reales como objetivos futuros. Ante cualquier discrepancia, la IA debe señalarla explícitamente y no completar los vacíos mediante suposiciones.
