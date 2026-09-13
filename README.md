# Ecommerce Platform — Microservices

A microservice-based ecommerce platform built with **Java 17** and **Spring Boot 4.x**.

## Services

| Service              | Responsibility                              | Persistence | Messaging/Other              | Port (dev) |
|----------------------|---------------------------------------------|-------------|------------------------------|------------|
| `api-gateway`        | Single entry point, routing, (auth at edge) | —           | Spring Cloud Gateway (WebFlux) | 8080     |
| `user-service`       | Customers, auth (JWT), addresses, roles     | `user_db`   | Security, Validation         | 8081       |
| `product-service`    | Product catalog, search, hot items (cache)  | `product_db`| Redis cache                  | 8082       |
| `inventory-service`  | Stock levels, reserve/release               | `inventory_db` | Redis + Kafka             | 8083       |
| `order-service`      | Order lifecycle, checkout orchestration     | `order_db`  | Kafka                        | 8084       |
| `payment-service`    | Payments & refunds                          | `payment_db`| Kafka                        | 8085       |
| `messaging-service`  | Email notifications (order/confirmation)    | —           | Kafka, Mail (SMTP)           | 8086       |

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
            │             │                    └────────▶  (8085)
            │             │                        (Kafka events)
            ▼             ▼                                  
      messaging-service ◀────────── events: order.created, order.paid,
      (8086/SMTP)                     stock.reserved, payment.verified
```

### Communication model

- **Synchronous** HTTP for read/command APIs that a client calls directly on the
  business services, all fronted by the API Gateway.
- **Asynchronous, event-driven** (Kafka) for cross-service state flows:
  order → inventory (reserve stock) → payment → messaging (notify customer).

### Infrastructure (local)

Everything the services depend on runs from one command:

```bash
docker compose up -d
```

| Component | Host port | Databases / details |
|-----------|-----------|---------------------|
| PostgreSQL | 5432 | `user_db`, `product_db`, `inventory_db`, `order_db`, `payment_db` (user `ecommerce` / pass `ecommerce`) |
| Redis | 6379 | — |
| Kafka (KRaft) | 9092 | auto topic creation on |
| MailHog (SMTP) | 1025 (SMTP) / 8025 (UI) | dev mail sink |

## Running the services

Each service is an independent Maven project. From a service directory:

```bash
mvn spring-boot:run
```

Start the infrastructure *before* any service that needs a database, broker, or
cache. The recommended start order for the full stack is:

1. `infra` (docker compose) up
2. `user-service` → `product-service` → `inventory-service`
3. `order-service` → `payment-service` → `messaging-service`
4. `api-gateway` (last, once real backends are reachable)

## Repo layout

```
├── docker-compose.yml         # local infra (postgres, kafka, redis, mailhog)
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
└── frontend/                  # planned frontend
```

## Core commands

```bash
mvn -f backend/product-service/pom.xml clean package   # build a single service
mvn clean package                                       # build from a service dir
docker compose logs -f                                  # follow infra logs
```