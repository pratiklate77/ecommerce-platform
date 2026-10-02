# Ecommerce Platform — Microservices

A microservice-based ecommerce platform built with **Java 17** and **Spring Boot 4.x**.

## Services

| Service              | Responsibility                              | Persistence | Messaging/Other              | Port (dev) |
|----------------------|---------------------------------------------|-------------|------------------------------|------------|
| `api-gateway`        | Single entry point, routing, (auth at edge) | —           | Spring Cloud Gateway (WebFlux) | 8080     |
| `user-service`       | Customers, auth (JWT), addresses, roles     | `user_db`   | Security, Validation         | 8081       |
| `product-service`    | Product catalog, search, hot items (cache)  | `product_db`| Redis cache                  | 8082       |
| `inventory-service`  | Stock levels, reserve/release               | `inventory_db` | Redis                         | 8083       |
| `order-service`      | Order lifecycle, checkout orchestration     | `order_db`     | —                             | 8084       |
| `payment-service`    | Payments & refunds                          | `payment_db`   | —                             | 8085       |
| `messaging-service`  | Email notifications (order/confirmation)    | —              | Mail (SMTP)                   | 8086       |

> The port assignments above are conventions for local development. The exact
> port each service binds is controlled by its own `server.port` config.

## Architecture overview

```
                       ┌──────────────────────────────┐
   Frontend ─────────▶ │          api-gateway         │  port 8080
                       └──────────────┬───────────────┘
            ┌─────────────┬───────────┴───────────┬──────────────┐
            ▼             ▼                       ▼              ▼
      user-service    product-service       inventory-      order-service
      (8081/JWT)      (8082/Redis)          service         (8084)
            │             │                  (8083)             │
            │             │                    │          payment-service
            │             │                    └────────▶  (dummy gateway)
            ▼             ▼                                  
      messaging-service          (SMTP notifications; invoked directly)
      (8086/SMTP)
```

### Communication model

- **Synchronous** HTTP for read/command APIs that a client calls directly on the
  business services, all fronted by the API Gateway. Services expose REST
  endpoints (e.g. `payment-service` handles payments/refunds through a simulated
  gateway).
- Backend flows are **synchronous HTTP** between services — there is no message
  broker.

### Infrastructure (local)

Everything the services depend on runs from one command:

```bash
docker compose up -d
```

| Component | Host port | Databases / details |
|-----------|-----------|---------------------|
| PostgreSQL | 5432 | `user_db`, `product_db`, `inventory_db`, `order_db`, `payment_db` (user `ecommerce` / pass `ecommerce`) |
| Redis | 6379 | — |
| MailHog (SMTP) | 1025 (SMTP) / 8025 (UI) | dev mail sink |

## Running the services

Each service is an independent Maven project. From a service directory:

```bash
mvn spring-boot:run
```

Start the infrastructure *before* any service that needs a database or
cache. The recommended start order for the full stack is:

1. `infra` (docker compose) up
2. `user-service` → `product-service` → `inventory-service`
3. `order-service` → `payment-service` → `messaging-service`
4. `api-gateway` (last, once real backends are reachable)

## Repo layout

```
├── docker-compose.yml         # local infra (postgres, redis, mailhog)
├── infra/
│   └── postgres/init/         # one database per owning service
├── backend/
│   ├── api-gateway/
│   ├── user-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── payment-service/
│   └── messaging-service/
└── frontend/                  # React/Vite customer storefront
```

## Core commands

```bash
mvn -f backend/product-service/pom.xml clean package   # build a single service
mvn clean package                                       # build from a service dir
docker compose logs -f                                  # follow infra logs
```

## Quick start (full stack)

Each service ships a production-ready Dockerfile. To build and run the entire
platform:

```bash
cp .env.example .env        # optional; every variable has a safe default
docker compose up -d --build
docker compose ps           # wait until every service is healthy
```

- API Gateway:   http://localhost:8080
- Storefront:    http://localhost:8088
- MailHog UI:    http://localhost:8025   (sent customer emails appear here)

The API Gateway is the single entry point. All business-service ports (8081–8086)
are also published for direct testing.

Overriding per-service hostnames: the Gateway's routes default to `localhost`
(for host-side `mvn spring-boot:run`). Inside Docker it is configured to route to
the service DNS names automatically (see `docker-compose.yml`). Set the
`*_SERVICE_HOST=localhost` env vars when running the Gateway from your IDE.

All services share the JWT signing secret. Keep `APP_JWT_SECRET` identical
across `user-service` (issuer) and `api-gateway` (verifier); the `docker
compose` example defaults both to the same dev secret.

## End-to-end smoke test (happy path)

1. **Register a customer** (returns a JWT):
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"email":"buyer@example.com","password":"Password123!","firstName":"Ada","lastName":"Lovelace"}'
   TOKEN="<paste access_token from the response>"
   ```

2. **Create a product + seed its inventory** (repeat for each SKU you want):
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/products -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' \
     -d '{"sku":"MOUSE-1","name":"Wireless Mouse","description":"Ergonomic","category":"Electronics","price":"29.99","currency":"USD"}'

   curl -s -X POST http://localhost:8080/api/v1/inventory/items -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' \
     -d '{"sku":"MOUSE-1","productId":1,"productName":"Wireless Mouse","availableQuantity":50,"reorderLevel":5}'
   ```

3. **Place an order** (identity is taken from the token, not the body):
   ```bash
   ORDER_JSON='{"currency":"USD","shippingCost":"4.99","taxAmount":"2.50","items":[{"productId":1,"sku":"MOUSE-1","productName":"Wireless Mouse","unitPrice":"29.99","quantity":2}]}'
   curl -s -X POST http://localhost:8080/api/v1/orders -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' -d "$ORDER_JSON"
   ```
   The order is created in the **PENDING** state (query `order-service` at
   `GET /api/v1/orders` to see it).

4. **Pay for the order** (dummy payment gateway — `PaymentGatewaySimulator`):
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/payments -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' \
     -d '{"orderId":1,"orderNumber":"<orderNumber>","amount":"67.47","currency":"USD","method":"CARD","paymentToken":"tok_test"}'
   ```
   The dummy gateway charges the token and the payment is stored as **VERIFIED**
   with a transaction reference; the response's `paymentId` can be used to
   refund it later.

5. **Cancel to exercise the refund path** (status must be cancelable):
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/orders/1/cancel -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' -d '{"reason":"changed my mind"}'
   ```
   Refund the verified payment through the dummy gateway by calling
   `POST /api/v1/payments/{paymentId}/refund` (against `payment-service`).

### Making a user an ADMIN

`register` always creates a `CUSTOMER`. To manage users/admin operations,
promote a user in `user_db` and log in again:

```sql
-- against the user_db database
UPDATE users SET role = 'ADMIN' WHERE email = 'buyer@example.com';
```

### Declined-payment shortcut

Pass a `paymentToken` containing the literal substring `decline`
(e.g. `tok_decline`) to see the payment FAILED path (the dummy gateway declines
the charge).

## Unit tests

```bash
mvn -f backend/inventory-service/pom.xml test   # + the other services
```

Unit tests use Mockito (no external infra required). The `*ApplicationTests`
context tests boot a full Spring context and therefore need the PostgreSQL /
Redis / MailHog containers from `docker compose`.
