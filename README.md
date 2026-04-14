# digit-permit-service

> A multi-tenant government permit issuance microservice built on the DIGIT platform.  
> Designed for the Qualisys Consulting Technical Assessment.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Multi-Tenancy](#multi-tenancy)
- [Transactional Outbox Pattern](#transactional-outbox-pattern)
- [Resilience — Circuit Breaker & Retry](#resilience--circuit-breaker--retry)
- [Payment Gateway Mock (WireMock)](#payment-gateway-mock-wiremock)
- [N+1 Query Prevention](#n1-query-prevention)
- [Database Migrations (Flyway)](#database-migrations-flyway)
- [Outbox Poller](#outbox-poller)
- [Event Consumer](#event-consumer)
- [Testing the System](#testing-the-system)
- [Known Simplifications](#known-simplifications)

---

## Overview

`digit-permit-service` is a **Spring Boot microservice** that handles government permit issuance for multiple ministries (tenants) on a single shared infrastructure. It is built to satisfy the following core requirements:

- **Multi-tenancy** — Complete data isolation between ministries at the database level using PostgreSQL schema-per-tenant
- **Transactional Event Publishing** — Guaranteed `PermitCreated` event delivery to RabbitMQ using the Transactional Outbox Pattern
- **Resilience** — Circuit Breaker, Retry, and TimeLimiter wrapping all external payment gateway calls via Resilience4j
- **Performance** — N+1 query prevention on the summary endpoint using `JOIN FETCH`

The entire system runs **100% locally** via a single Docker Compose command with zero cloud dependencies.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     MINISTRY PORTAL (Client)                    │
│          POST /api/permits · GET /api/permits/summary           │
│           X-Tenant-ID: Ministry_Health / Ministry_Education     │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP Request + X-Tenant-ID header
┌──────────────────────────────▼──────────────────────────────────┐
│              digit-permit-service · Spring Boot · :8080         │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  OncePerRequestFilter — Tenant Security Gate            │   │
│  │  Reads header · Validates public.tenants · ThreadLocal  │   │
│  └──────────────────────────┬──────────────────────────────┘   │
│                             │                                   │
│  ┌──────────────────────────▼──────────────────────────────┐   │
│  │  PermitController    │    SummaryController             │   │
│  │  POST /api/permits   │    GET /api/permits/summary      │   │
│  └──────────────────────┬────────────────────────────────  ┘   │
│                         │                                       │
│  ┌──────────────────────▼──────────────────────────────────┐   │
│  │  PermitService — Business Logic · State Machine         │   │
│  │  findOrCreateApplicant · verifyPayment · buildPermit    │   │
│  └──────────────────────┬────────────────────────────────  ┘   │
│                         │                                       │
│  ┌──────────────────────▼──────────────────────────────────┐   │
│  │  Resilience4j — CircuitBreaker · Retry · TimeLimiter    │   │
│  └──────────────────────┬────────────────────────────────  ┘   │
│                         │                                       │
│  ┌──────────────────────▼──────────────────────────────────┐   │
│  │  Single DB Transaction                                  │   │
│  │  ├── INSERT applicants (if new)                         │   │
│  │  ├── INSERT permits                                     │   │
│  │  ├── INSERT payment_records                             │   │
│  │  └── INSERT outbox_events (status = PENDING)            │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  OutboxEventPoller — @Scheduled every 5s                 │  │
│  │  Loops all tenants · Sets ThreadLocal · Publishes        │  │
│  └──────────────────────┬─────────────────────────────────  ┘  │
└──────────────────────────┼──────────────────────────────────────┘
                           │
          ┌────────────────┼────────────────────┐
          │                │                    │
   ┌──────▼──────┐  ┌──────▼──────┐   ┌────────▼───────┐
   │ PostgreSQL  │  │  RabbitMQ   │   │   WireMock     │
   │   :5432     │  │  :5672      │   │   :8081        │
   │             │  │  UI: :15672 │   │                │
   │ public      │  │             │   │ POST           │
   │ ├ tenants   │  │ permit      │   │ /api/payments  │
   │             │  │ .created    │   │ /verify        │
   │ min_health  │  │ queue       │   │                │
   │ ├ permits   │  └──────┬──────┘   │ 3s delay       │
   │ ├ applicants│         │          │ 30% → 503      │
   │ ├ payment_  │  ┌──────▼──────┐   │ 70% → 200      │
   │   records   │  │   Event     │   └────────────────┘
   │ └ outbox_   │  │  Consumer   │
   │   events    │  │   :8082     │
   │             │  └─────────────┘
   │ min_edu     │
   │ (same)      │
   └─────────────┘
```

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Core language |
| Spring Boot | 3.x | Application framework |
| Spring Data JPA | 3.x | ORM / repository layer |
| Hibernate | 6.x | JPA implementation · multi-tenancy strategy |
| PostgreSQL | 16-alpine | Relational database · schema-per-tenant |
| Flyway | 9.x | Database migrations · per-tenant schema provisioning |
| RabbitMQ | 3-management-alpine | Message broker · PermitCreated events |
| Resilience4j | 2.x | Circuit Breaker · Retry · TimeLimiter |
| WireMock | 3.2.0 | Payment gateway mock · 3s delay · 30% 503 |
| Docker Compose | 3.8 | Local orchestration · zero cloud dependencies |
| Lombok | latest | Boilerplate reduction |
| Jackson | 2.x | JSON serialisation · outbox payload |

---

## Project Structure

```
digit-permit-service/
├── src/
│   └── main/
│       ├── java/devtom/digitpermit/
│       │   ├── config/
│       │   │   ├── HibernateConfig.java          # Multi-tenancy JPA configuration
│       │   │   ├── RabbitMQConfig.java            # Exchange · queue · binding
│       │   │   └── RestTemplateConfig.java        # HTTP client for WireMock
│       │   ├── controller/
│       │   │   └── PermitController.java          # POST /api/permits · GET /summary
│       │   ├── dto/
│       │   │   ├── CreatePermitRequest.java
│       │   │   ├── CreateApplicantRequest.java
│       │   │   ├── PermitResponse.java
│       │   │   ├── PermitSummaryResponse.java
│       │   │   ├── ApplicantResponse.java
│       │   │   ├── OutboxEventPayload.java
│       │   │   ├── PaymentVerificationRequest.java
│       │   │   └── PaymentVerificationResponse.java
│       │   ├── entity/
│       │   │   ├── Tenant.java                    # public schema · registry
│       │   │   ├── Applicant.java
│       │   │   ├── Permit.java
│       │   │   ├── PaymentRecord.java
│       │   │   └── OutboxEvent.java
│       │   ├── enums/
│       │   │   ├── PermitType.java
│       │   │   ├── PermitStatus.java
│       │   │   ├── PaymentStatus.java
│       │   │   ├── OutboxStatus.java
│       │   │   └── TenantStatus.java
│       │   ├── filter/
│       │   │   └── TenantFilter.java              # OncePerRequestFilter
│       │   ├── mapper/
│       │   │   ├── PermitMapper.java
│       │   │   └── ApplicantMapper.java
│       │   ├── messaging/
│       │   │   └── RabbitMQPublisher.java
│       │   ├── repository/
│       │   │   ├── TenantRepository.java
│       │   │   ├── ApplicantRepository.java
│       │   │   ├── PermitRepository.java
│       │   │   ├── PaymentRecordRepository.java
│       │   │   └── OutboxEventRepository.java
│       │   ├── scheduler/
│       │   │   └── OutboxEventPoller.java         # @Scheduled · per-tenant loop
│       │   ├── service/
│       │   │   ├── TenantService.java             # Schema creation · Flyway
│       │   │   ├── PermitService.java             # Core business logic
│       │   │   ├── OutboxEventService.java        # Transactional event processing
│       │   │   └── PaymentGatewayClient.java      # Resilience4j-wrapped HTTP call
│       │   └── tenancy/
│       │       ├── TenantContextHolder.java       # ThreadLocal wrapper
│       │       ├── TenantIdentifierResolver.java  # CurrentTenantIdentifierResolver
│       │       └── TenantConnectionProvider.java  # MultiTenantConnectionProvider
│       └── resources/
│           ├── application.yml
│           └── db/
│               └── migration/
│                   ├── public/
│                   │   └── V1__create_tenant_table.sql
│                   └── tenants/
│                       └── V1__create_tenant_tables.sql
├── permit-event-consumer/                         # Optional consumer service
│   └── src/main/java/devtom/consumer/
│       ├── PermitEventConsumer.java
│       └── ConsumerRabbitMQConfig.java
├── wiremock/
│   └── mappings/
│       └── payment-verify.json                   # WireMock stub configuration
├── docker-compose.yml
├── Dockerfile
└── README.md
```

---

## Getting Started

### Prerequisites

The only requirement on the reviewer's machine is:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) with Docker Compose

No Java, no Maven, no PostgreSQL, no RabbitMQ installation required.

### Running the System

```bash
# 1. Clone the repository
git clone https://github.com/DevTom/digit-permit-service.git

# 2. Navigate into the project
cd digit-permit-service

# 3. Start all services
docker compose up --build
```

This single command starts all five services:

| Service | URL | Credentials |
|---|---|---|
| Permit Service API | http://localhost:8080 | — |
| RabbitMQ Management UI | http://localhost:15672 | guest / guest |
| WireMock Admin | http://localhost:8081/__admin | — |
| PostgreSQL | localhost:5432 | username / password |
| Event Consumer | http://localhost:8082 | — |

### Stopping the System

```bash
docker compose down

# To also remove volumes (wipes the database)
docker compose down -v
```

---

## Environment Variables

All environment variables are defined in `docker-compose.yml`. No `.env` file is required to run the system.

| Variable | Service | Value |
|---|---|---|
| `POSTGRES_USER` | postgres · app | `username` |
| `POSTGRES_PASSWORD` | postgres · app | `password` |
| `POSTGRES_DB` | postgres · app | `digit_permit_db` |
| `SPRING_DATASOURCE_URL` | app | `jdbc:postgresql://postgres:5432/digit_permit_db` |
| `SPRING_RABBITMQ_HOST` | app | `event-listening` |
| `PAYMENT_GATEWAY_URL` | app | `http://external-services-mock:8080` |
| `RABBITMQ_DEFAULT_USER` | rabbitmq · consumer | `guest` |
| `RABBITMQ_DEFAULT_PASS` | rabbitmq · consumer | `guest` |

---

## API Reference

### POST /api/permits

Creates a new permit application. Verifies payment, saves the permit, and publishes a `PermitCreated` event.

**Required Header:**
```
X-Tenant-ID: Ministry_Health
```

**Request Body:**
```json
{
  "applicant": {
    "firstName": "Emeka",
    "lastName": "Okafor",
    "email": "emeka@email.com",
    "phoneNumber": "08031234567",
    "nationalId": "NIN-2948301"
  },
  "permitType": "HEALTH",
  "description": "Pharmacy business permit - 14 Marina Street Lagos"
}
```

**Permit Types:** `HEALTH` · `CONSTRUCTION` · `BUSINESS` · `ENVIRONMENTAL`

**Success Response — 201 Created:**
```json
{
  "permitId": "a3f9c112-...",
  "permitNumber": "HEA-2024-00147",
  "status": "PAYMENT_VERIFIED",
  "permitType": "HEALTH",
  "description": "Pharmacy business permit - 14 Marina Street Lagos",
  "applicant": {
    "id": "b7d2e...",
    "firstName": "Emeka",
    "lastName": "Okafor",
    "email": "emeka@email.com",
    "phoneNumber": "08031234567",
    "nationalId": "NIN-2948301"
  },
  "paymentStatus": "SUCCESS",
  "message": "Payment confirmed. Your permit application is under review.",
  "createdAt": "2024-11-08T14:32:00"
}
```

**Error Responses:**

| Status | Cause |
|---|---|
| `400 Bad Request` | Missing or malformed request body |
| `400 Bad Request` | Missing `X-Tenant-ID` header |
| `403 Forbidden` | Unknown or inactive tenant |

---

### GET /api/permits/summary

Returns all permits for the requesting ministry. Uses `JOIN FETCH` to prevent N+1 queries.

**Required Header:**
```
X-Tenant-ID: Ministry_Health
```

**Success Response — 200 OK:**
```json
[
  {
    "permitId": "a3f9c112-...",
    "permitNumber": "HEA-2024-00147",
    "status": "PAYMENT_VERIFIED",
    "permitType": "HEALTH",
    "applicantFullName": "Emeka Okafor",
    "applicantNationalId": "NIN-2948301",
    "createdAt": "2024-11-08T14:32:00"
  }
]
```

> **Data isolation proof:** Calling this endpoint with `X-Tenant-ID: Ministry_Education` returns only Education permits. It is physically impossible for this endpoint to return Health ministry data regardless of what is in the request body.

---

## Multi-Tenancy

### How It Works

The system implements **schema-per-tenant isolation** in PostgreSQL. Each ministry gets a dedicated schema within a single database instance.

```
digit_permit_db
├── public              ← platform schema (tenants registry)
│   └── tenants
├── ministry_health     ← Ministry of Health schema
│   ├── applicants
│   ├── permits
│   ├── payment_records
│   └── outbox_events
└── ministry_education  ← Ministry of Education schema
    ├── applicants
    ├── permits
    ├── payment_records
    └── outbox_events
```

### Isolation Mechanism

1. Every request passes through `TenantFilter` (`OncePerRequestFilter`)
2. The filter reads `X-Tenant-ID`, validates it against `public.tenants`, and stores it in a `ThreadLocal`
3. `TenantIdentifierResolver` reads the `ThreadLocal` on every Hibernate operation
4. `TenantConnectionProvider` executes `SET search_path TO ministry_health` on the connection before any query runs
5. PostgreSQL enforces the boundary — `SELECT * FROM permits` can only ever reach `ministry_health.permits`
6. On connection release, `search_path` is reset to `public` — preventing connection pool contamination

### Testing Isolation

```bash
# Create a permit for Ministry of Health
curl -X POST http://localhost:8080/api/permits \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: Ministry_Health" \
  -d '{ "applicant": { ... }, "permitType": "HEALTH", "description": "..." }'

# Query from Ministry of Education — returns empty, not Health data
curl http://localhost:8080/api/permits/summary \
  -H "X-Tenant-ID: Ministry_Education"

# Query from Ministry of Health — returns only Health permits
curl http://localhost:8080/api/permits/summary \
  -H "X-Tenant-ID: Ministry_Health"
```

### Security Note

In this assessment the `X-Tenant-ID` header is trusted as provided. In a production system this header would be derived from a validated JWT claim, not accepted at face value from the client. The tenant resolution mechanism is identical — only the trust boundary changes.

---

## Transactional Outbox Pattern

### The Problem Being Solved

A naive implementation of "save permit and publish event" has a fundamental flaw:

```
// DANGEROUS — dual write problem
permitRepository.save(permit);      // succeeds
rabbitTemplate.send(event);         // fails — event lost forever
```

If the message broker is down after the permit saves, the event is lost. The permit exists but no downstream system was notified.

### The Solution

The outbox pattern eliminates this by writing the event **into the same database transaction** as the permit:

```
Single Transaction:
├── INSERT into permits         ← domain write
├── INSERT into payment_records ← domain write
└── INSERT into outbox_events   ← event write (status = PENDING)
    ↓ COMMIT (atomic — all succeed or all fail)

Separate background process:
└── Reads outbox_events WHERE status = PENDING
    └── Publishes to RabbitMQ
        └── Marks status = PUBLISHED
```

The permit and the event intent are **always in sync** because they live in the same database. The broker delivery is eventually consistent but guaranteed.

### Observing the Outbox

Connect to PostgreSQL and query the outbox table directly:

```sql
-- Set the schema for the ministry you're inspecting
SET search_path TO ministry_health;

-- View all events and their publishing status
SELECT id, event_type, status, retry_count, created_at, published_at
FROM outbox_events
ORDER BY created_at DESC;
```

---

## Resilience — Circuit Breaker & Retry

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      permit-service:
        slidingWindowSize: 10
        slidingWindowType: COUNT_BASED
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 5
        registerHealthIndicator: true
  timelimiter:
    instances:
      permit-service:
        timeoutDuration: 2s
  retry:
    instances:
      permit-service:
        maxAttempts: 3
        waitDuration: 1000ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

### Circuit Breaker States

```
CLOSED (normal operation)
    │
    │ failure rate exceeds 50% across last 10 calls
    ▼
OPEN (failing fast — no calls attempted)
    │
    │ after 10 seconds cooldown
    ▼
HALF-OPEN (testing recovery — 5 trial calls)
    │                    │
    │ trials succeed      │ trials fail
    ▼                    ▼
CLOSED               OPEN again
```

### Retry Behaviour

When a call fails, Resilience4j retries with exponential backoff:

| Attempt | Wait Before Retry |
|---|---|
| 1st retry | 1000ms |
| 2nd retry | 2000ms |
| 3rd retry (final) | 4000ms → fallback triggered |

### Fallback Behaviour

When all retries are exhausted or the circuit is open, the fallback method fires. The permit is still created with `status = PENDING_PAYMENT`. It is not lost. The response to the client is:

```json
{
  "status": "PENDING_PAYMENT",
  "message": "Payment verification is temporarily unavailable. Your application has been saved."
}
```

### Observing Circuit Breaker Health

```bash
curl http://localhost:8080/actuator/health
```

The response includes the circuit breaker state:

```json
{
  "components": {
    "circuitBreakers": {
      "details": {
        "permit-service": {
          "state": "CLOSED",
          "failureRate": "20.0%",
          "bufferedCalls": 10
        }
      }
    }
  }
}
```

---

## Payment Gateway Mock (WireMock)

### What It Simulates

WireMock runs as a Docker container at `:8081` and pretends to be an external payment gateway. It is configured to:

- Always wait **3 seconds** before responding (simulating a slow external API)
- Return **200 SUCCESS** 70% of the time
- Return **503 Service Unavailable** 30% of the time

This deterministic randomness exercises the full Resilience4j chain on every request.

### Stub Registration

The 70/30 weighted stub is registered against WireMock's Admin API on startup. The stub targets a single endpoint:

```
POST /api/payments/verify
```

Your Permit Service always calls this one endpoint. It never knows in advance whether it will receive a success or a failure — exactly as it would behave against a real unstable external API.

### Inspecting WireMock

The WireMock management UI is available at:

```
http://localhost:8081/__admin/mappings
```

This shows all registered stubs, request logs, and response statistics.

---

## N+1 Query Prevention

### The Problem

When fetching a list of permits, each permit has an associated `Applicant`. A naive query causes N+1:

```
Query 1:  SELECT * FROM permits          → returns 100 rows
Query 2:  SELECT * FROM applicants WHERE id = 1
Query 3:  SELECT * FROM applicants WHERE id = 2
...
Query 101: SELECT * FROM applicants WHERE id = 100
```

101 queries for 100 permits. With 1,000 permits this becomes 1,001 queries — catastrophic for performance.

### The Solution

The `GET /api/permits/summary` endpoint uses `JOIN FETCH` in the JPQL query to load all data in a single SQL statement:

```java
@Query("SELECT p FROM Permit p JOIN FETCH p.applicant")
List<Permit> findAllWithApplicant();
```

This produces exactly **one SQL query** regardless of how many permits exist:

```sql
SELECT p.*, a.*
FROM permits p
INNER JOIN applicants a ON p.applicant_id = a.id
```

### Proof via SQL Logging

SQL logging is enabled in `application.yml`:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

When you call `GET /api/permits/summary`, observe the console output. You will see **exactly one SELECT statement** — not one per permit. This is your observable proof that N+1 is prevented.

---

## Database Migrations (Flyway)

### Two Migration Contexts

Flyway runs in two separate contexts on application startup:

**Context 1 — Public schema (runs once):**
```
db/migration/public/V1__create_tenant_table.sql
```
Creates the `public.tenants` registry table and seeds the two default ministries: `Ministry_Health` and `Ministry_Education`.

**Context 2 — Tenant schemas (runs per tenant):**
```
db/migration/tenants/V1__create_tenant_tables.sql
```
Creates `applicants`, `permits`, `payment_records`, and `outbox_events` tables. This script is executed once for every active tenant found in `public.tenants`, targeting each ministry's schema.

### Adding a New Ministry

To onboard a new ministry at runtime, call `TenantService.createTenant()`. It will:

1. Create the PostgreSQL schema (`IF NOT EXISTS`)
2. Run the tenant migration scripts against the new schema via Flyway
3. Insert a record into `public.tenants` making the tenant visible to the request filter

> Note: Schema creation uses DDL which PostgreSQL auto-commits. Compensating logic drops the schema if Flyway migration fails, preventing half-initialised tenants.

---

## Outbox Poller

The `OutboxEventPoller` runs every **5 seconds** as a `@Scheduled` background job.

### Why Per-Tenant Looping Is Required

The poller runs on a background thread — not an HTTP request thread. This means no `X-Tenant-ID` header arrives, the `OncePerRequestFilter` does not run, and the `ThreadLocal` is empty. Without tenant context, Hibernate defaults to the `public` schema which has no `outbox_events` table.

The poller solves this by explicitly setting the tenant context for each tenant before querying:

```
Every 5 seconds:
    Fetch all ACTIVE tenants from public.tenants
    For each tenant:
        1. Manually set ThreadLocal = tenant schema name
        2. Query outbox_events WHERE status = PENDING
        3. For each event: publish to RabbitMQ
        4. Mark event as PUBLISHED
        5. Clear ThreadLocal (finally block — always runs)
```

### Retry and Failure Handling

| State | Behaviour |
|---|---|
| Publish succeeds | `status = PUBLISHED`, `publishedAt = now()` |
| Publish fails (attempt 1-4) | `retryCount++`, stays `PENDING`, retried next poll |
| Publish fails (attempt 5) | `status = FAILED`, excluded from future polls |

Failed events can be identified and investigated with:

```sql
SET search_path TO ministry_health;
SELECT * FROM outbox_events WHERE status = 'FAILED';
```

> **Production note:** In a high-volume production environment this polling approach would be replaced with [Debezium](https://debezium.io/) — a change data capture tool that reacts to PostgreSQL write-ahead log entries in real time, eliminating the polling interval entirely.

---

## Event Consumer

The `permit-event-consumer` is an optional lightweight Spring Boot service running at `:8082`. Its sole purpose is to consume `PermitCreated` events from the RabbitMQ `permit.created` queue and log them — providing observable end-to-end proof that the publishing pipeline works.

### Expected Log Output

When a permit is created and the outbox poller publishes the event, the consumer logs:

```
╔══════════════════════════════════════════════════
║ PermitCreated Event Received
║ Permit ID     : a3f9c112-...
║ Permit Number : HEA-2024-00147
║ Permit Type   : HEALTH
║ Applicant     : Emeka Okafor
║ National ID   : NIN-2948301
║ Ministry      : Ministry_Health
║ Status        : PAYMENT_VERIFIED
║ Timestamp     : 2024-11-08T14:32:00
╚══════════════════════════════════════════════════
```

This log confirms the full pipeline completed successfully:
**permit saved → outbox event saved → poller published → consumer received.**

---

## Testing the System

### Full Happy Path Test

```bash
# 1. Create a permit for Ministry of Health
curl -X POST http://localhost:8080/api/permits \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: Ministry_Health" \
  -d '{
    "applicant": {
      "firstName": "Emeka",
      "lastName": "Okafor",
      "email": "emeka@email.com",
      "phoneNumber": "08031234567",
      "nationalId": "NIN-2948301"
    },
    "permitType": "HEALTH",
    "description": "Pharmacy business permit - 14 Marina Street Lagos"
  }'

# 2. Retrieve all Health Ministry permits
curl http://localhost:8080/api/permits/summary \
  -H "X-Tenant-ID: Ministry_Health"

# 3. Verify isolation — Education Ministry sees nothing
curl http://localhost:8080/api/permits/summary \
  -H "X-Tenant-ID: Ministry_Education"
```

### Test Missing Header (expects 400)

```bash
curl -X POST http://localhost:8080/api/permits \
  -H "Content-Type: application/json" \
  -d '{ "applicant": { ... }, "permitType": "HEALTH", "description": "..." }'
```

### Test Invalid Tenant (expects 403)

```bash
curl http://localhost:8080/api/permits/summary \
  -H "X-Tenant-ID: Ministry_Finance"
```

### Test Repeat Applicant (findOrCreate)

```bash
# Submit two permits with the same nationalId
# First creates the applicant, second reuses the existing record
# Only one applicant row should exist in ministry_health.applicants

curl -X POST http://localhost:8080/api/permits \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: Ministry_Health" \
  -d '{ "applicant": { "nationalId": "NIN-2948301", ... }, "permitType": "BUSINESS", ... }'
```

### Observe Circuit Breaker Opening

```bash
# Run this in a loop — after enough 503 responses the circuit will open
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/permits \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: Ministry_Health" \
    -d '{ ... }'
  echo "Request $i complete"
done

# Check circuit breaker state
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
```

---

## Known Simplifications

The following simplifications were made deliberately to keep the scope focused on what the assessment is testing:

| Simplification | Production Equivalent |
|---|---|
| `X-Tenant-ID` header trusted as provided | Header value derived from validated JWT claim |
| No authentication or authorisation layer | OAuth2 / JWT with ministry-scoped roles |
| Single outbox poller thread | Debezium CDC for real-time event capture |
| Fixed permit processing fee hardcoded | Fee schedule table per permit type per ministry |
| No permit approval workflow UI | Ministry officer review portal |
| WireMock stub registered manually | Real payment gateway integration |
| Sequential permit number generation | Distributed sequence with collision guarantees |

---

## Author

**Ridwanullahi Towolawi**  
Backend Engineer  
[GitHub](https://github.com/DevTom) · devtom@email.com

---