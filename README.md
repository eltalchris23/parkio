# Parkio

## Descripción General

Parkio es un proyecto backend en desarrollo para administrar estacionamientos, usuarios, roles y cajones de estacionamiento.

Está construido con Java 21 y Spring Boot, utiliza PostgreSQL como base de datos, Spring Data JPA para persistencia y Flyway para controlar la evolución del esquema.

Actualmente, el proyecto contiene:

- Modelo de entidades JPA.
- DTOs de entrada y salida.
- Repositorios Spring Data JPA.
- Contratos de servicio.
- CRUD REST completo para los módulos Rol, Estacionamiento, Cajón y Usuario.
- Login mediante `/api/auth/login`.
- Seguridad HTTP con Spring Security y JWT.
- Manejo global de excepciones y validación para las operaciones implementadas.
- Pruebas unitarias de mapper, servicio y controlador para Rol, Estacionamiento, Cajón y Usuario.
- Hash seguro de contraseñas de Usuario mediante BCrypt.
- Migraciones iniciales de base de datos.
- Documentación de arquitectura, dominio, API implementada y funcionalidades propuestas.

El proyecto expone APIs REST funcionales para autenticar usuarios en `/api/auth/login` y administrar roles en `/api/roles`, estacionamientos en `/api/estacionamientos`, cajones en `/api/cajones` y usuarios en `/api/usuarios`.

La autenticación JWT ya está implementada. La autorización granular por rol ya inició: actualmente `/api/roles` requiere rol `ADMIN`, varias operaciones de `/api/usuarios` distinguen entre `ADMIN`, `USER` y `OPERADOR`, y `/api/estacionamientos` permite consultas a `ADMIN`, `OPERADOR` y `USER`, dejando la escritura únicamente a `ADMIN`. Cajón todavía no tiene reglas específicas por rol.

## Objetivos del Sistema

Según el modelo actual y la documentación existente, Parkio busca proporcionar una base para:

- Administrar usuarios del sistema.
- Definir y asignar roles a usuarios.
- Registrar estacionamientos con ubicación geográfica.
- Asociar usuarios con estacionamientos.
- Registrar y administrar cajones dentro de cada estacionamiento.
- Consultar el estado de los cajones.
- Mantener información de auditoría básica sobre las entidades.
- Autenticar usuarios mediante correo, contraseña BCrypt y JWT.

La autorización por roles ya forma parte del código ejecutable de forma inicial para el módulo Rol. La autorización granular del resto de módulos continúa como trabajo pendiente.

## Tecnologías Utilizadas

| Tecnología | Versión o uso |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.15 |
| Spring Web | API REST de Rol, Estacionamiento, Cajón y Usuario |
| Spring Data JPA | Persistencia y repositorios |
| Hibernate | Implementación JPA |
| PostgreSQL | Base de datos relacional |
| Flyway | Versionado y migración del esquema |
| Jakarta Validation | Validación declarativa implementada en los DTOs de entrada de Rol, Estacionamiento, Cajón y Usuario |
| Spring Security | Seguridad HTTP, protección de endpoints y soporte OAuth2 Resource Server para JWT |
| Spring Security Crypto | Generación de hashes BCrypt para contraseñas |
| Lombok | Generación de getters, setters, constructores y builders |
| Maven | Gestión de dependencias y construcción |
| Maven Wrapper | Maven 3.9.16 |
| JUnit 5 | Pruebas mediante Spring Boot Test |
| PlantUML | Diagramas en la documentación |

El proyecto incluye Spring Security para proteger endpoints, Spring Security OAuth2 Resource Server para validar JWT y `spring-security-crypto` para BCrypt. La autorización granular por rol ya está implementada en Rol, Usuario y Estacionamiento. Cajón todavía no tiene reglas específicas por rol.

## Arquitectura del Proyecto

El código está organizado mediante una estructura modular por dominio. Cada módulo agrupa sus entidades, DTOs, repositorios y servicios.

La arquitectura prevista sigue una separación por capas:

```text
Controller → Service → Repository → PostgreSQL
```

Estado actual de las capas:

| Capa | Estado |
|---|---|
| Entidades | Implementadas |
| DTOs | Definidos |
| Repositorios | Definidos con Spring Data JPA |
| Servicios | Rol, Estacionamiento, Cajón y Usuario implementados |
| Controladores | `RolController`, `EstacionamientoController`, `CajonController` y `UsuarioController` implementados |
| Mappers | `RolMapper`, `EstacionamientoMapper`, `CajonMapper` y `UsuarioMapper` implementados |
| Seguridad | Autenticación JWT implementada; autorización por rol implementada en `/api/roles`, `/api/usuarios` y `/api/estacionamientos`; reglas por rol pendientes en Cajón |
| Manejo global de errores | Implementado mediante `GlobalExceptionHandler` y `ApiError` |
| Auditoría JPA | Habilitada |
| Migraciones | Implementadas de V1 a V6 |

