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
| `TEAM_ADMIN_NAME` | `Platform Admin` | Display name of the seeded team admin |
| `TEAM_ADMIN_EMAIL` | *(empty)* | Email of the first `TEAM_ADMIN`; seeding is skipped when empty |
| `TEAM_ADMIN_PASSWORD` | *(empty)* | Password of the first `TEAM_ADMIN`; seeding is skipped when empty |
| `PLATFORM_DEFAULT_COMMISSION` | `10` | Initial platform commission (%); editable from the team console |
| `FRONTEND_URL` | `http://localhost:3000` | Public URL of the React app, used for links in emails |
| `DATA_ENCRYPTION_KEY` | dev-only fallback | Current AES-256 key for bank account data (32+ random chars) |
| `DATA_ENCRYPTION_PREVIOUS_KEYS` | *(empty)* | Comma-separated older keys kept readable during rotation |
| `ENFORCE_STRONG_ENCRYPTION_KEY` | `false` | Refuse to start on a weak/default key; set `true` in production |
| `EMAIL_PROVIDER` | `log` | `log`, `smtp` or `resend` |
| `EMAIL_FROM` | `Foodiyapa <no-reply@example.com>` | Sender address |
| `SMTP_HOST` / `SMTP_PORT` | `smtp.gmail.com` / `587` | SMTP relay (Gmail, Amazon SES SMTP, …) |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | *(empty)* | SMTP credentials (Gmail: an App Password) |
| `RESEND_API_KEY` | *(empty)* | Only when `EMAIL_PROVIDER=resend` |
| `PAYMENT_GATEWAY` | `none` | `none`, `stripe` (or `razorpay`, not yet implemented) |
| `STRIPE_SECRET_KEY` / `STRIPE_PUBLISHABLE_KEY` | *(empty)* | Required when `PAYMENT_GATEWAY=stripe` |
| `STRIPE_WEBHOOK_SECRET` | *(empty)* | Signing secret of the webhook endpoint (or from `stripe listen`) |

### Tests

```bash
./mvnw test   # unit tests + context test on in-memory H2; no database needed
```

## Deployment

