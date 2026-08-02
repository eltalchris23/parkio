# Parkio API v1

## Información General

### Base URL

```text
/api/v1
```

### Estado del contrato

Este documento describe el contrato implementado actualmente para los módulos:

- Auth.
- Usuario.
- Rol.
- Estacionamiento.
- Cajón.
- Reserva.
- Ticket.
- Tarifa.
- Pago.
- Catálogos.

El módulo Tarifa expone endpoints REST para consultar, crear, actualizar y desactivar lógicamente tarifas activas por estacionamiento. La tarifa activa se usa al registrar salida de un ticket para calcular y persistir el cobro.

El módulo Pago expone endpoints REST para listar pagos de forma paginada con filtros, registrar el cobro de tickets pendientes de pago, calcular cambio, cerrar tickets y liberar cajones.

No describe funcionalidades futuras salvo que se indiquen explícitamente como pendientes.

### Autenticación

La autenticación JWT está implementada con Spring Security y OAuth2 Resource Server.

Endpoints públicos:

```http
POST /api/v1/auth/login
POST /api/v1/usuarios
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

Todos los demás endpoints de negocio requieren JWT válido:

```http
Authorization: Bearer <token>
```

El endpoint `GET /api/v1/auth/me` requiere JWT válido y devuelve la información vigente del usuario autenticado.

El JWT incluye, entre otros datos:

- `sub`: correo del usuario.
- `usuarioId`: identificador del usuario.
- `roles`: roles asignados al usuario.

Los roles del claim `roles` se convierten a authorities de Spring Security con prefijo `ROLE_`. Por ejemplo, `ADMIN` se interpreta internamente como `ROLE_ADMIN`.

### Health Check

Los endpoints de Health Check están implementados con Spring Boot Actuator y se exponen fuera de la base `/api/v1`:

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

No requieren JWT porque están pensados para monitoreo, balanceadores, contenedores, despliegues o frontends que necesiten verificar disponibilidad básica del backend.

Respuesta esperada:

```json
{
  "status": "UP"
}
```

La configuración actual expone únicamente `health` y mantiene ocultos los detalles internos mediante `show-details: never`. No están documentados como públicos endpoints sensibles de Actuator como `env`, `beans`, `configprops` o `metrics`.

### OpenAPI y Swagger UI

La documentación interactiva está implementada con Springdoc OpenAPI.

Los controladores principales ya incluyen anotaciones OpenAPI:

- `AuthController`.
- `RolController`.
- `EstacionamientoController`.
- `CajonController`.
- `UsuarioController`.
- `ReservaController`.
- `TicketController`.
- `TarifaEstacionamientoController`.
- `PagoController`.
- `CatalogoController`.

En ambiente de desarrollo:

```http
GET /api/v1/swagger-ui.html
GET /api/v1/v3/api-docs
```

`/api/v1/swagger-ui.html` abre la interfaz visual para consultar y probar la API. `/api/v1/v3/api-docs` expone el contrato OpenAPI en formato JSON.

Swagger UI se genera a partir de los controladores reales y muestra la documentación declarada con `@Tag`, `@Operation`, respuestas HTTP y parámetros relevantes. Los endpoints de negocio conservan la base global:

```text
/api/v1
```

Ejemplo:

```http
POST /api/v1/auth/login
GET /api/v1/auth/me
GET /api/v1/roles
GET /api/v1/estacionamientos
GET /api/v1/cajones
POST /api/v1/reservas
GET /api/v1/reservas/mis-reservas
PATCH /api/v1/reservas/{reservaId}/cancelar
POST /api/v1/tickets/entrada
PATCH /api/v1/tickets/{ticketId}/salida
GET /api/v1/tarifas/estacionamiento/{estacionamientoId}
POST /api/v1/tarifas
PUT /api/v1/tarifas/estacionamiento/{estacionamientoId}
DELETE /api/v1/tarifas/estacionamiento/{estacionamientoId}
POST /api/v1/pagos
GET /api/v1/pagos
GET /api/v1/pagos/ticket/{ticketId}
GET /api/v1/usuarios
GET /api/v1/catalogos/cajones/tipos
GET /api/v1/catalogos/cajones/estados
```

Para probar endpoints protegidos desde Swagger UI se debe usar el botón `Authorize` y proporcionar un JWT con el esquema Bearer.

`UsuarioController` documenta la seguridad por método porque combina un endpoint público de registro (`POST /api/v1/usuarios`) con endpoints protegidos por JWT.

Springdoc está deshabilitado por defecto y también en el perfil `prod`. Actualmente se habilita desde el perfil `dev`.

### Autorización por roles

Roles base existentes en base de datos:

| Rol | Uso actual |
|---|---|
| `ADMIN` | Administración global de Parkio |
| `OWNER` | Dueño de uno o varios estacionamientos; puede administrar sus propios estacionamientos y los cajones asociados |
| `OPERADOR` | Operación de estacionamientos según permisos actuales |
| `USER` | Usuario/cliente final y rol asignado por defecto en el registro público |

| Módulo | Consulta | Escritura / Administración |
|---|---|---|
| Auth | Público para login; `/auth/me` requiere JWT válido | No aplica |
| Usuario | `ADMIN`; o propio usuario para `USER`/`OPERADOR` en endpoints permitidos | `ADMIN`; o propio usuario para actualización/cambio de contraseña |
| Rol | `ADMIN` | `ADMIN` |
| Estacionamiento | `ADMIN`, `OWNER`, `OPERADOR`, `USER`; `OWNER` solo ve los propios | `ADMIN`; `OWNER` solo administra los propios |
| Cajón | `ADMIN`, `OWNER`, `OPERADOR`, `USER`; `OWNER` solo ve cajones de estacionamientos propios | `ADMIN`; `OWNER` solo administra cajones propios; cambio de estado también permite `OPERADOR` |
| Reserva | `USER` consulta sus propias reservas; `ADMIN`, `OWNER` y `OPERADOR` consultan por código; `ADMIN` consulta por ID | `USER` crea y cancela reservas propias |
| Ticket | `ADMIN`, `OWNER`, `OPERADOR` y `USER` consultan tickets según alcance | `ADMIN`, `OWNER` y `OPERADOR` registran entrada y salida según alcance |
| Tarifa | `ADMIN` global; `OWNER` solo sobre estacionamientos propios | `ADMIN` global; `OWNER` solo sobre estacionamientos propios |
| Pago | `ADMIN`, `OWNER` y `OPERADOR` listan pagos según alcance; `ADMIN`, `OWNER`, `OPERADOR` y `USER` consultan pagos por ticket según alcance | `ADMIN`, `OWNER` y `OPERADOR` registran pagos según alcance |
| Catálogos | `ADMIN`, `OPERADOR`, `USER` | No aplica |

### Identificador de transacción

Todas las respuestas HTTP incluyen el header:

```http
X-Transaction-Id: <uuid-o-valor-enviado-por-cliente>
```

Si el cliente envía `X-Transaction-Id`, Parkio reutiliza ese valor. Si no lo envía, el backend genera uno nuevo.

El mismo valor se incluye en:

- respuestas exitosas estandarizadas;
- respuestas de error;
- MDC de logs.

### Respuesta exitosa estándar

Las operaciones con cuerpo usan `ApiResponse<T>`:

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Operación realizada correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {}
}
```

