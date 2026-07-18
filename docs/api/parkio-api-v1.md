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

En ambiente de desarrollo:

```http
GET /api/v1/swagger-ui.html
GET /api/v1/v3/api-docs
```

`/api/v1/swagger-ui.html` abre la interfaz visual para consultar y probar la API. `/api/v1/v3/api-docs` expone el contrato OpenAPI en formato JSON.

Swagger UI se genera a partir de los controladores reales. Los endpoints de negocio conservan la base global:

```text
/api/v1
```

Ejemplo:

```http
POST /api/v1/auth/login
GET /api/v1/roles
GET /api/v1/estacionamientos
GET /api/v1/cajones
GET /api/v1/usuarios
```

Para probar endpoints protegidos desde Swagger UI se debe usar el botón `Authorize` y proporcionar un JWT con el esquema Bearer.

Springdoc está deshabilitado por defecto y también en el perfil `prod`. Actualmente se habilita desde el perfil `dev`.

### Autorización por roles

| Módulo | Consulta | Escritura / Administración |
|---|---|---|
| Auth | Público para login | No aplica |
| Usuario | `ADMIN`; o propio usuario para `USER`/`OPERADOR` en endpoints permitidos | `ADMIN`; o propio usuario para actualización/cambio de contraseña |
| Rol | `ADMIN` | `ADMIN` |
| Estacionamiento | `ADMIN`, `OPERADOR`, `USER` | `ADMIN` |
| Cajón | `ADMIN`, `OPERADOR`, `USER` | `ADMIN`; cambio de estado también permite `OPERADOR` |

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
- `GET /api/v1/usuarios`, `DELETE /api/v1/usuarios/{usuarioId}` y asignaciones/retiros requieren `ADMIN`.
- `GET /api/v1/usuarios/{usuarioId}`, `PUT /api/v1/usuarios/{usuarioId}` y `PATCH /api/v1/usuarios/{usuarioId}/password` permiten `ADMIN`, o `USER`/`OPERADOR` cuando el `usuarioId` de la ruta coincide con el claim `usuarioId`.

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

#### Response 204

Sin cuerpo. Realiza borrado lógico.

### Asignar rol a usuario

```http
POST /api/v1/usuarios/{usuarioId}/roles
```

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

#### Response 204

Sin cuerpo.

### Asignar estacionamiento a usuario

```http
POST /api/v1/usuarios/{usuarioId}/estacionamientos
```

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
- `GET /api/v1/estacionamientos` y `GET /api/v1/estacionamientos/{estacionamientoId}` permiten `ADMIN`, `OPERADOR` y `USER`.
- `POST`, `PUT` y `DELETE` requieren `ADMIN`.

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
- `GET /api/v1/cajones`, `GET /api/v1/cajones?estacionamientoId={id}` y `GET /api/v1/cajones/{cajonId}` permiten `ADMIN`, `OPERADOR` y `USER`.
- `PATCH /api/v1/cajones/{cajonId}/estado` permite `ADMIN` y `OPERADOR`.
- `POST`, `PUT` y `DELETE` requieren `ADMIN`.

Tipos permitidos:

- `AUTO`
- `MOTO`
- `DISCAPACITADO`
- `ELECTRICO`

Estados permitidos:

- `LIBRE`
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

Usa el mismo cuerpo de creación. La actualización conserva el estado operativo actual del cajón.

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

Sin cuerpo. Realiza borrado lógico.

#### Errores del módulo

| HTTP | Causa |
|---|---|
| `400` | Datos inválidos, tipo inválido o estado inválido |
| `401` | JWT ausente o inválido |
| `403` | Usuario sin permisos para la operación |
| `404` | Cajón o estacionamiento inexistente/inactivo |
| `409` | Número duplicado dentro del estacionamiento |

## Pruebas automatizadas relacionadas

El backend cuenta con pruebas unitarias de mapper, servicio y controlador para Rol, Estacionamiento, Cajón y Usuario.

`SecurityConfigTest` cubre reglas de seguridad HTTP, autorización por roles, autenticación JWT simulada y validaciones CORS. Las pruebas CORS validan preflight `OPTIONS` desde orígenes permitidos, rechazo de orígenes no configurados y exposición de `X-Transaction-Id` para consumo desde frontend.

`HealthCheckSecurityIntegrationTest` valida que `/actuator/health`, `/actuator/health/liveness` y `/actuator/health/readiness` puedan consultarse sin JWT y respondan estado `UP`.

También existen pruebas de integración con Spring Boot completo, PostgreSQL y perfil `test`:

- `AuthUsuarioIntegrationTest`.
- `RolIntegrationTest`.
- `EstacionamientoIntegrationTest`.
- `CajonIntegrationTest`.
- `UsuarioIntegrationTest`.

Estas pruebas validan que la conexión use `parkio_test` antes de limpiar datos de prueba.

`UsuarioIntegrationTest` cubre creación pública con rol base `USER`, conflictos por correo duplicado, permisos sobre usuario propio, bloqueo de acceso a usuarios ajenos, cambio de contraseña, administración de roles y estacionamientos por `ADMIN`, borrado lógico y rechazo de login para usuarios inactivos.

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

