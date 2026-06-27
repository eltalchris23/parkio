# Documentación Parkio

## ERD
- `erd/parkio-erd.puml`

## UML
- `uml/parkio-domain.puml`
- `uml/parkio-use-cases.puml`

## Arquitectura
- `architecture/spring-boot-architecture.puml`
- `architecture/parkio-package-structure.puml`
- `architecture/parkio-jwt-flow.puml`

## Secuencia
- `sequence/parkio-login-sequence.puml`
- `sequence/parkio-create-estacionamiento-sequence.puml`
- `sequence/parkio-create-cajon-sequence.puml`

## API
- `api/parkio-api-v1.md`

## Casos de uso
- `use-cases/mvp-use-cases.md`

## Estado actual

Los módulos Rol, Estacionamiento y Cajón cuentan con API REST, servicios transaccionales, mappers y pruebas unitarias. Rol y Estacionamiento tienen validaciones declarativas; `CajonRequest` todavía no. El manejo global de excepciones está implementado. Los flujos de autenticación y Usuario continúan como arquitectura objetivo.