### Respuesta paginada estándar

Los listados usan `ApiResponse<PageResponse<T>>`:

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Registros consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "content": [],
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true,
    "empty": true
  }
}
```

Parámetros de paginación soportados por Spring Data:

| Parámetro | Descripción |
|---|---|
| `page` | Número de página. Inicia en `0`. |
| `size` | Cantidad máxima de registros por página. |
| `sort` | Campo Java y dirección. Ejemplo: `nombre,asc`. |

### Respuesta de error estándar

Las respuestas de error usan `ApiError`:

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos inválidos",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "path": "/api/v1/roles",
  "validationErrors": {
    "nombre": "El nombre del rol es obligatorio"
  }
}
```

Mapeo de errores implementado:

| Excepción / caso | HTTP |
|---|---|
| Validación Jakarta Validation | `400 Bad Request` |
| JSON inválido o enum inválido | `400 Bad Request` |
| Falta de autenticación | `401 Unauthorized` |
| Falta de permisos | `403 Forbidden` |
| Recurso inexistente o inactivo | `404 Not Found` |
| Conflicto de negocio o integridad | `409 Conflict` |
| Error no controlado | `500 Internal Server Error` |

### Borrado lógico

Las operaciones `DELETE` de Rol, Usuario, Estacionamiento y Cajón realizan borrado lógico mediante `activo=false`.

Reglas implementadas:

- Los registros inactivos no aparecen en listados.
- Consultar un registro inactivo por identificador responde `404 Not Found`.
- Un usuario inactivo no puede iniciar sesión.
- Al desactivar un estacionamiento, también se desactivan sus cajones activos.
- Las restricciones únicas de base de datos siguen aplicando aunque el registro esté inactivo.

### Bootstrap del primer ADMIN

`POST /api/v1/usuarios` crea usuarios con rol base `USER`. No existe creación pública de administradores.

Para habilitar el primer administrador en un ambiente local o controlado:

```sql
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuario u
JOIN rol r ON r.nombre = 'ADMIN'
WHERE u.email = 'tu-correo@dominio.com'
ON CONFLICT DO NOTHING;
```

Después de asignar el rol, el usuario debe iniciar sesión nuevamente para obtener un JWT actualizado.

## Módulo Auth

### Login

```http
POST /api/v1/auth/login
```

Endpoint público.

#### Request

```json
{
  "email": "admin@parkio.com",
  "password": "123456"
}
```

Validaciones:

- `email` es obligatorio y debe tener formato válido.
- `password` es obligatorio.

#### Response 200

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

#### Errores

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos |
| `401` | Credenciales inválidas o usuario inactivo |

### Usuario autenticado

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

Endpoint protegido. Requiere JWT válido, pero no requiere un rol específico adicional.