The repo ships a multi-stage `Dockerfile` and a `render.yaml` blueprint for [Render](https://render.com). Point Render at this repo (Language: Docker, root directory: the repo root) and set the environment variables above — use a `jdbc:postgresql://...` URL for a hosted PostgreSQL such as Neon or Supabase.

## Roles

Three tiers: customers, restaurant side, and the platform team.

- `CUSTOMER` — browse, cart, order. **Public signup always creates a customer**; no role can be chosen.
- `ADMIN` — restaurant owner: full management of their own restaurant. Owners are onboarded through a team-approved application (phase 2).
- `MANAGER` / `MEMBER` — restaurant staff, assigned by the owner via `/api/restaurant-roles/assign`.
- `TEAM_ADMIN` — platform team: everything below plus team member management (`/api/team/members`).
- `TEAM_MANAGER` — platform team: suspend/reactivate restaurants, block/unblock customers, change the commission. Payouts and refunds later.
- `TEAM_CUSTOMER_SUPPORT` / `TEAM_RESTAURANT_SUPPORT` — platform team, read-only in phase 1.

Ownership of restaurant resources is checked per restaurant on every `/api/admin/**` endpoint. The team console lives under `/api/team/**`; route access is granted to every team role and admin/manager-only actions are enforced with `@PreAuthorize`.

The first `TEAM_ADMIN` is seeded at startup from `TEAM_ADMIN_EMAIL` / `TEAM_ADMIN_PASSWORD`. Blocked accounts are rejected at sign-in and on their next authenticated request. Suspended restaurants are hidden from public listings and cannot receive orders.

### Restaurant onboarding

1. A signed-in customer applies at `POST /api/restaurant-applications` (restaurant details, contact, photos, payout bank account). `GET .../me` shows their applications; `PUT .../{id}/withdraw` withdraws a pending one.
2. The team reviews the queue at `/api/team/restaurant-applications`. `TEAM_ADMIN`, `TEAM_MANAGER` and `TEAM_RESTAURANT_SUPPORT` may approve or reject (a reason is required); other team roles can only read.
3. Approval creates the restaurant (closed until the owner opens it), stores the payout account, promotes the applicant to `ADMIN` and emails them. The owner must sign in again to receive a token with the new role.

Bank account numbers and UPI ids are encrypted at rest (AES-GCM, key from `DATA_ENCRYPTION_KEY`) and returned masked to everyone except the owner (`/api/admin/restaurants/{id}/bank-account`) and `TEAM_ADMIN` / `TEAM_MANAGER` (`/api/team/restaurants/{id}/bank-account`).

### Payments, refunds and payouts

Every order has a `Payment`. Cash on delivery is settled (with the commission snapshot) when the restaurant marks the order delivered or completed. Online methods go through one `PaymentGateway` implementation selected by `PAYMENT_GATEWAY`:

- `none` (default): only cash on delivery is offered at checkout.
- `stripe`: PaymentIntents with automatic payment methods. The browser completes the payment with the client secret returned by `POST /api/order`, then calls `POST /api/payments/{id}/confirm`; Stripe's webhook (`POST /api/payments/webhook/stripe`, signature-verified, no JWT) is the source of truth in production. Unpaid orders sit in `PAYMENT_PENDING` and are hidden from the restaurant.
- `razorpay`: placeholder bean documenting the contract; not implemented.

Refunds: customers request them on their own orders (`/api/orders/{id}/refunds`); cancelling a paid order raises one automatically. `TEAM_ADMIN` / `TEAM_MANAGER` approve, reject, or create refunds at `/api/team/refunds`. Gateway payments are reversed through the gateway; cash orders are refunded by manual bank transfer to the customer's account or UPI id (encrypted at rest), then completed with the bank reference.

Payouts: `POST /api/team/payouts/generate` creates one pending payout per restaurant for a period from settled payments not yet paid out, net of refunds and commission (each payment keeps the commission rate in force when it was paid). The team transfers the money and records the reference with `mark-paid`; cancelling a pending payout releases its payments. Owners see earnings and payouts at `/api/admin/restaurants/{id}/earnings` and `/payouts`.

### Protecting bank account data

Bank account numbers and UPI ids (restaurant payout accounts, customers' saved refund accounts, refund destinations, payout snapshots) are handled to the standard the RBI expects for sensitive customer financial data:

- **Encrypted at rest** with AES-256-GCM and a fresh IV per value (`EncryptedStringConverter`). Ciphertext carries the fingerprint of the key that produced it.
- **Key management**: the key comes from `DATA_ENCRYPTION_KEY` (never committed, unique per environment, at least 32 random characters). `ENFORCE_STRONG_ENCRYPTION_KEY=true` refuses to start on a weak or default key. Rotation: move the old key into `DATA_ENCRYPTION_PREVIOUS_KEYS`, set a new key, restart, then run *Re-encrypt* from Settings > Security (`POST /api/team/security/encryption/rotate`), which rewrites every encrypted column under the current key.
- **Masked by default**: APIs return `****1234`-style values except to the account's owner and to `TEAM_ADMIN` / `TEAM_MANAGER`.
- **Access audit**: every unmasked view by a team member is written to `sensitive_data_access_log` (who, which record, when) and visible at `GET /api/team/security/access-log`.
- **Never logged**: sensitive fields are excluded from entity `toString()` and are not written to application logs.
- **No card data**: card, UPI-app and net-banking credentials never touch this system; the payment gateway tokenises them (RBI card-on-file tokenisation rules are the gateway's responsibility).
- **Deployment**: serve the API over HTTPS only, and host the database in India to satisfy the RBI payment-data localisation circular.

Customers manage their saved refund account at `/api/users/me/bank-account`; a cash-on-delivery refund request without bank details falls back to it, and `saveBankAccount: true` on a request stores the supplied details.

### Email

Transactional email (application received / approved / rejected, team account created) goes through one `EmailSender` implementation selected by `EMAIL_PROVIDER`:

- `log` (default): messages are written to the application log only.
- `smtp`: Spring Mail. Gmail with an App Password, or Amazon SES through its SMTP endpoint (`SMTP_HOST=email-smtp.<region>.amazonaws.com`).
- `resend`: Resend HTTP API (`RESEND_API_KEY`).

Sending is asynchronous; a failing mail server is logged and never fails the user's request.

## API reference

A Postman collection is included in the repo root (`REST API for FoodOrdering.postman_collection.json`).

Table relations / ER diagram:

![er sql](https://github.com/user-attachments/assets/3decf24c-a1e4-4908-a98a-724991f87342)
