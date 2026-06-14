# Parkio API v1

## Información General

### Base URL

```text
/api
```

### Autenticación

La API utiliza JWT.

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
| 400    | Datos inválidos       |
| 401    | No autenticado        |
| 403    | Sin permisos          |
| 404    | Recurso no encontrado |
| 409    | Conflicto             |
| 500    | Error interno         |

```
```