Este endpoint consulta la información vigente del usuario autenticado usando el claim `usuarioId` del JWT. El frontend puede usarlo después del login o al recargar la aplicación para conocer el usuario, sus roles y sus estacionamientos asignados sin decodificar directamente el token.

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Usuario autenticado consultado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Christian",
    "apellido": "Hernandez",
    "email": "christian@parkio.com",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00",
    "roles": ["ADMIN"],
    "estacionamientoIds": [1]
  }
}
```

#### Errores

| HTTP | Causa |
|---|---|
| `401` | Token ausente, inválido o sin claim `usuarioId` |
| `404` | Usuario autenticado no encontrado o inactivo |

## Módulo Rol

Seguridad:

- Requiere JWT válido.
- Requiere rol `ADMIN`.

### Listar roles

```http
GET /api/v1/roles?page=0&size=10&sort=nombre,asc
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Roles consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "content": [
      {
        "id": 1,
        "nombre": "ADMIN",
        "activo": true,
        "fechaCreacion": "2026-07-18T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

### Consultar rol

```http
GET /api/v1/roles/{rolId}
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Rol consultado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "ADMIN",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Crear rol

```http
POST /api/v1/roles
```

#### Request

```json
{
  "nombre": "SUPERVISOR",
  "activo": true
}
```

Validaciones:

- `nombre` es obligatorio.
- `nombre` permite máximo 50 caracteres.
- `activo` es obligatorio.
- `nombre` no debe estar duplicado.

#### Response 201

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 201,
  "message": "Rol creado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 4,
    "nombre": "SUPERVISOR",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Actualizar rol

```http
PUT /api/v1/roles/{rolId}
```

Usa el mismo cuerpo de creación.

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Rol actualizado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 4,
    "nombre": "SUPERVISOR_GENERAL",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Eliminar rol

```http
DELETE /api/v1/roles/{rolId}
```

#### Response 204

Sin cuerpo. Realiza borrado lógico.

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin rol `ADMIN` |
| `404` | Rol inexistente o inactivo |
| `409` | Nombre duplicado o restricción de integridad |

## Módulo Usuario

Seguridad:

- `POST /api/v1/usuarios` es público y asigna automáticamente rol base `USER`.
- `GET /api/v1/usuarios` y `DELETE /api/v1/usuarios/{usuarioId}` requieren `ADMIN`.
- `GET /api/v1/usuarios/{usuarioId}` y `PUT /api/v1/usuarios/{usuarioId}` permiten `ADMIN`, el propio usuario autenticado o `OWNER` cuando el usuario objetivo es un `OPERADOR` asignado a uno de sus estacionamientos.
- `PATCH /api/v1/usuarios/{usuarioId}/password` permite `ADMIN` o el propio usuario autenticado. `OWNER` no cambia contraseñas de operadores.
- `POST /api/v1/usuarios/{usuarioId}/roles` y `DELETE /api/v1/usuarios/{usuarioId}/roles/{rolId}` requieren `ADMIN`. `OWNER` no asigna ni retira roles.
- `POST /api/v1/usuarios/{usuarioId}/estacionamientos` permite `ADMIN` o `OWNER` cuando el estacionamiento pertenece al `OWNER` autenticado y el usuario objetivo ya tiene rol `OPERADOR`.
- `DELETE /api/v1/usuarios/{usuarioId}/estacionamientos/{estacionamientoId}` permite `ADMIN` o `OWNER` cuando retira uno de sus propios estacionamientos de un usuario con rol `OPERADOR`.

Las respuestas nunca incluyen `passwordHash`.

### Listar usuarios

```http
GET /api/v1/usuarios?page=0&size=10&sort=email,asc
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Usuarios consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "content": [
      {
        "id": 1,
        "nombre": "Juan",
        "apellido": "Pérez",
        "email": "juan@parkio.com",
        "activo": true,
        "fechaCreacion": "2026-07-18T10:00:00",
        "roles": [
          "USER"
        ],
        "estacionamientoIds": []
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

### Crear usuario

```http
POST /api/v1/usuarios
```

Endpoint público.

#### Request

```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com",
  "password": "123456"
}
```

Validaciones:

- `nombre` es obligatorio y permite máximo 100 caracteres.
- `apellido` es opcional y permite máximo 100 caracteres.
- `email` es obligatorio, debe tener formato válido y permite máximo 150 caracteres.
- `password` es obligatorio.
- `email` no debe estar duplicado.
- El rol base `USER` debe existir.

#### Response 201

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 201,
  "message": "Usuario creado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@parkio.com",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00",
    "roles": [
      "USER"
    ],
    "estacionamientoIds": []
  }
}
```

### Consultar usuario

```http
GET /api/v1/usuarios/{usuarioId}
```

Permisos: `ADMIN` puede consultar cualquier usuario; cualquier usuario autenticado puede consultarse a sí mismo; `OWNER` puede consultar operadores asignados a sus propios estacionamientos.

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Usuario consultado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@parkio.com",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00",
    "roles": [
      "USER"
    ],
    "estacionamientoIds": []
  }
}
```

### Actualizar usuario

```http
PUT /api/v1/usuarios/{usuarioId}
```

Permisos: `ADMIN` puede actualizar cualquier usuario; cualquier usuario autenticado puede actualizarse a sí mismo; `OWNER` puede actualizar operadores asignados a sus propios estacionamientos.

#### Request

```json
{
  "nombre": "Juan Carlos",
  "apellido": "Pérez",
  "email": "juan.carlos@parkio.com"
}
```

La actualización general no exige contraseña.

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Usuario actualizado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Juan Carlos",
    "apellido": "Pérez",
    "email": "juan.carlos@parkio.com",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00",
    "roles": [
      "USER"
    ],
    "estacionamientoIds": []
  }
}
```

### Cambiar contraseña

```http
PATCH /api/v1/usuarios/{usuarioId}/password
```

Permisos: `ADMIN` puede cambiar la contraseña de cualquier usuario; cualquier usuario autenticado puede cambiar su propia contraseña. `OWNER` no cambia contraseñas de operadores salvo que sea su propia cuenta.

#### Request

```json
{
  "nuevaPassword": "nueva-clave"
}
```

#### Response 204

Sin cuerpo.

### Eliminar usuario

```http
DELETE /api/v1/usuarios/{usuarioId}
```

Requiere rol `ADMIN`. `OWNER` no realiza borrado lógico global de operadores; para quitar a un operador de un estacionamiento debe usar el retiro de estacionamiento.

#### Response 204

Sin cuerpo. Realiza borrado lógico.

### Asignar rol a usuario

```http
POST /api/v1/usuarios/{usuarioId}/roles
```

Requiere rol `ADMIN`. `OWNER` no puede asignar roles para evitar elevación de privilegios.

#### Request

```json
{
  "rolId": 1
}
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Rol asignado correctamente al usuario",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@parkio.com",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00",
    "roles": [
      "USER",
      "OPERADOR"
    ],
    "estacionamientoIds": []
  }
}
```

### Retirar rol de usuario

```http
DELETE /api/v1/usuarios/{usuarioId}/roles/{rolId}
```

Requiere rol `ADMIN`. `OWNER` no puede retirar roles.

#### Response 204

Sin cuerpo.

### Asignar estacionamiento a usuario

```http
POST /api/v1/usuarios/{usuarioId}/estacionamientos
```

Permisos: `ADMIN` puede asignar cualquier estacionamiento activo. `OWNER` solo puede asignar estacionamientos propios a usuarios activos que ya tengan rol `OPERADOR`.

#### Request

```json
{
  "estacionamientoId": 1
}
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Estacionamiento asignado correctamente al usuario",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@parkio.com",
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00",
    "roles": [
      "USER"
    ],
    "estacionamientoIds": [
      1
    ]
  }
}
```

### Retirar estacionamiento de usuario

```http
DELETE /api/v1/usuarios/{usuarioId}/estacionamientos/{estacionamientoId}
```

Permisos: `ADMIN` puede retirar cualquier relación. `OWNER` solo puede retirar estacionamientos propios de usuarios activos con rol `OPERADOR`.

#### Response 204

Sin cuerpo.

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos |
| `401` | JWT ausente o inválido en endpoints protegidos |
| `403` | Usuario sin permisos o intentando operar otro usuario |
| `404` | Usuario, rol o estacionamiento inexistente/inactivo |
| `409` | Email duplicado, relación ya existe o relación no existe al retirarla |

## Módulo Estacionamiento

Seguridad:

- Requiere JWT válido.
- `GET /api/v1/estacionamientos` y `GET /api/v1/estacionamientos/{estacionamientoId}` permiten `ADMIN`, `OWNER`, `OPERADOR` y `USER`.
- `ADMIN` consulta y administra todos los estacionamientos.
- `OWNER` consulta, crea, actualiza y elimina lógicamente solo sus propios estacionamientos.
- `OPERADOR` consulta únicamente estacionamientos asignados mediante `usuario_estacionamiento`.
- `USER` conserva la consulta permitida actual.
- `POST`, `PUT` y `DELETE` requieren `ADMIN` u `OWNER`; la capa de servicio limita a `OWNER` a sus propios estacionamientos.

### Listar estacionamientos

```http
GET /api/v1/estacionamientos?page=0&size=10&sort=nombre,asc
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Estacionamientos consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "content": [
      {
        "id": 1,
        "nombre": "Parkio Centro",
        "descripcion": "Sucursal Centro Histórico",
        "latitud": 19.432608,
        "longitud": -99.133209,
        "ownerId": 2,
        "activo": true,
        "fechaCreacion": "2026-07-18T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

### Consultar estacionamiento

```http
GET /api/v1/estacionamientos/{estacionamientoId}
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Estacionamiento consultado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Parkio Centro",
    "descripcion": "Sucursal Centro Histórico",
    "latitud": 19.432608,
    "longitud": -99.133209,
    "ownerId": 2,
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Crear estacionamiento

```http
POST /api/v1/estacionamientos
```

#### Request

```json
{
  "nombre": "Parkio Centro",
  "descripcion": "Sucursal Centro Histórico",
  "latitud": 19.432608,
  "longitud": -99.133209
}
```

Validaciones:

- `nombre` es obligatorio y permite máximo 150 caracteres.
- `descripcion` es opcional y permite máximo 500 caracteres.
- `latitud` es obligatoria, debe estar entre `-90` y `90`, y permite hasta 8 decimales.
- `longitud` es obligatoria, debe estar entre `-180` y `180`, y permite hasta 8 decimales.

#### Response 201

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 201,
  "message": "Estacionamiento creado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Parkio Centro",
    "descripcion": "Sucursal Centro Histórico",
    "latitud": 19.432608,
    "longitud": -99.133209,
    "ownerId": 2,
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Actualizar estacionamiento

```http
PUT /api/v1/estacionamientos/{estacionamientoId}
```

Usa el mismo cuerpo de creación.

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Estacionamiento actualizado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "nombre": "Parkio Reforma",
    "descripcion": "Sucursal Reforma",
    "latitud": 19.427,
    "longitud": -99.1677,
    "ownerId": 2,
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Eliminar estacionamiento

```http
DELETE /api/v1/estacionamientos/{estacionamientoId}
```

#### Response 204

Sin cuerpo. Realiza borrado lógico del estacionamiento y de sus cajones activos asociados.

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin rol `ADMIN` en operaciones de escritura |
| `404` | Estacionamiento inexistente o inactivo |

## Módulo Cajón

Seguridad:

- Requiere JWT válido.
- `GET /api/v1/cajones`, `GET /api/v1/cajones?estacionamientoId={id}` y `GET /api/v1/cajones/{cajonId}` permiten `ADMIN`, `OWNER`, `OPERADOR` y `USER`.
- `ADMIN` consulta y administra todos los cajones activos.
- `OWNER` consulta, crea, actualiza, cambia estado y elimina lógicamente solo cajones pertenecientes a sus propios estacionamientos.
- `OPERADOR` consulta y cambia estado únicamente en cajones de estacionamientos asignados mediante `usuario_estacionamiento`.
- `USER` conserva la consulta permitida actual.
- `PATCH /api/v1/cajones/{cajonId}/estado` permite `ADMIN`, `OWNER` y `OPERADOR`.
- `POST`, `PUT` y `DELETE` requieren `ADMIN` u `OWNER`; la capa de servicio limita a `OWNER` a sus propios estacionamientos.

Tipos permitidos:

- `AUTO`
- `MOTO`
- `DISCAPACITADO`
- `ELECTRICO`

Estados permitidos:

- `LIBRE`
- `RESERVADO`
- `OCUPADO`
- `FUERA_SERVICIO`

### Listar cajones

```http
GET /api/v1/cajones?page=0&size=10&sort=numero,asc
```

### Listar cajones por estacionamiento

```http
GET /api/v1/cajones?estacionamientoId=1&page=0&size=10&sort=numero,asc
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Cajones consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "content": [
      {
        "id": 1,
        "numero": "A-001",
        "tipo": "AUTO",
        "estado": "LIBRE",
        "estacionamientoId": 1,
        "activo": true,
        "fechaCreacion": "2026-07-18T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

### Consultar cajón

```http
GET /api/v1/cajones/{cajonId}
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Cajon consultado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "numero": "A-001",
    "tipo": "AUTO",
    "estado": "LIBRE",
    "estacionamientoId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Crear cajón

```http
POST /api/v1/cajones
```

#### Request

```json
{
  "numero": "A-001",
  "tipo": "AUTO",
  "estacionamientoId": 1
}
```

Validaciones:

- `numero` es obligatorio y permite máximo 20 caracteres.
- `tipo` es obligatorio y debe ser un valor permitido.
- `estacionamientoId` es obligatorio y positivo.
- El estacionamiento debe existir y estar activo.
- Si el usuario autenticado tiene rol `OWNER`, el estacionamiento debe pertenecerle.
- El número no debe estar duplicado dentro del mismo estacionamiento.

El estado inicial se asigna automáticamente como `LIBRE`.

#### Response 201

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 201,
  "message": "Cajon creado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "numero": "A-001",
    "tipo": "AUTO",
    "estado": "LIBRE",
    "estacionamientoId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Actualizar cajón

```http
PUT /api/v1/cajones/{cajonId}
```

Usa el mismo cuerpo de creación. La actualización conserva el estado operativo actual del cajón. Si el usuario autenticado tiene rol `OWNER`, tanto el cajón original como el estacionamiento destino deben estar dentro de sus propios estacionamientos.

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Cajon actualizado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "numero": "B-002",
    "tipo": "ELECTRICO",
    "estado": "LIBRE",
    "estacionamientoId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Cambiar estado del cajón

```http
PATCH /api/v1/cajones/{cajonId}/estado
```

#### Request

```json
{
  "estado": "OCUPADO"
}
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Estado del cajon actualizado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "numero": "A-001",
    "tipo": "AUTO",
    "estado": "OCUPADO",
    "estacionamientoId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-18T10:00:00"
  }
}
```

### Eliminar cajón

```http
DELETE /api/v1/cajones/{cajonId}
```

#### Response 204

Sin cuerpo. Realiza borrado lógico. Si el usuario autenticado tiene rol `OWNER`, solo puede eliminar cajones de sus propios estacionamientos.

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos, tipo inválido o estado inválido |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin permisos para la operación |
| `404` | Cajón o estacionamiento inexistente/inactivo |
| `409` | Número duplicado dentro del estacionamiento |

## Módulo Reserva

El módulo Reserva permite apartar temporalmente un cajón disponible para un cliente final.

Reglas actuales:

- Requiere JWT válido.
- `USER` puede crear reservas y consultar sus propias reservas.
- `USER` puede cancelar únicamente sus propias reservas.
- `ADMIN`, `OWNER` y `OPERADOR` pueden consultar una reserva por código público.
- `ADMIN` puede consultar una reserva por identificador interno.
- El frontend no envía la duración de la reserva.
- El backend calcula la expiración usando `parkio.reserva.expiracion-minutos`.
- El backend ejecuta una revisión automática de reservas vencidas usando `parkio.reserva.expiracion-check-ms`.
- Al crear una reserva, el cajón cambia a estado `RESERVADO`.
- No se permite crear una reserva si el cajón no está `LIBRE`.
- No se permite crear una reserva si ya existe una reserva `CREADA` y vigente para el mismo cajón.
- Solo se puede cancelar una reserva propia que esté en estado `CREADA` y que todavía no haya expirado.
- Al cancelar o expirar una reserva, el cajón vuelve a `LIBRE` cuando no existe otra reserva vigente sobre el mismo cajón.

### Crear reserva

```http
POST /api/v1/reservas
```

Requiere rol `USER`.

#### Request

```json
{
  "estacionamientoId": 1,
  "cajonId": 1,
  "placa": "ABC123"
}
```

#### Response 201

```json
{
  "timestamp": "2026-07-25T10:00:00",
  "status": 201,
  "message": "Reserva creada correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "codigo": "RSV-A1B2C3D4",
    "placa": "ABC123",
    "estado": "CREADA",
    "fechaReserva": "2026-07-25T10:00:00",
    "fechaExpiracion": "2026-07-25T10:20:00",
    "tiempoExpiracionMinutos": 20,
    "usuarioId": 1,
    "estacionamientoId": 1,
    "cajonId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-25T10:00:00"
  }
}
```

### Consultar mis reservas

```http
GET /api/v1/reservas/mis-reservas?page=0&size=10&sort=fechaReserva,desc
```

Requiere rol `USER`.

#### Response 200

```json
{
  "timestamp": "2026-07-25T10:00:00",
  "status": 200,
  "message": "Reservas consultadas correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "content": [
      {
        "id": 1,
        "codigo": "RSV-A1B2C3D4",
        "placa": "ABC123",
        "estado": "CREADA",
        "fechaReserva": "2026-07-25T10:00:00",
        "fechaExpiracion": "2026-07-25T10:20:00",
        "tiempoExpiracionMinutos": 20,
        "usuarioId": 1,
        "estacionamientoId": 1,
        "cajonId": 1,
        "activo": true,
        "fechaCreacion": "2026-07-25T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

### Consultar reserva por código

```http
GET /api/v1/reservas/codigo/{codigo}
```

Requiere rol `ADMIN`, `OWNER` u `OPERADOR`.

#### Response 200

```json
{
  "timestamp": "2026-07-25T10:00:00",
  "status": 200,
  "message": "Reserva consultada correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "codigo": "RSV-A1B2C3D4",
    "placa": "ABC123",
    "estado": "CREADA",
    "fechaReserva": "2026-07-25T10:00:00",
    "fechaExpiracion": "2026-07-25T10:20:00",
    "tiempoExpiracionMinutos": 20,
    "usuarioId": 1,
    "estacionamientoId": 1,
    "cajonId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-25T10:00:00"
  }
}
```

### Cancelar reserva propia

```http
PATCH /api/v1/reservas/{reservaId}/cancelar
```

Requiere rol `USER`.

La reserva debe pertenecer al usuario autenticado, estar en estado `CREADA` y seguir vigente. Si la reserva ya expiró o ya no está en estado cancelable, el backend responde `409 Conflict`.

#### Response 200

```json
{
  "timestamp": "2026-07-25T10:05:00",
  "status": 200,
  "message": "Reserva cancelada correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "codigo": "RSV-A1B2C3D4",
    "placa": "ABC123",
    "estado": "CANCELADA",
    "fechaReserva": "2026-07-25T10:00:00",
    "fechaExpiracion": "2026-07-25T10:20:00",
    "tiempoExpiracionMinutos": 20,
    "usuarioId": 1,
    "estacionamientoId": 1,
    "cajonId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-25T10:00:00"
  }
}
```

### Consultar reserva por ID

```http
GET /api/v1/reservas/{reservaId}
```

Requiere rol `ADMIN`.

#### Response 200

```json
{
  "timestamp": "2026-07-25T10:00:00",
  "status": 200,
  "message": "Reserva consultada correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "codigo": "RSV-A1B2C3D4",
    "placa": "ABC123",
    "estado": "CREADA",
    "fechaReserva": "2026-07-25T10:00:00",
    "fechaExpiracion": "2026-07-25T10:20:00",
    "tiempoExpiracionMinutos": 20,
    "usuarioId": 1,
    "estacionamientoId": 1,
    "cajonId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-25T10:00:00"
  }
}
```

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos en la solicitud |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin permisos para la operación |
| `404` | Usuario, estacionamiento, cajón o reserva inexistente/inactivo |
| `409` | Cajón no disponible, cajón fuera del estacionamiento indicado, reserva vigente duplicada, reserva no cancelable o reserva vencida |

## Módulo Ticket

El módulo Ticket permite registrar la entrada real de un vehículo al estacionamiento usando una reserva vigente y registrar la salida operativa para calcular el monto a pagar.

Reglas actuales:

- Requiere JWT válido.
- `ADMIN`, `OWNER`, `OPERADOR` y `USER` pueden consultar tickets según alcance.
- `ADMIN`, `OWNER` y `OPERADOR` pueden registrar entradas y salidas según alcance.
- `ADMIN` puede operar tickets de cualquier estacionamiento.
- `OWNER` solo puede operar tickets de estacionamientos propios.
- `OPERADOR` solo puede operar tickets de estacionamientos asignados mediante `usuario_estacionamiento`.
- `USER` solo puede consultar tickets propios.
- El usuario autenticado se obtiene desde el claim `usuarioId` del JWT.
- El frontend no envía el identificador del operador, owner o admin.
- La reserva debe existir, estar activa, estar en estado `CREADA` y seguir vigente.
- Una reserva solo puede convertirse en un ticket activo.
- Un cajón no puede tener más de un ticket `ABIERTO` activo.
- Al registrar entrada, la reserva cambia a `USADA`.
- Al registrar entrada, el cajón cambia a `OCUPADO`.
- El ticket se crea en estado `ABIERTO`.
- Al registrar salida, el ticket cambia a `PENDIENTE_PAGO`.
- Al registrar salida, se asigna `fechaSalida`.
- Al registrar salida, debe existir una tarifa activa para el estacionamiento.
- Al registrar salida, se calcula `montoTotal` usando precio por hora, tolerancia, cobro por fracción y tarifa mínima.
- La tarifa mínima se aplica desde el primer minuto de estancia.
- El ticket guarda los parámetros de tarifa aplicados para conservar trazabilidad histórica aunque la tarifa cambie después.
- Al registrar salida, el cajón permanece `OCUPADO`.
- El cajón se libera hasta que el pago sea registrado en el módulo Pago.
- `CERRADO` representa tickets liquidados mediante pago registrado.

Todavía no está implementada facturación fiscal ni emisión de comprobantes.

### Listar tickets

```http
GET /api/v1/tickets?page=0&size=10&sort=fechaEntrada,desc
```

Filtros opcionales:

```http
GET /api/v1/tickets?estado=ABIERTO
GET /api/v1/tickets?estado=PENDIENTE_PAGO
GET /api/v1/tickets?estado=CERRADO
GET /api/v1/tickets?estacionamientoId=1
GET /api/v1/tickets?estado=ABIERTO&estacionamientoId=1&page=0&size=10&sort=fechaEntrada,desc
```

Parámetros:

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `estado` | `ABIERTO`, `PENDIENTE_PAGO` o `CERRADO` | No | Filtra tickets por estado operativo |
| `estacionamientoId` | `Long` | No | Filtra tickets de un estacionamiento específico |
| `page` | `Integer` | No | Página solicitada, iniciando en `0` |
| `size` | `Integer` | No | Tamaño de página |
| `sort` | `String` | No | Ordenamiento Spring Data, por ejemplo `fechaEntrada,desc` |

Permisos:

- `ADMIN`: ve todos los tickets activos.
- `OWNER`: ve tickets de estacionamientos propios.
- `OPERADOR`: ve tickets de estacionamientos asignados.
- `USER`: ve tickets propios.

Los filtros no amplían permisos. Si un usuario filtra por un estacionamiento fuera de su alcance, la respuesta queda limitada a los tickets que realmente puede ver.

Response `200 OK`:

```json
{
  "timestamp": "2026-08-01T18:30:00",
  "status": 200,
  "message": "Tickets consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "content": [
      {
        "id": 1,
        "codigo": "TCK-ABC12345",
        "estado": "ABIERTO",
        "placa": "ABC123",
        "fechaEntrada": "2026-08-01T18:10:00",
        "fechaSalida": null,
        "minutosEstancia": null,
        "montoTotal": null,
        "precioPorHoraAplicado": null,
        "minutosToleranciaAplicados": null,
        "cobrarFraccionAplicado": null,
        "tarifaMinimaAplicada": null,
        "reservaId": 1,
        "usuarioId": 1,
        "operadorEntradaId": 2,
        "estacionamientoId": 1,
        "cajonId": 1,
        "activo": true,
        "fechaCreacion": "2026-08-01T18:10:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

### Consultar ticket por ID

```http
GET /api/v1/tickets/{ticketId}
```

Permisos:

- `ADMIN`: puede consultar cualquier ticket activo.
- `OWNER`: solo tickets de estacionamientos propios.
- `OPERADOR`: solo tickets de estacionamientos asignados.
- `USER`: solo tickets propios.

Response `200 OK`:

```json
{
  "timestamp": "2026-08-01T18:30:00",
  "status": 200,
  "message": "Ticket consultado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "codigo": "TCK-ABC12345",
    "estado": "ABIERTO",
    "placa": "ABC123",
    "fechaEntrada": "2026-08-01T18:10:00",
    "fechaSalida": null,
    "minutosEstancia": null,
    "montoTotal": null,
    "precioPorHoraAplicado": null,
    "minutosToleranciaAplicados": null,
    "cobrarFraccionAplicado": null,
    "tarifaMinimaAplicada": null,
    "reservaId": 1,
    "usuarioId": 1,
    "operadorEntradaId": 2,
    "estacionamientoId": 1,
    "cajonId": 1,
    "activo": true,
    "fechaCreacion": "2026-08-01T18:10:00"
  }
}
```

### Registrar entrada con reserva

```http
POST /api/v1/tickets/entrada
```

Requiere rol `ADMIN`, `OWNER` u `OPERADOR`.

#### Request

```json
{
  "codigoReserva": "RSV-A1B2C3D4"
}
```

#### Response 201

```json
{
  "timestamp": "2026-07-25T10:10:00",
  "status": 201,
  "message": "Ticket creado correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "codigo": "TCK-A1B2C3D4",
    "estado": "ABIERTO",
    "placa": "ABC123",
    "fechaEntrada": "2026-07-25T10:10:00",
    "fechaSalida": null,
    "minutosEstancia": null,
    "montoTotal": null,
    "precioPorHoraAplicado": null,
    "minutosToleranciaAplicados": null,
    "cobrarFraccionAplicado": null,
    "tarifaMinimaAplicada": null,
    "reservaId": 1,
    "usuarioId": 1,
    "operadorEntradaId": 2,
    "estacionamientoId": 1,
    "cajonId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-25T10:10:00"
  }
}
```

#### Efectos de negocio

Después de una respuesta `201 Created`:

- La reserva queda en estado `USADA`.
- El cajón queda en estado `OCUPADO`.
- El ticket queda en estado `ABIERTO`.

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos en la solicitud |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin rol permitido para operar tickets |
| `404` | Usuario autenticado o reserva inexistente/inactiva |
| `409` | Reserva no está en `CREADA`, reserva vencida, usuario sin alcance sobre el estacionamiento, reserva ya convertida o cajón con ticket abierto |

### Registrar salida

```http
PATCH /api/v1/tickets/{ticketId}/salida
```

Requiere rol `ADMIN`, `OWNER` u `OPERADOR`.

#### Response 200

```json
{
  "timestamp": "2026-07-25T11:00:00",
  "status": 200,
  "message": "Salida registrada correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": {
    "id": 1,
    "codigo": "TCK-A1B2C3D4",
    "estado": "PENDIENTE_PAGO",
    "placa": "ABC123",
    "fechaEntrada": "2026-07-25T10:10:00",
    "fechaSalida": "2026-07-25T11:00:00",
    "minutosEstancia": 50,
    "montoTotal": 25.00,
    "precioPorHoraAplicado": 25.00,
    "minutosToleranciaAplicados": 10,
    "cobrarFraccionAplicado": true,
    "tarifaMinimaAplicada": 15.00,
    "reservaId": 1,
    "usuarioId": 1,
    "operadorEntradaId": 2,
    "estacionamientoId": 1,
    "cajonId": 1,
    "activo": true,
    "fechaCreacion": "2026-07-25T10:10:00"
  }
}
```

#### Efectos de negocio

Después de una respuesta `200 OK`:

- El ticket queda en estado `PENDIENTE_PAGO`.
- El ticket tiene `fechaSalida`.
- El ticket tiene `minutosEstancia`, `montoTotal` y una copia de los parámetros de tarifa aplicados.
- El cajón permanece en estado `OCUPADO`.
- El cajón se liberará hasta que el pago sea registrado en el módulo Pago.

#### Errores de salida

| HTTP | Causa |
|---|---|
| `401` | JWT ausente o inválido |
| `403` | Usuario sin rol permitido para operar tickets |
| `404` | Usuario autenticado, ticket inexistente/inactivo o tarifa activa inexistente para el estacionamiento |
| `409` | Ticket distinto de `ABIERTO` o usuario sin alcance sobre el estacionamiento |

## Módulo Tarifa

El módulo Tarifa permite configurar reglas de cobro por estacionamiento.

Estado actual:

- Existe la tabla `tarifa_estacionamiento` mediante Flyway.
- Existe la entidad `TarifaEstacionamiento`.
- Existen los DTOs `TarifaEstacionamientoRequest` y `TarifaEstacionamientoResponse`.
- Existe `TarifaEstacionamientoRepository`.
- Existe `TarifaEstacionamientoMapper`.
- Existe `TarifaEstacionamientoService` y `TarifaEstacionamientoServiceImpl`.
- Existe `TarifaEstacionamientoController`.
- Existen pruebas unitarias de mapper, servicio y controlador.
- Existe prueba de integración `TarifaEstacionamientoIntegrationTest`.

Seguridad:

- Cada estacionamiento puede tener como máximo una tarifa activa.
- `ADMIN` puede administrar tarifas de cualquier estacionamiento.
- `OWNER` solo puede administrar tarifas de estacionamientos propios.
- `OPERADOR` y `USER` no administran tarifas.
- La eliminación de tarifa es lógica mediante `activo=false`.
- La tarifa activa se usa para calcular el cobro al registrar salida de ticket.
- La tarifa mínima se cobra desde el primer minuto de estancia.

### Consultar tarifa por estacionamiento

```http
GET /api/v1/tarifas/estacionamiento/{estacionamientoId}
```

Requiere rol `ADMIN` u `OWNER`.

#### Response 200

```json
{
  "timestamp": "2026-08-01T15:30:00",
  "status": 200,
  "message": "Tarifa consultada correctamente",
  "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
  "data": {
    "id": 1,
    "estacionamientoId": 1,
    "precioPorHora": 25.00,
    "minutosTolerancia": 10,
    "cobrarFraccion": true,
    "tarifaMinima": 15.00,
    "activo": true,
    "fechaCreacion": "2026-08-01T15:25:00"
  }
}
```

### Crear tarifa

```http
POST /api/v1/tarifas
```

Requiere rol `ADMIN` u `OWNER`.

#### Request

```json
{
  "estacionamientoId": 1,
  "precioPorHora": 25.00,
  "minutosTolerancia": 10,
  "cobrarFraccion": true,
  "tarifaMinima": 15.00
}
```

#### Response 201

```json
{
  "timestamp": "2026-08-01T15:30:00",
  "status": 201,
  "message": "Tarifa creada correctamente",
  "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
  "data": {
    "id": 1,
    "estacionamientoId": 1,
    "precioPorHora": 25.00,
    "minutosTolerancia": 10,
    "cobrarFraccion": true,
    "tarifaMinima": 15.00,
    "activo": true,
    "fechaCreacion": "2026-08-01T15:25:00"
  }
}
```

### Actualizar tarifa

```http
PUT /api/v1/tarifas/estacionamiento/{estacionamientoId}
```

Requiere rol `ADMIN` u `OWNER`.

Usa el mismo cuerpo de creación. El `estacionamientoId` de la ruta debe coincidir con el `estacionamientoId` del body.

#### Response 200

```json
{
  "timestamp": "2026-08-01T15:35:00",
  "status": 200,
  "message": "Tarifa actualizada correctamente",
  "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
  "data": {
    "id": 1,
    "estacionamientoId": 1,
    "precioPorHora": 30.00,
    "minutosTolerancia": 15,
    "cobrarFraccion": false,
    "tarifaMinima": 20.00,
    "activo": true,
    "fechaCreacion": "2026-08-01T15:25:00"
  }
}
```

### Eliminar tarifa lógicamente

```http
DELETE /api/v1/tarifas/estacionamiento/{estacionamientoId}
```

Requiere rol `ADMIN` u `OWNER`.

#### Response 204

Sin cuerpo de respuesta.

### Errores esperados

| Código | Motivo |
|---|---|
| `400` | Datos inválidos en la solicitud |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin rol permitido para administrar tarifas |
| `404` | Estacionamiento o tarifa inexistente/inactiva; para `OWNER`, estacionamiento fuera de su alcance |
| `409` | Ya existe tarifa activa para el estacionamiento o el `estacionamientoId` del path no coincide con el body |

- El registro de salida de ticket usa la tarifa activa del estacionamiento para calcular el cobro y guardar los parámetros aplicados en el ticket.

## Módulo Pago

El módulo Pago permite registrar la liquidación de tickets que ya tienen salida registrada y están en estado `PENDIENTE_PAGO`.

Estado actual:

- Existe la tabla `pago` mediante Flyway.
- Existe la entidad `Pago`.
- Existen los enums `MetodoPago` y `EstadoPago`.
- Existen los DTOs `PagoRequest` y `PagoResponse`.
- Existe `PagoRepository`.
- Existe `PagoMapper`.
- Existe `PagoService` y `PagoServiceImpl`.
- Existe `PagoController`.
- Existen pruebas unitarias de mapper, servicio y controlador.
- Existe prueba de integración `PagoIntegrationTest`.

Reglas:

- Solo se pueden pagar tickets activos en estado `PENDIENTE_PAGO`.
- El monto total se toma desde `ticket.montoTotal`; no se recibe desde el frontend.
- `montoRecibido` debe ser mayor o igual a `montoTotal`.
- `cambio` se calcula en backend como `montoRecibido - montoTotal`.
- Un ticket solo puede tener un pago activo.
- Al registrar el pago, el pago queda `REGISTRADO`, el ticket pasa a `CERRADO` y el cajón pasa a `LIBRE`.
- `ADMIN` puede registrar pagos de cualquier estacionamiento.
- `OWNER` solo puede registrar pagos de estacionamientos propios.
- `OPERADOR` solo puede registrar pagos de estacionamientos asignados.
- `USER` no registra pagos, pero puede consultar pagos de sus propios tickets.
- El listado general de pagos solo está disponible para `ADMIN`, `OWNER` y `OPERADOR`.

### Listar pagos

```http
GET /api/v1/pagos?page=0&size=10&sort=fechaPago,desc
```

Requiere rol `ADMIN`, `OWNER` u `OPERADOR`.

Filtros opcionales:

```http
GET /api/v1/pagos?estacionamientoId=1
GET /api/v1/pagos?metodoPago=EFECTIVO
GET /api/v1/pagos?fechaInicio=2026-08-01&fechaFin=2026-08-31
GET /api/v1/pagos?estacionamientoId=1&metodoPago=EFECTIVO&fechaInicio=2026-08-01&fechaFin=2026-08-31&page=0&size=10&sort=fechaPago,desc
```

Parámetros:

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `estacionamientoId` | `Long` | No | Filtra pagos de un estacionamiento específico |
| `metodoPago` | `EFECTIVO`, `TARJETA` o `TRANSFERENCIA` | No | Filtra pagos por método de pago |
| `fechaInicio` | `yyyy-MM-dd` | No | Filtra pagos registrados desde el inicio de esa fecha |
| `fechaFin` | `yyyy-MM-dd` | No | Filtra pagos registrados hasta el final de esa fecha |
| `page` | `Integer` | No | Página solicitada, iniciando en `0` |
| `size` | `Integer` | No | Tamaño de página |
| `sort` | `String` | No | Ordenamiento Spring Data, por ejemplo `fechaPago,desc` |

Permisos:

- `ADMIN`: ve todos los pagos activos.
- `OWNER`: ve pagos de estacionamientos propios.
- `OPERADOR`: ve pagos de estacionamientos asignados.
- `USER`: no puede usar el listado general; debe consultar sus pagos por ticket.

Los filtros no amplían permisos. Si un usuario filtra por un estacionamiento fuera de su alcance, la respuesta queda limitada a los pagos que realmente puede ver.

#### Response 200

```json
{
  "timestamp": "2026-08-01T18:25:00",
  "status": 200,
  "message": "Pagos consultados correctamente",
  "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
  "data": {
    "content": [
      {
        "id": 1,
        "ticketId": 1,
        "codigoTicket": "TCK-ABC12345",
        "montoTotal": 15.00,
        "montoRecibido": 100.00,
        "cambio": 85.00,
        "metodoPago": "EFECTIVO",
        "estado": "REGISTRADO",
        "fechaPago": "2026-08-01T18:20:00",
        "operadorId": 9,
        "activo": true,
        "fechaCreacion": "2026-08-01T18:20:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

### Registrar pago

```http
POST /api/v1/pagos
```

Requiere rol `ADMIN`, `OWNER` u `OPERADOR`.

#### Request

```json
{
  "ticketId": 1,
  "montoRecibido": 100.00,
  "metodoPago": "EFECTIVO"
}
```

Métodos de pago disponibles:

```text
EFECTIVO
TARJETA
TRANSFERENCIA
```

#### Response 201

```json
{
  "timestamp": "2026-08-01T18:20:00",
  "status": 201,
  "message": "Pago registrado correctamente",
  "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
  "data": {
    "id": 1,
    "ticketId": 1,
    "codigoTicket": "TCK-ABC12345",
    "montoTotal": 15.00,
    "montoRecibido": 100.00,
    "cambio": 85.00,
    "metodoPago": "EFECTIVO",
    "estado": "REGISTRADO",
    "fechaPago": "2026-08-01T18:20:00",
    "operadorId": 9,
    "activo": true,
    "fechaCreacion": "2026-08-01T18:20:00"
  }
}
```

#### Efectos de negocio

- Crea un registro en `pago`.
- Cambia el ticket de `PENDIENTE_PAGO` a `CERRADO`.
- Cambia el cajón de `OCUPADO` a `LIBRE`.
- Devuelve el cambio que el cajero debe entregar al cliente.

### Consultar pago por ticket

```http
GET /api/v1/pagos/ticket/{ticketId}
```

Requiere JWT válido.

Alcance:

- `ADMIN`: consulta cualquier pago.
- `OWNER`: consulta pagos de tickets de sus estacionamientos.
- `OPERADOR`: consulta pagos de tickets de estacionamientos asignados.
- `USER`: consulta pagos de sus propios tickets.

#### Response 200

```json
{
  "timestamp": "2026-08-01T18:25:00",
  "status": 200,
  "message": "Pago consultado correctamente",
  "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
  "data": {
    "id": 1,
    "ticketId": 1,
    "codigoTicket": "TCK-ABC12345",
    "montoTotal": 15.00,
    "montoRecibido": 100.00,
    "cambio": 85.00,
    "metodoPago": "EFECTIVO",
    "estado": "REGISTRADO",
    "fechaPago": "2026-08-01T18:20:00",
    "operadorId": 9,
    "activo": true,
    "fechaCreacion": "2026-08-01T18:20:00"
  }
}
```

### Errores esperados

| Código | Motivo |
|---|---|
| `400` | Datos inválidos en la solicitud |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin rol permitido para listar, registrar o consultar pagos |
| `404` | Usuario autenticado, ticket o pago inexistente/inactivo |
| `409` | Ticket distinto de `PENDIENTE_PAGO`, pago duplicado, monto insuficiente, rango de fechas inválido o usuario sin alcance |

## Módulo Catálogos

Seguridad:

- Requiere JWT válido.
- Permite los roles `ADMIN`, `OPERADOR` y `USER`.
- No modifica datos; solo expone valores técnicos existentes en enums del backend.

Los catálogos permiten que el frontend construya listas desplegables sin quemar valores técnicos en el código cliente. Actualmente se generan desde los enums `TipoCajon` y `EstadoCajon`, por lo que no requieren migración Flyway ni tablas adicionales.

### Consultar tipos de cajón

```http
GET /api/v1/catalogos/cajones/tipos
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Tipos de cajon consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": [
    {
      "codigo": "AUTO",
      "descripcion": "Auto"
    },
    {
      "codigo": "MOTO",
      "descripcion": "Moto"
    },
    {
      "codigo": "DISCAPACITADO",
      "descripcion": "Discapacitado"
    },
    {
      "codigo": "ELECTRICO",
      "descripcion": "Electrico"
    }
  ]
}
```

### Consultar estados de cajón

```http
GET /api/v1/catalogos/cajones/estados
```

#### Response 200

```json
{
  "timestamp": "2026-07-18T10:00:00",
  "status": 200,
  "message": "Estados de cajon consultados correctamente",
  "transactionId": "0f5d5c9b-8dc1-4bd1-a173-08f16eb4f96e",
  "data": [
    {
      "codigo": "LIBRE",
      "descripcion": "Libre"
    },
    {
      "codigo": "RESERVADO",
      "descripcion": "Reservado"
    },
    {
      "codigo": "OCUPADO",
      "descripcion": "Ocupado"
    },
    {
      "codigo": "FUERA_SERVICIO",
      "descripcion": "Fuera de servicio"
    }
  ]
}
```

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `401` | JWT ausente o inválido |
| `403` | Usuario sin permisos para consultar catálogos |

## Pruebas automatizadas relacionadas

El backend cuenta con pruebas unitarias de mapper, servicio y controlador para Rol, Estacionamiento, Cajón, Usuario, Reserva, Ticket, Tarifa y Pago, prueba unitaria del scheduler de Reserva, prueba unitaria del cálculo de cobro de Ticket, además de pruebas unitarias de servicio y controlador para Catálogos.

`SecurityConfigTest` cubre reglas de seguridad HTTP, autorización por roles, autenticación JWT simulada, validaciones CORS y acceso protegido a Catálogos. Las pruebas CORS validan preflight `OPTIONS` desde orígenes permitidos, rechazo de orígenes no configurados y exposición de `X-Transaction-Id` para consumo desde frontend.

`HealthCheckSecurityIntegrationTest` valida que `/actuator/health`, `/actuator/health/liveness` y `/actuator/health/readiness` puedan consultarse sin JWT y respondan estado `UP`.

También existen pruebas de integración con Spring Boot completo, PostgreSQL y perfil `test`:

- `AuthUsuarioIntegrationTest`.
- `RolIntegrationTest`.
- `EstacionamientoIntegrationTest`.
- `CajonIntegrationTest`.
- `ReservaIntegrationTest`.
- `TicketIntegrationTest`.
- `PagoIntegrationTest`.
- `UsuarioIntegrationTest`.
- `CatalogoIntegrationTest`.

Estas pruebas validan que la conexión use `parkio_test` antes de limpiar datos de prueba.

`AuthUsuarioIntegrationTest` cubre registro público, login con JWT, consulta de endpoint protegido, rechazo de `/api/v1/auth/me` sin token y consulta exitosa de `/api/v1/auth/me` con un JWT real emitido por el backend.

`UsuarioIntegrationTest` cubre creación pública con rol base `USER`, conflictos por correo duplicado, permisos sobre usuario propio, bloqueo de acceso a usuarios ajenos, cambio de contraseña, administración de roles y estacionamientos por `ADMIN`, borrado lógico y rechazo de login para usuarios inactivos.

`EstacionamientoIntegrationTest` cubre rechazo sin JWT, consulta con `USER`, administración global con `ADMIN`, borrado lógico con desactivación de cajones asociados, alcance de `OWNER` para crear, listar, consultar, actualizar y eliminar lógicamente únicamente sus propios estacionamientos, y alcance de `OPERADOR` para consultar solo estacionamientos asignados mediante `usuario_estacionamiento`. También valida que `owner_id` se asigne desde el JWT y que un `OWNER` no afecte estacionamientos ni cajones de otro `OWNER`.

`CajonIntegrationTest` cubre rechazo sin JWT, consulta con `USER`, cambio de estado con `OPERADOR`, administración global con `ADMIN`, conflictos por número duplicado, borrado lógico, alcance de `OWNER` para operar únicamente cajones ubicados en sus propios estacionamientos y alcance de `OPERADOR` para consultar y cambiar estado solo en cajones de estacionamientos asignados.

`ReservaIntegrationTest` cubre rechazo sin JWT, creación de reserva con `USER`, cambio del cajón a `RESERVADO`, bloqueo de doble reserva sobre el mismo cajón, consulta de reservas propias, consulta por código con `OPERADOR`, consulta por identificador interno con `ADMIN`, cancelación manual de reservas propias y expiración de reservas vencidas con liberación del cajón.

`TicketIntegrationTest` cubre rechazo sin JWT, rechazo con rol `USER`, creación de ticket con `OPERADOR` asignado al estacionamiento, creación con `ADMIN`, creación con `OWNER` sobre estacionamiento propio, consulta paginada de tickets, filtros por `estado` y `estacionamientoId`, consulta por identificador, cambio de reserva a `USADA`, cambio de cajón a `OCUPADO`, registro de salida con cambio a `PENDIENTE_PAGO`, cálculo de cobro con tarifa activa, persistencia de monto total y parámetros aplicados, cajón aún `OCUPADO`, bloqueo de doble ticket para la misma reserva y rechazo de operador no asignado al estacionamiento.

`PagoIntegrationTest` cubre rechazo sin JWT, registro de pago de un ticket en estado `PENDIENTE_PAGO`, cálculo de cambio, cambio del ticket a `CERRADO`, liberación del cajón a `LIBRE`, consulta del pago por ticket, listado paginado con filtros y bloqueo del listado general para `USER`.

`CatalogoIntegrationTest` cubre rechazo sin JWT, acceso con roles `ADMIN`, `OPERADOR` y `USER`, formato `ApiResponse`, presencia de `transactionId` y valores reales de los catálogos de tipos y estados de Cajón derivados de los enums `TipoCajon` y `EstadoCajon`.

## Códigos HTTP utilizados

| Código | Descripción |
|---|---|
| `200` | Operación exitosa con cuerpo |
| `201` | Recurso creado |
| `204` | Operación exitosa sin cuerpo |
| `400` | Datos inválidos |
| `401` | No autenticado |
| `403` | Sin permisos |
| `404` | Recurso no encontrado |
| `409` | Conflicto |
| `500` | Error interno |

