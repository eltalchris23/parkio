# Parkio API v1

## Información General

### Base URL

```text
/api
```

### Autenticación

La autenticación JWT está planificada, pero todavía no está implementada. Los endpoints actuales de Rol no requieren token y no deben considerarse seguros para un entorno productivo.

### Estado de implementación

| Módulo | Estado |
|---|---|
| Rol | CRUD REST implementado |
| Auth | Propuesto; no implementado |
| Usuario | Propuesto; no implementado |
| Estacionamiento | Propuesto; no implementado |
| Cajón | Propuesto; no implementado |

El siguiente encabezado representa el formato de autenticación previsto para el futuro:

Ejemplo:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

# Módulo Auth

## Login

### Endpoint

```http
POST /api/auth/login
```

### Request

```json
{
  "email": "admin@parkio.com",
  "password": "123456"
}
```

### Response 200

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Response 401

```json
{
  "message": "Credenciales inválidas"
}
```

---

# Módulo Rol

## Listar Roles

```http
GET /api/roles
```

### Response 200

```json
[
  {
    "id": 1,
    "nombre": "ADMIN",
    "activo": true,
    "fechaCreacion": "2026-06-20T12:00:00"
  }
]
```

## Consultar Rol

```http
GET /api/roles/{rolId}
```

### Response 200

```json
{
  "id": 1,
  "nombre": "ADMIN",
  "activo": true,
  "fechaCreacion": "2026-06-20T12:00:00"
}
```

### Response 404

Se devuelve cuando el identificador no corresponde a un rol existente.

## Crear Rol

```http
POST /api/roles
```

### Request

```json
{
  "nombre": "ADMIN",
  "activo": true
}
```

Validaciones:

- `nombre` es obligatorio y admite hasta 50 caracteres.
- `activo` es obligatorio.
- El nombre no puede estar duplicado.

### Response 201

```json
{
  "id": 1,
  "nombre": "ADMIN",
  "activo": true,
  "fechaCreacion": "2026-06-20T12:00:00"
}
```

## Actualizar Rol

```http
PUT /api/roles/{rolId}
```

Utiliza el mismo cuerpo y las mismas validaciones de la creación.

### Response 200

Devuelve el rol actualizado.

## Eliminar Rol

```http
DELETE /api/roles/{rolId}
```

### Response 204

La eliminación actual es física y la respuesta no contiene cuerpo.

---

# Módulo Usuario

## Crear Usuario

### Endpoint

```http
POST /api/usuarios
```

### Request

```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com",
  "password": "123456"
}
```

### Response 201

```json
{
  "id": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com"
}
```

---

## Consultar Usuario

### Endpoint

```http
GET /api/usuarios/{id}
```

### Response 200

```json
{
  "id": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com"
}
```

---

# Módulo Estacionamiento

## Crear Estacionamiento

### Endpoint

```http
POST /api/estacionamientos
```

### Request

```json
{
  "nombre": "Parkio Centro",
  "descripcion": "Sucursal Centro Histórico",
  "latitud": 19.432608,
  "longitud": -99.133209
}
```

### Response 201

```json
{
  "id": 1,
  "nombre": "Parkio Centro",
  "descripcion": "Sucursal Centro Histórico",
  "latitud": 19.432608,
  "longitud": -99.133209
}
```

---

## Listar Estacionamientos

### Endpoint

```http
GET /api/estacionamientos
```

### Response 200

```json
[
  {
    "id": 1,
    "nombre": "Parkio Centro"
  },
  {
    "id": 2,
    "nombre": "Parkio Reforma"
  }
]
```

---

# Módulo Cajón

## Crear Cajón

### Endpoint

```http
POST /api/cajones
```

### Request

```json
{
  "estacionamientoId": 1,
  "numero": "A-001",
  "tipo": "AUTO"
}
```

### Response 201

```json
{
  "id": 1,
  "estacionamientoId": 1,
  "numero": "A-001",
  "tipo": "AUTO",
  "estado": "LIBRE"
}
```

---

## Consultar Cajones

### Endpoint

```http
GET /api/cajones?estacionamientoId=1
```

### Response 200

```json
[
  {
    "id": 1,
    "numero": "A-001",
    "tipo": "AUTO",
    "estado": "LIBRE"
  }
]
```

---

# Códigos HTTP utilizados

| Código | Descripción           |
| ------ | --------------------- |
| 200    | Operación exitosa     |
| 201    | Recurso creado        |
| 204    | Operación exitosa sin cuerpo |
| 400    | Datos inválidos       |
| 401    | No autenticado        |
| 403    | Sin permisos          |
| 404    | Recurso no encontrado |
| 409    | Conflicto             |
| 500    | Error interno         |

## Formato de Error Implementado

Las operaciones del módulo Rol utilizan el siguiente formato:

```json
{
  "timestamp": "2026-06-20T16:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos inválidos",
  "path": "/api/roles",
  "validationErrors": {
    "nombre": "El nombre del rol es obligatorio"
  }
}
```
