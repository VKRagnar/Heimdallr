# Sprint 1 Import Samples

These files are sample inputs for the backend import APIs and manual seed validation.

## Files

- `applications.csv`: ACE, IPRO and CMS application inventory. Each row represents one application instance in one environment.
- `servers.csv`: server inventory with agent, access and deployed-application fields.
- `dependencies.json`: first-batch Kafka and PostgreSQL dependency samples for later M03 dependency modeling.

## Import Scope

The Sprint 1 database migration covers M01, M02, M05 and M08. Application and server samples map directly to:

- `business_line`
- `department`
- `user_account`
- `application`
- `application_owner`
- `server`
- `application_instance`
- `app_server_binding`
- `application_authorization`
- `audit_event`

Kafka and PostgreSQL dependencies are intentionally kept in JSON for later backend import wiring. They should be imported after M03 tables such as `monitor_object`, `object_app_dependency` and `data_source_binding` are available.

## Backend Build Assumption

Backend integration should use the Maven multi-module project. A future backend module can load these files from `scripts/import-samples/` in tests or expose import endpoints that accept the same field names.
