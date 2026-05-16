# Dynamic REST Engine - Agent Continuity Guide

This document defines the constraints, philosophies, and extension points for AI agents extending this repository.

## 1. Abstract, Pluggable Architecture Constraints
- **DynamicPayload**: Always use the `DynamicPayload` record to transport payloads across boundaries. Do NOT create concrete POJOs or `@Entity` definitions.
- **Storage Boundaries**: Any database or storage layer MUST implement `DynamicStorageRepository`. Never expose database-specific semantics (like `JdbcTemplate` or ORM features) out of the repository layer.
- **Auto-Provisioning**: Table schema provisioning is simple (`id BIGSERIAL PRIMARY KEY`, `payload JSONB NOT NULL`, `created_at`, `updated_at`). For future extensions, adhere to isolated schemas per `entity_type`.

## 2. Dynamic REST Layer
- **No Static Controllers**: Do not add standard `@RestController` annotated classes for new CRUD components.
- **Registration**: All dynamic routes must be registered/unregistered via the Spring `RequestMappingHandlerMapping` bean using the logic present in `DynamicRestHandler`.
- **Operations Supported**: Currently scoped to `1:1 By ID` strictly. When adding global operations (e.g., list views `GET {basePath}` or filters), you must ensure they remain dynamic and do not violate the core generic principles.

## 3. Configuration & State
- **ZooKeeper Dependency**: The configuration state is externalized in Apache ZooKeeper (via Apache Curator). Do not hardcode routes in `application.properties` or standard Java config classes.

## 4. Code Quality Expectations
- Keep testing coverage > 80% with Jacoco.
- Use TestContainers for deep integration testing across external dependencies (e.g. databases, ZooKeeper).
- Any newly implemented protocol extensions (like gRPC) should follow similar patterns, parsing ZK nodes and standing up active listeners.
