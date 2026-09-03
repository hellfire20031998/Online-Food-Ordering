# Online Food Ordering — Backend

Spring Boot REST API for a food-ordering platform: customers browse restaurants and menus, manage a cart, and place orders; restaurant owners manage their restaurant, menu, categories, ingredients, and incoming orders.

The React frontend lives in its own repository: [Online-food-ordering-Frontend-](https://github.com/hellfire20031998/Online-food-ordering-Frontend-).

## Stack

Spring Boot 3.4 (Java 17), Spring Security + JWT, Spring Data JPA, MySQL (local dev) or PostgreSQL (production).

## Running locally

Requirements: JDK 17, and a database — MySQL with a `my_restaurant` database, or any PostgreSQL.

```bash
git clone https://github.com/hellfire20031998/Online-Food-Ordering.git
cd Online-Food-Ordering
./mvnw spring-boot:run
```

Configuration is environment-driven with local-dev fallbacks. For local use, copy `.env.example` to `.env` (gitignored) and fill in your values; real OS environment variables take precedence over `.env`.

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/my_restaurant` | JDBC URL (MySQL or PostgreSQL) |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `1234` | Database password |
| `JWT_SECRET` | dev-only fallback | **Set a real value in any non-local environment** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed origins |
| `PORT` | `8080` | HTTP port (set by Render automatically) |

### Tests

```bash
./mvnw test   # unit tests + context test on in-memory H2; no database needed
```

## Deployment

The repo ships a multi-stage `Dockerfile` and a `render.yaml` blueprint for [Render](https://render.com). Point Render at this repo (Language: Docker, root directory: the repo root) and set the environment variables above — use a `jdbc:postgresql://...` URL for a hosted PostgreSQL such as Neon or Supabase.

## Roles

- `CUSTOMER` — browse, cart, order.
- `ADMIN` — restaurant owner: full management of their own restaurant.
- `MANAGER` / `MEMBER` — restaurant staff, assigned by the owner via `/api/restaurant-roles/assign`.

Signup only permits choosing `CUSTOMER` or `ADMIN`; ownership of admin resources is checked per restaurant on every management endpoint.

## API reference

A Postman collection is included in the repo root (`REST API for FoodOrdering.postman_collection.json`).

Table relations / ER diagram:

![er sql](https://github.com/user-attachments/assets/3decf24c-a1e4-4908-a98a-724991f87342)