La clase principal habilita la auditoría mediante `@EnableJpaAuditing`. Las entidades heredan los campos comunes desde `BaseEntity`.

> La autenticación implementada utiliza `AuthController`, `AuthService`, `JwtService` y `SecurityConfig`. No existe un `JwtFilter` propio porque la validación del token se delega al soporte OAuth2 Resource Server de Spring Security.

## Estructura de Carpetas

```text
parkio/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── docs/
│   ├── api/
│   │   └── parkio-api-v1.md
│   ├── architecture/
│   │   ├── parkio-jwt-flow.puml
│   │   ├── parkio-package-structure.puml
│   │   └── spring-boot-architecture.puml
│   ├── erd/
│   │   └── parkio-erd.puml
│   ├── sequence/
│   │   ├── parkio-create-cajon-sequence.puml
│   │   ├── parkio-create-estacionamiento-sequence.puml
│   │   └── parkio-login-sequence.puml
│   ├── uml/
│   │   ├── parkio-domain.puml
│   │   └── parkio-use-cases.puml
│   ├── use-cases/
│   │   └── mvp-use-cases.md
│   └── README.md
├── src/
│   ├── main/
│   │   ├── java/com/kasaca/parkio/
│   │   │   ├── audit/
│   │   │   ├── cajon/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── mapper/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   ├── estacionamiento/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── mapper/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   ├── rol/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── mapper/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   ├── shared/
│   │   │   │   ├── entity/
│   │   │   │   └── exception/
│   │   │   ├── usuario/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   └── ParkioApplication.java
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── application.yaml
│   │       └── banner.txt
│   └── test/
│       └── java/com/kasaca/parkio/
│           ├── cajon/
│           │   ├── controller/
│           │   ├── mapper/
│           │   └── service/
│           ├── estacionamiento/
│           │   ├── controller/
│           │   ├── mapper/
│           │   └── service/
│           ├── rol/
│           │   ├── controller/
│           │   ├── mapper/
│           │   └── service/
│           └── ParkioApplicationTests.java
├── .gitattributes
├── .gitignore
├── HELP.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

El directorio `target/` contiene artefactos generados por Maven y no forma parte del código fuente.

## Modelo de Datos (resumen)

Todas las entidades principales heredan de `BaseEntity`, que define:

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Identificador autogenerado |
| `activo` | `Boolean` | Indicador de registro activo |
| `fechaCreacion` | `LocalDateTime` | Fecha de creación administrada por auditoría JPA |
| `fechaActualizacion` | `LocalDateTime` | Fecha de última actualización |

### Usuario

Representa a un usuario del sistema.

Campos propios:

- Nombre.
- Apellido opcional.
- Correo electrónico único.
- Hash de contraseña.
- Roles asociados.
- Estacionamientos asociados.

### Rol

Representa un rol asignable a usuarios.

Campos propios:

- Nombre único.
- Usuarios asociados.

### Estacionamiento

Representa un estacionamiento y su ubicación.

Campos propios:

- Nombre.
- Descripción opcional.
- Latitud.
- Longitud.
- Cajones asociados.
- Usuarios asociados.

### Cajón

Representa un espacio perteneciente a un estacionamiento.

Campos propios:

- Número.
- Tipo.
- Estado.
- Estacionamiento propietario.

`tipo` y `estado` se almacenan como `VARCHAR(30)` y se modelan mediante los enums `TipoCajon` y `EstadoCajon`. Los tipos disponibles son `AUTO`, `MOTO`, `DISCAPACITADO` y `ELECTRICO`; los estados disponibles son `LIBRE`, `OCUPADO` y `FUERA_SERVICIO`.

### Relaciones

```text
Usuario  * ─── *  Rol
Usuario  * ─── *  Estacionamiento
Estacionamiento  1 ─── *  Cajón
```

Las relaciones muchos a muchos utilizan las tablas intermedias:

- `usuario_rol`
- `usuario_estacionamiento`

Cada número de cajón debe ser único dentro de un estacionamiento, de acuerdo con la restricción definida en la migración `V6`.

## Módulos Existentes

### Auth

Incluye:

- `AuthLoginRequest` y `AuthResponse`.
- `AuthController`.
- `AuthService`.
- `AuthServiceImpl`.
- `JwtService`.
- `JwtProperties`.
- `SecurityConfig`.
- `UnauthorizedException`.
- Pruebas unitarias y de configuración de seguridad.

El módulo implementa inicio de sesión mediante correo y contraseña. Las credenciales se validan contra `Usuario.passwordHash` usando `PasswordEncoder` y BCrypt. Cuando son válidas, se emite un JWT con el correo del usuario, su identificador y sus roles como claims. El login está disponible en `/api/auth/login`.

Los endpoints distintos al login y la creación de usuarios requieren encabezado `Authorization: Bearer <token>`. La creación de usuarios permanece pública para permitir el registro inicial. El módulo Rol requiere rol `ADMIN`. En Usuario, `ADMIN` puede administrar usuarios, mientras que `USER` y `OPERADOR` pueden consultar, actualizar y cambiar la contraseña únicamente de su propio usuario. En Estacionamiento, `ADMIN`, `OPERADOR` y `USER` pueden consultar, pero solo `ADMIN` puede crear, actualizar o eliminar. Cajón todavía no tiene autorización específica por rol.

### Usuario

Incluye:

- Entidad `Usuario`.
- `UsuarioCreateRequest`, `UsuarioUpdateRequest`, `UsuarioPasswordRequest`, `UsuarioRolRequest`, `UsuarioEstacionamientoRequest` y `UsuarioResponse`.
- `UsuarioRepository`.
- `UsuarioService`.
- `UsuarioServiceImpl`.
- `UsuarioMapper`.
- `UsuarioController`.
- Validaciones de entrada.
- Hash de contraseñas mediante `PasswordEncoder` y BCrypt.
- Pruebas unitarias de mapper, servicio y controlador.

El repositorio utiliza `Long` como tipo de identificador, en concordancia con `BaseEntity`.

El módulo implementa operaciones para listar, consultar, crear, actualizar y eliminar usuarios, además de asignar y retirar roles y estacionamientos. Valida correos duplicados y asociaciones, utiliza transacciones y nunca incluye `passwordHash` en las respuestas. `UsuarioResponse` expone los nombres de roles y los identificadores de estacionamientos asociados.

La autorización de Usuario usa `@PreAuthorize` y el helper `UsuarioSecurity` para comparar el `usuarioId` de la ruta contra el claim `usuarioId` del JWT. `ADMIN` puede administrar usuarios; `USER` y `OPERADOR` solo pueden consultar, actualizar y cambiar la contraseña de su propio usuario. Las operaciones de asignación y retiro de roles o estacionamientos son exclusivas de `ADMIN`.

La creación, actualización general y modificación de contraseña utilizan DTOs y operaciones separadas. La autenticación se realiza desde el módulo Auth.

### Rol

Incluye:

- Entidad `Rol`.
- `RolRequest` y `RolResponse`.
- `RolRepository`.
- `RolService`.
- `RolServiceImpl`.
- `RolMapper`.
- `RolController`.
- Validaciones de entrada.
- Pruebas unitarias de mapper, servicio y controlador.

El módulo implementa operaciones para listar, consultar, crear, actualizar y eliminar roles. Utiliza DTOs, mapper, transacciones, validación de nombres duplicados y las excepciones compartidas `ResourceNotFoundException` y `ConflictException`.

### Estacionamiento

Incluye:

- Entidad `Estacionamiento`.
- `EstacionamientoRequest` y `EstacionamientoResponse`.
- `EstacionamientoRepository`.
- `EstacionamientoService`.
- `EstacionamientoServiceImpl`.
- `EstacionamientoMapper`.
- `EstacionamientoController`.
- Validaciones de entrada.
- Pruebas unitarias de mapper, servicio y controlador.

El módulo implementa operaciones para listar, consultar, crear, actualizar y eliminar estacionamientos. Utiliza DTOs, mapper, transacciones y `ResourceNotFoundException` para recursos inexistentes. La eliminación actual es física; una restricción de integridad se traduce a `409 Conflict` mediante el manejador global. La autorización permite listar y consultar a `ADMIN`, `OPERADOR` y `USER`; crear, actualizar y eliminar son operaciones exclusivas de `ADMIN`.

### Cajón

Incluye:

- Entidad `Cajon`.
- `CajonRequest` y `CajonResponse`.
- `CajonRepository`.
- `CajonService`.
- `CajonServiceImpl`.
- `CajonMapper`.
- `CajonController`.
- `TipoCajon` y `EstadoCajon`.
- `CajonEstadoRequest` para cambios de estado.
- Pruebas unitarias de mapper, servicio y controlador.

El módulo implementa operaciones para listar, filtrar por estacionamiento, consultar, crear, actualizar, cambiar el estado y eliminar cajones. Valida la existencia del cajón y del estacionamiento, evita números duplicados dentro del mismo estacionamiento y aplica Jakarta Validation en `CajonRequest` y `CajonEstadoRequest`.

### Auditoría

`BaseEntity` utiliza `AuditingEntityListener`, `@CreatedDate` y `@LastModifiedDate`.

La auditoría se habilita en `ParkioApplication` mediante:

```java
@EnableJpaAuditing
```

Existe además una clase `AuditConfig`, aunque actualmente no contiene configuración adicional.

## Configuración del Entorno

La configuración se encuentra en:

```text
src/main/resources/application.yaml
```

Configuración actual:

```yaml
server:
  port: 8023

