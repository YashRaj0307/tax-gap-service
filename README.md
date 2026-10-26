# Tax Gap Detection & Compliance Validation Service

Spring Boot REST API backed by PostgreSQL. The project includes taxpayer/income tax-gap analysis APIs and a transaction-level compliance pipeline.

## Technology

- Java 21, Spring Boot 4.1.2, Spring MVC, Spring Security HTTP Basic
- Spring Data JPA / Hibernate
- PostgreSQL 18, Flyway schema migrations
- JUnit 5, Mockito, Spring Boot Test, JaCoCo

## Features

- Taxpayer registration and paginated/sorted retrieval
- Reported-income registration, duplicate source-reference protection, pagination/sorting, idempotency via `Idempotency-Key`
- Tax-gap analysis for taxpayer and tax year
- Batch financial transaction ingestion that stores raw JSON for every record and records per-record validation failures
- Tax computation (`expectedTax = amount × taxRate`; `taxGap = expectedTax − reportedTax`); statuses: `COMPLIANT` when absolute gap is at most 1.00, `UNDERPAID` above 1.00, `OVERPAID` below -1.00, and `NON_COMPLIANT` when reported tax is missing
- Database-driven configurable rules: high-value transactions, refund amount validation against original sale, and GST slab validation
- Persisted exceptions with customer/severity/rule filters
- Audit events for `INGESTION`, `RULE_EXECUTION`, and `TAX_COMPUTATION`
- SQL-backed customer tax and exception summary reports
- Database-backed users; a development user is seeded by Flyway

## Prerequisites

- JDK 21
- PostgreSQL 16+ (tested by the project setup against PostgreSQL 18)
- No Docker is required

## Database setup (Windows CMD)

Create the application and test databases if they do not exist:

```sql
CREATE DATABASE tax_gap_db;
CREATE DATABASE tax_gap_test_db;
```

Run these statements as a PostgreSQL administrator. Update `DB_USERNAME` and `DB_PASSWORD` environment variables if your local PostgreSQL credentials differ. The defaults in `application.properties` are local-development defaults; do not use them in production.

Flyway applies migrations V1 through V7 automatically on application startup. Do not manually create tables that are already managed by Flyway.

```cmd
cd C:\Users\Lenovo\Documents\Project\tax-gap-service
set DB_USERNAME=postgres
set DB_PASSWORD=YOUR_LOCAL_POSTGRES_PASSWORD
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

The service starts on `http://localhost:8080`.

## Authentication

The V7 migration inserts a database user:

- Username: `devuser`
- Password: `LocalDevOnly-ChangeMe-2026`
- Role: `ADMIN` (development-only; required for rule-management endpoints)

The password is stored as a BCrypt hash. This is a development-only account; change the database credential before sharing or deploying the application. All `/api/**` routes require HTTP Basic authentication. Health is public at `/actuator/health`.

## Batch ingestion

Endpoint: `POST /api/financial-transactions/batch`

Windows CMD example:

```cmd
curl.exe -i -u devuser:LocalDevOnly-ChangeMe-2026 -X POST http://localhost:8080/api/financial-transactions/batch -H "Content-Type: application/json" --data-binary @samples/transactions.json
```

Example payload is in [`samples/transactions.json`](samples/transactions.json). Each record is handled independently: invalid records are persisted with `validationStatus=FAILURE`, the original JSON is preserved in `raw_transaction`, and failure reasons are stored as a JSON array. Missing `reportedTax` is accepted as a validly shaped transaction but yields `NON_COMPLIANT` status. Refund records should include `originalTransactionId` referencing a previously ingested sale.

## API quick reference

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/financial-transactions/batch` | Ingest transaction batch |
| `GET` | `/api/financial-transactions?customerId=CUST-101` | List/filter transactions |
| `GET` | `/api/rules` | List database rules |
| `PATCH` | `/api/rules/{id}/enabled?enabled=false` | Enable/disable a rule |
| `PUT` | `/api/rules/{id}/configuration` | Update a rule's JSON configuration |
| `GET` | `/api/exceptions?customerId=CUST-101&severity=HIGH&ruleName=HIGH_VALUE_TRANSACTION` | Filter exceptions |
| `GET` | `/api/audit-events/transactions/{transactionId}` | Read audit trail for a transaction |
| `GET` | `/api/reports/customers/{customerId}/tax-summary` | SQL-backed customer tax summary |
| `GET` | `/api/reports/exceptions/summary` | Total/by-severity/customer exception summary |
| `GET` | `/api/v1/taxpayers` | Existing taxpayer API; see controller for query options |
| `POST` | `/api/v1/taxpayers` | Register taxpayer |
| `POST` | `/api/v1/reported-income` | Register reported income; optional `Idempotency-Key` header |
| `POST` | `/api/v1/tax-gap-analyses/taxpayers/{taxpayerId}?taxYear=2026` | Run taxpayer tax-gap analysis |

All endpoints except actuator health/info require Basic auth. Rules are seeded in migration V7. Default rule configuration:

- `HIGH_VALUE_TRANSACTION`: amount greater than `100000` creates a HIGH exception.
- `REFUND_LIMIT`: refund must identify a prior successful sale using `originalTransactionId`, match its customer, and not exceed the sale amount.
- `GST_SLAB_VALIDATION`: configured slabs require minimum rates of 5%, 12%, and 18% at the seeded amount thresholds. These are sample configurable rules for the assessment, not a statement of current legal GST requirements.

Example rule configuration update:

```cmd
curl.exe -u devuser:LocalDevOnly-ChangeMe-2026 -X PUT http://localhost:8080/api/rules/3/configuration -H "Content-Type: application/json" -d "{\"slabs\":[{\"threshold\":0,\"requiredRate\":0.05},{\"threshold\":50000,\"requiredRate\":0.12},{\"threshold\":100000,\"requiredRate\":0.18}]}"
```

## Tests and coverage

Run unit and integration tests (integration tests use the real PostgreSQL test database configured in `src/test/resources/application-test.properties`):

```cmd
.\mvnw.cmd clean test
.\mvnw.cmd verify
```

JaCoCo HTML report: `target/site/jacoco/index.html`. Capture the coverage page for the assessment submission after `verify` completes. The test database must be created and accessible before running integration tests. Never run test migrations against a database containing production data.

## Architecture and schema

- Architecture explanation: [`docs/architecture.md`](docs/architecture.md)
- Database schema relationship diagram: [`docs/database-schema.mmd`](docs/database-schema.mmd)

## Production hardening checklist

- Replace the seeded development user and local database credentials.
- Add TLS at the ingress/API gateway and use a secret manager for credentials.
- Add authorization roles and least-privilege rule-management permissions.
- Add pagination to large transaction/exception/audit queries and retention policies for audit data.
- Add request size limits, idempotency for batch ingestion, metrics/alerts, and operational dashboards.
- Validate configured GST rules against the business owner's authoritative policy before production use.
