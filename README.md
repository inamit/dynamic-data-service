# Dynamic REST Engine

A modular, configuration-driven REST API engine bootstrapped using Java 21, Spring Boot 3.x, and PostgreSQL.
This engine uses Apache Curator to dynamically read configuration from ZooKeeper and expose REST endpoints based on the payload configurations on the fly.

## Features
- **Generic Data Model**: Data is transferred securely via a generic JSON payload and wrapped securely.
- **Storage Abstraction**: Clean storage interfaces backing Postgres implementation (JSONB).
- **Table Provisioning**: Dynamically issues a `CREATE TABLE IF NOT EXISTS` for unknown entities using isolated tables by type.
- **ZooKeeper Integration**: Reads the configurations via active watch using Apache Curator on `/${APP_ENV}/services/${SERVICE_NAME}`.
- **Dynamic REST Routing**: Strict 1:1 CRUD operations based on IDs are registered and deregistered in Spring Boot's mapping layer on the fly.

## Setup
Dependencies required to run locally: Docker Desktop, ZooKeeper, and PostgreSQL.

### Development Environment (Docker Compose)
We have provided a Docker compose file to make running it locally very easy.
```bash
docker-compose up -d
```
This spins up PostgreSQL and ZooKeeper instances ready for connections locally.

### ZooKeeper Configuration Structure
To configure the dynamic engine to expose routes, establish a path inside ZooKeeper. The path follows the structure: `/${APP_ENV}/services/${SERVICE_NAME}/entities`.

**Example Path**: `/dev/services/inventory`

The tree structure should be set up as follows:
- `/${env}`
  - `/services`
    - `/${serviceName}`
      - `/entities`
        - `/${entityName}`  <- This node contains the JSON configuration for the entity.

The engine can automatically infer the required properties from the `entityName` (the ZooKeeper node name) if the JSON payload is minimal or empty (e.g., `{}`).

By default:
- `type` is inferred as the node name.
- `basePath` is inferred as `/api/v1/{entityName}`.
- `storageEngine` defaults to `POSTGRES`.

If you need to override these defaults, you can provide an explicit JSON payload. For example, to configure a product entity at `/dev/services/inventory/entities/product` with an overridden `basePath`, use the following schema:

**Payload Schema**:
```json
{
  "basePath": "/api/v1/products"
}
```
If you push this to the ZooKeeper node, the engine will instantly recognize the change and generate a new dynamic handler mapped to `/api/v1/products` respectively for full CRUD capabilities.