spring:
  application:
    name: parkio

  datasource:
    url: jdbc:postgresql://localhost:5432/parkio
    username: postgres
    password: 123123

  jpa:
    hibernate:
      ddl-auto: validate

    show-sql: true

  flyway:
    enabled: true

parkio:
  security:
    jwt:
      issuer: parkio
      secret: ${PARKIO_JWT_SECRET:clave-local-de-desarrollo-cambiar-en-produccion}
      expiration-minutes: 60
```

La aplicación espera:

- PostgreSQL disponible en `localhost:5432`.
- Una base de datos llamada `parkio`.
- El usuario `postgres`.
- La contraseña configurada actualmente en el archivo.
- La variable `PARKIO_JWT_SECRET` configurada para entornos reales o productivos.
- El puerto HTTP `8023` disponible.

La configuración de JWT permite externalizar el secreto mediante `PARKIO_JWT_SECRET`. Las credenciales de base de datos todavía permanecen en `application.yaml`; para entornos compartidos o productivos se recomienda externalizarlas y crear perfiles de Spring.

Hibernate utiliza `ddl-auto: validate`, por lo que valida el esquema, pero no crea ni actualiza las tablas. Flyway es responsable de ejecutar las migraciones.

## Requisitos Previos

- JDK 21.
- PostgreSQL.
- Git, si se obtiene el proyecto desde un repositorio remoto.
- Un puerto PostgreSQL disponible en `5432`.
- El puerto `8023` disponible para la aplicación.

No es obligatorio instalar Maven globalmente, ya que el proyecto incluye Maven Wrapper 3.9.16.

La URL del repositorio remoto y un procedimiento oficial para aprovisionar PostgreSQL no están documentados en el proyecto.

## Instalación

1. Obtener el código fuente y entrar al directorio del proyecto:

   ```bash
   cd parkio
   ```

2. Crear la base de datos PostgreSQL:

   ```sql
   CREATE DATABASE parkio;
   ```

3. Verificar que las credenciales de `application.yaml` correspondan con la instalación local de PostgreSQL.

4. Descargar dependencias y compilar el proyecto.

   En Windows:

   ```powershell
   .\mvnw.cmd clean package
   ```

   En Linux o macOS:

   ```bash
   ./mvnw clean package
   ```

El proyecto contiene una prueba de carga del contexto de Spring y pruebas unitarias para mapper, servicio y controlador de Rol, Estacionamiento, Cajón y Usuario. Todavía no existe una suite de integración dedicada con PostgreSQL.

## Ejecución Local

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

Si la conexión con PostgreSQL y las migraciones son correctas, la aplicación inicia en:

```text
http://localhost:8023
```

Actualmente está disponible el login bajo `/api/auth/login` y la creación de usuarios mediante `POST /api/usuarios` sin token. Los endpoints CRUD de roles bajo `/api/roles` requieren un token JWT válido con rol `ADMIN`. En `/api/usuarios`, las operaciones administrativas requieren `ADMIN` y las operaciones sobre el propio usuario permiten `USER` u `OPERADOR` cuando el `usuarioId` de la ruta coincide con el claim del JWT. En `/api/estacionamientos`, las consultas permiten `ADMIN`, `OPERADOR` y `USER`, mientras que las modificaciones requieren `ADMIN`. Los endpoints de cajones bajo `/api/cajones` requieren un token JWT válido, pero todavía no tienen reglas específicas por rol.

También es posible ejecutar el artefacto compilado:

```bash
java -jar target/parkio-0.0.1-SNAPSHOT.jar
```

## Migraciones Flyway

Flyway está habilitado y utiliza la ubicación convencional:

```text
src/main/resources/db/migration
```

Migraciones existentes:

| Versión | Archivo | Propósito |
|---|---|---|
| V1 | `V1__create_rol.sql` | Crea la tabla `rol` |
| V2 | `V2__create_usuario.sql` | Crea la tabla `usuario` |
| V3 | `V3__create_usuario_rol.sql` | Crea la relación entre usuarios y roles |
| V4 | `V4__create_estacionamiento.sql` | Crea la tabla `estacionamiento` |
| V5 | `V5__create_usuario_estacionamiento.sql` | Crea la relación entre usuarios y estacionamientos |
| V6 | `V6__create_cajon.sql` | Crea la tabla `cajon` y sus restricciones |

Las migraciones se ejecutan automáticamente al iniciar la aplicación.

No se incluyen migraciones con datos iniciales. Por tanto, el proyecto no crea usuarios, roles ni estacionamientos predeterminados.

Las migraciones aplicadas no deben modificarse una vez utilizadas en un entorno compartido. Los cambios futuros de esquema deben agregarse mediante nuevas versiones consecutivas.

## Convenciones de Desarrollo

Las convenciones observadas en el proyecto son:

- Paquete base: `com.kasaca.parkio`.
- Organización por módulo de dominio.
- Entidades persistentes en paquetes `entity`.
- DTOs implementados como `record`.
- Repositorios basados en `JpaRepository`.
- Interfaces e implementaciones de servicio separadas.
- Uso de nombres de clases en singular.
- Uso de nombres de tablas y columnas en español y `snake_case`.
- Uso de `Long` como identificador en `BaseEntity`.
- Auditoría común mediante herencia.
- Relaciones JPA configuradas con carga diferida cuando se declara explícitamente.
- Migraciones Flyway con el formato `V{número}__{descripción}.sql`.
- Diagramas técnicos almacenados como archivos PlantUML.

Antes de ampliar la implementación conviene mantener estas reglas:

- Usar `Long` consistentemente como tipo de identificador en entidades, repositorios y servicios.
- No exponer entidades JPA directamente desde los controladores.
- Utilizar DTOs para solicitudes y respuestas.
- Agregar validaciones Jakarta Validation a los DTOs de entrada.
- Mantener la lógica de negocio en la capa de servicios.
- Mantener el acceso a datos en repositorios.
- Externalizar secretos y credenciales.
- Añadir pruebas para servicios, repositorios y controladores.
- Crear nuevas migraciones en lugar de modificar migraciones ya aplicadas.

Estas últimas recomendaciones todavía no están aplicadas completamente en el código actual.

## Documentación Disponible

La carpeta `docs/` contiene:

| Documento | Descripción |
|---|---|
| `api/parkio-api-v1.md` | Contrato implementado para Auth, Rol, Estacionamiento, Cajón y Usuario |
| `architecture/spring-boot-architecture.puml` | Arquitectura objetivo por capas |
| `architecture/parkio-package-structure.puml` | Organización propuesta de paquetes |
| `architecture/parkio-jwt-flow.puml` | Flujo de autenticación JWT |
| `erd/parkio-erd.puml` | Diagrama entidad-relación |
| `uml/parkio-domain.puml` | Modelo de dominio |
| `uml/parkio-use-cases.puml` | Casos de uso y actores |
| `sequence/parkio-login-sequence.puml` | Secuencia propuesta de inicio de sesión |
| `sequence/parkio-create-estacionamiento-sequence.puml` | Secuencia propuesta para registrar estacionamientos |
| `sequence/parkio-create-cajon-sequence.puml` | Secuencia propuesta para registrar cajones |
| `use-cases/mvp-use-cases.md` | Casos de uso iniciales del MVP |

Parte de esta documentación describe componentes futuros. Los módulos Auth, Rol, Estacionamiento, Cajón y Usuario, el manejo global de errores y la autenticación JWT están implementados. La autorización granular por roles ya está aplicada en Rol, Usuario y Estacionamiento; todavía falta aplicarla a Cajón.

## Roadmap Futuro

A partir de las brechas entre el código y la documentación, el trabajo pendiente incluye:

- Completar autorización granular por roles en Cajón.
- Externalizar la configuración sensible.
- Incorporar perfiles para desarrollo, pruebas y producción.
- Añadir pruebas de integración con PostgreSQL.
- Mantener sincronizados el contrato API y el código implementado.

Este roadmap se deriva de la documentación existente y del estado incompleto del código. No representa funcionalidades ya disponibles.

## Autor

Christian Salazar
eltalchris@gmail.com

El autor también está declarado en la sección `developers` del `pom.xml`. El paquete base y el `groupId` utilizan el identificador:

```text
com.kasaca
```

El proyecto se encuentra asociado técnicamente con Kasaca.
