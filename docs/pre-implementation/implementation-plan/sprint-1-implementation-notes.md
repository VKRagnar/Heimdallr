# Sprint 1 Implementation Notes

## 1. Database Scope

Sprint 1 migration files are under `backend/db-migration/` and follow Flyway naming:

- `V001__sprint1_foundation.sql`
- `V002__sprint1_seed_data.sql`

Implemented batches:

| Batch | Tables |
| --- | --- |
| M01 | `dictionary_item`, `business_line`, `department`, `user_account` |
| M02 | `application`, `application_owner`, `server`, `application_instance`, `app_server_binding` |
| M05 | `role`, `permission`, `user_role`, `role_permission`, `application_authorization`, `business_line_authorization`, `sensitive_access_grant` |
| M08 | `mask_policy`, `mask_rule`, `export_task`, `audit_event`, `external_event_outbox`, `retention_policy` |

Design choices:

- Core tables use `uuid` primary keys, `timestamptz` timestamps, `jsonb` extension fields, and `created_at` / `updated_at` / `deleted_at`.
- PostgreSQL `pgcrypto` is enabled for `gen_random_uuid()`.
- `updated_at` is maintained through a shared trigger function.
- Cross-business-line tables carry `business_line_id`; environment-scoped assets carry `env`.
- M03 dependency and data-source tables are not created in this sprint note because the requested scope is M01, M02, M05 and M08.

## 2. Seed Data

`V002__sprint1_seed_data.sql` initializes:

- Dictionary values for environments, record status, business health, access status, agent status, export status and audit result.
- Default users: `admin`, `sre001`, `user001`, `user002`, `user201`, `user301`.
- Default business lines: `logistics`, `commerce`, `content`, `shared`.
- ACE, IPRO and CMS prod applications.
- Four prod servers and application instances.
- Default roles: `platform_admin`, `sre`, `app_owner`, `viewer`.
- Default permissions for assets, permissions, audit, export and sensitive access.
- Application and business-line grants.
- Default masking policies and masking rules.
- Sample sensitive access grant, export task and audit events.

The seed script is versioned instead of repeatable so a normal Flyway run applies it once per database.

## 3. Local Dependencies

Local PostgreSQL and Redis are defined in `deploy/docker-compose.yml`.

Start dependencies:

```bash
cd deploy
cp .env.example .env
docker compose up -d
```

Default connection values:

| Item | Value |
| --- | --- |
| PostgreSQL host | `localhost` |
| PostgreSQL port | `5432` |
| PostgreSQL database | `heimdallr_monitor` |
| PostgreSQL user | `monitor` |
| PostgreSQL password | `monitor_pass` |
| Redis host | `localhost` |
| Redis port | `6379` |

Health checks use `pg_isready` and `redis-cli ping`.

## 4. Migration Application

Backend build and migration integration should follow the Maven multi-module project. Suggested Flyway properties for the backend module:

```properties
spring.flyway.enabled=true
spring.flyway.locations=filesystem:backend/db-migration
spring.datasource.url=jdbc:postgresql://localhost:5432/heimdallr_monitor
spring.datasource.username=monitor
spring.datasource.password=monitor_pass
```

Manual validation with the Flyway CLI can use the same migration folder:

```bash
flyway -url=jdbc:postgresql://localhost:5432/heimdallr_monitor -user=monitor -password=monitor_pass -locations=filesystem:backend/db-migration migrate
```

## 5. Import Samples

Import samples live under `scripts/import-samples/`:

- `applications.csv`: application and instance inventory for ACE, IPRO and CMS.
- `servers.csv`: server inventory with agent and access fields.
- `dependencies.json`: Kafka and PostgreSQL dependency samples reserved for later M03 import wiring.

Application CSV import maps to M01/M02/M05 tables by:

- `business_line_code` -> `business_line.code`
- `department_code` -> `department.code`
- `primary_owner` / `backup_owner` -> `user_account.username`
- `application_code` + `env` -> `application.code` + `application.env`
- `instance_ip` + `instance_port` + `server_hostname` -> `application_instance`
- `server_hostname` -> `server.hostname`

Server CSV import maps to:

- `business_line_code` -> `business_line.code`
- `owner_username` -> `user_account.username`
- `deployed_app_codes` -> `application.code`, then `app_server_binding`
- Agent and access fields -> `server.agent_*`, `server.access_status`, `server.business_health_status`

Dependency JSON is intentionally not inserted by V002 because M03 tables are out of this implementation slice.

## 6. To Confirm

- Whether user identity will stay `username` based for imports or switch to employee number / IAM subject.
- Final enum codes for application type, server type, owner role and authorization access level.
- Whether `audit_event`, `export_task` and other high-volume tables should be partitioned in Sprint 1 or deferred.
- Whether IP addresses should remain PostgreSQL `inet` columns or be normalized to text for cross-database portability.
- Exact Maven module that will own Flyway configuration once the backend skeleton is finalized.
