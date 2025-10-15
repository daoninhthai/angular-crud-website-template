# Payment API

A production-ready Payment & Fintech REST API built with Spring Boot. Handles digital wallet management, P2P transfers, payment processing, and webhook notifications with enterprise-grade security and reliability.

## Tech Stack

- **Java 11** / **Spring Boot 2.7**
- **PostgreSQL 15** - Primary relational database
- **Redis 7** - Caching, idempotency key storage, rate limiting
- **Apache Kafka** - Asynchronous event streaming and notifications
- **Spring Security + JWT** - Stateless authentication with role-based access control
- **Docker Compose** - Full local development environment
- **Swagger/SpringFox** - Interactive API documentation

## Features

- **Digital Wallet** - Deposit, withdraw, and balance inquiry with real-time updates
- **P2P Money Transfer** - Peer-to-peer transfers with deadlock prevention via ordered locking
- **Payment Processing** - Merchant payment flow with refund support (full and partial)
- **Idempotency** - All write operations are idempotent using Redis + DB dual-layer storage
- **Webhook Notifications** - HMAC-SHA256 signed webhook delivery to merchants
- **Automatic Retry** - Exponential backoff retry for failed webhooks (up to 5 attempts)
- **JWT Authentication** - Stateless auth with role-based access control (USER, MERCHANT, ADMIN)
- **Comprehensive Audit Logging** - Full audit trail for all financial operations
- **Concurrency Safety** - Optimistic locking (@Version) + pessimistic locking for wallet operations
- **Input Validation** - Bean Validation (JSR 380) on all request DTOs
- **Event-Driven Architecture** - Kafka-based event publishing for transaction, payment, and transfer events

## API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/auth/register` | Register a new account | Public |
| `POST` | `/api/auth/login` | Authenticate and receive JWT | Public |
| `GET` | `/api/accounts/{accountNumber}` | Get account details | USER |
| `GET` | `/api/wallets/{accountNumber}/balance` | Get wallet balance | USER |
| `POST` | `/api/wallets/{accountNumber}/deposit` | Deposit funds | USER |
| `POST` | `/api/wallets/{accountNumber}/withdraw` | Withdraw funds | USER |
| `POST` | `/api/transfers` | Initiate a P2P transfer | USER |
| `GET` | `/api/transfers/{transferRef}` | Get transfer details | USER |
| `POST` | `/api/payments` | Create a new payment | USER |
| `GET` | `/api/payments/{paymentRef}` | Get payment details | USER |
| `POST` | `/api/payments/{paymentRef}/refund` | Refund a payment | USER |
| `GET` | `/api/transactions/{transactionRef}` | Get transaction details | USER |
| `GET` | `/api/transactions/history` | Get transaction history (paginated) | USER |
| `GET` | `/actuator/health` | Health check | Public |
| `GET` | `/actuator/info` | Application info | Public |
| `GET` | `/actuator/metrics` | Application metrics | Public |

All write endpoints require the `X-Idempotency-Key` header for safe retries.

## Getting Started

### Prerequisites

- Java 11+
- Maven 3.6+
- Docker & Docker Compose

### Quick Start

1. **Start infrastructure services:**

```bash
docker-compose up -d
```

2. **Run the application:**

```bash
mvn spring-boot:run
```

3. **Access Swagger UI:**

Open [http://localhost:8080/swagger-ui/](http://localhost:8080/swagger-ui/) in your browser.

### Running with Profiles

```bash
# Development (default)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Running Tests

```bash
mvn test
```

## Architecture

The application follows a layered architecture with clear separation of concerns:

```
Controller Layer (REST endpoints)
    |
Service Layer (Business logic, transaction management)
    |
Repository Layer (Data access, Spring Data JPA)
    |
Database (PostgreSQL)
```

**Cross-cutting concerns:**

- **Security** - JWT authentication filter intercepts requests and validates tokens. Role-based authorization via Spring Security annotations.
- **Idempotency** - Redis-backed idempotency service prevents duplicate processing of write operations. Keys are stored with configurable TTL.
- **Event Publishing** - Kafka producer publishes domain events (TransactionCreated, PaymentProcessed, TransferCompleted) for downstream consumers.
- **Webhook Delivery** - Asynchronous webhook service delivers signed payloads to merchant endpoints with exponential backoff retry.
- **Exception Handling** - Global exception handler translates domain exceptions into consistent API error responses.
- **Validation** - Bean Validation annotations on DTOs enforce input constraints at the controller layer.
- **Mapping** - Dedicated mapper components translate between entity and DTO layers, preventing entity leakage to API consumers.

## Project Structure

```
src/main/java/com/fintech/payment/
    config/          - Configuration classes (Security, Redis, Kafka, Swagger, Audit)
    controller/      - REST controllers
    dto/
        request/     - Request DTOs with validation annotations
        response/    - Response DTOs
    event/           - Kafka event payloads
    exception/       - Custom exceptions and global error handler
    mapper/          - Entity-to-DTO mappers
    model/
        entity/      - JPA entities
        enums/       - Enumeration types
    repository/      - Spring Data JPA repositories
    security/        - JWT provider, auth filter, user details service
    service/         - Business logic services
    util/            - Utility classes (currency, validation, masking)
```

## Configuration

Key configuration properties in `application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `jwt.secret` | JWT signing secret | (change in production) |
| `jwt.expiration` | JWT token TTL in milliseconds | 86400000 (24h) |
| `idempotency.ttl-minutes` | Idempotency key TTL | 1440 (24h) |
| `transfer.min-amount` | Minimum transfer amount | 1000 |
| `transfer.max-amount` | Maximum single transfer amount | 500000000 |
| `transfer.daily-limit` | Daily transfer limit per account | 2000000000 |
| `webhook.retry.max-retries` | Max webhook delivery attempts | 5 |

## TODO

- [ ] Stripe/VNPay payment gateway integration
- [ ] Rate limiting with Bucket4j
- [ ] API versioning (v1/v2) with URI-based strategy
- [ ] WebSocket support for real-time balance updates
- [ ] Flyway database migrations
- [ ] Distributed tracing with Sleuth + Zipkin
- [ ] Circuit breaker for external service calls (Resilience4j)
- [ ] Prometheus metrics export
- [ ] gRPC support for internal service communication
- [ ] Multi-currency real-time exchange rate integration
