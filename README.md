# Harbr

A backend for rental platform built with Spring Boot 4, providing RESTful APIs for property listings, bookings, payments, messaging, reviews, and notifications.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21, Spring Boot 4.0.6 |
| Web | Spring MVC, Spring WebSocket (STOMP) |
| Security | Spring Security, JWT (JJWT 0.12) |
| Data | Spring Data JPA, Hibernate 7, PostgreSQL 16 (PostGIS) |
| Cache | Redis |
| Messaging | RabbitMQ (Spring AMQP) |
| Search | Elasticsearch 7.17 (optional, disabled by default) |
| Database Migrations | Flyway |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Payments | Stripe Java SDK (with mock gateway for local dev) |
| Build | Maven, Lombok |

## Architecture

The application follows a **package-by-feature** modular monolith structure. Each business domain is organized into four layers:

```
com.harbr.{module}/
├── api/                  # REST controllers & WebSocket handlers
├── application/           # Service classes & DTOs
│   └── dto/              # Request/Response records
├── domain/               # JPA entities & enums
└── infrastructure/        # Repositories, gateways, config, security
```

### Modules

| Module | Description |
|--------|-------------|
| `auth` | Registration, login, JWT access/refresh tokens, password reset, role-based access (GUEST/HOST/ADMIN) |
| `property` | Property CRUD, address management with PostGIS geospatial queries, amenities, image uploads, availability rules |
| `booking` | Full booking lifecycle (PENDING → CONFIRMED/CANCELLED/COMPLETED/REJECTED), pricing engine, payment tracking |
| `payment` | Strategy-pattern payment gateway (Stripe + Mock), webhook processing, payment intents |
| `review` | Property reviews with star ratings, per-property rating summaries |
| `messaging` | Host-guest conversations via REST and WebSocket (STOMP), read receipts |
| `notification` | Multi-channel notifications (EMAIL/PUSH/IN_APP) using the transactional outbox pattern with RabbitMQ |
| `search` | Elasticsearch integration for full-text property search (conditionally enabled) |
| `common` | Cross-cutting concerns: security config, exception hierarchy, base entities, file storage, API response wrappers |

### Domain Model

```
┌─────────┐     ┌──────────┐     ┌──────────────┐
│  User   │────<│ Property  │────<│   Booking    │
│ (auth)  │     │ (property)│     │  (booking)   │
└─────────┘     └──────────┘     └──────────────┘
     │               │                     │
     │          ┌────┴────┐               │
     │          │ Address │               │
     │          │ Amenity │          ┌────┴──────────┐
     │          │ Image   │          │PaymentTransaction│
     │          └─────────┘          └───────────────┘
     │
     ├────<┌───────────────┐     ┌──────────────┐
     │     │ Conversation  │────<│   Message    │
     │     │ (messaging)   │     │ (messaging)  │
     │     └───────────────┘     └──────────────┘
     │
     ├────<┌──────────────┐
     │     │Notification  │
     │     │(notification)│
     │     └──────────────┘
     │
     └────<┌───────┐
           │Review │
           │(review)│
           └───────┘
```

All entities extend `BaseEntity` (UUID PK, `createdAt`, `updatedAt` via JPA auditing). `Property` extends `SoftDeletableEntity` with a `deletedAt` field for soft deletes.

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Run Infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL (with PostGIS), Redis, RabbitMQ, and Elasticsearch.

### Run the Application

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

### API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT tokens |
| POST | `/api/auth/refresh` | Refresh access token |
| DELETE | `/api/auth/logout` | Logout (invalidates refresh token) |
| POST | `/api/auth/forgot-password` | Request password reset token |
| POST | `/api/auth/reset-password` | Reset password using token |

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user profile |
| PATCH | `/api/users/me` | Update profile |
| POST | `/api/users/me/change-password` | Change password |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/users` | List all users (paginated) |
| PATCH | `/api/admin/users/{id}/role` | Update user role |
| PATCH | `/api/admin/users/{id}/verify` | Verify user |

### Properties
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/properties` | Create property |
| GET | `/api/properties` | List/search properties (paginated) |
| GET | `/api/properties/{id}` | Get property by ID |
| PATCH | `/api/properties/{id}` | Update property |
| DELETE | `/api/properties/{id}` | Soft-delete property |
| POST | `/api/properties/{id}/images` | Upload property image |
| DELETE | `/api/properties/images/{imageId}` | Delete image |
| GET | `/api/properties/{id}/availability` | Get availability rules |
| POST | `/api/properties/{id}/availability` | Add availability rule |

### Search
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/search` | Search properties with filters |

### Bookings
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bookings` | Create booking |
| GET | `/api/bookings` | List user's bookings |
| GET | `/api/bookings/{id}` | Get booking details |
| PATCH | `/api/bookings/{id}/cancel` | Cancel booking |
| PATCH | `/api/bookings/{id}/confirm` | Confirm booking (host) |
| PATCH | `/api/bookings/{id}/reject` | Reject booking (host) |
| PATCH | `/api/bookings/{id}/complete` | Complete booking (host) |

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/intent` | Create payment intent |
| POST | `/api/payments/webhook` | Payment webhook (Stripe) |

### Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reviews` | Create review |
| GET | `/api/reviews/property/{propertyId}` | List reviews for property |
| GET | `/api/reviews/property/{propertyId}/rating` | Get rating summary |

### Messaging
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/messages/conversations` | Create conversation |
| GET | `/api/messages/conversations` | List conversations |
| POST | `/api/messages/conversations/{id}/messages` | Send message |
| GET | `/api/messages/conversations/{id}/messages` | List messages (paginated) |
| PATCH | `/api/messages/conversations/{id}/read` | Mark as read |
| WS | `/ws` | WebSocket message handler (STOMP) |

### Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications` | List notifications (paginated) |
| GET | `/api/notifications/unread-count` | Get unread count |
| PATCH | `/api/notifications/{id}/read` | Mark as read |

### Amenities
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/amenities` | List all amenities |

## Security

The application uses stateless JWT-based authentication:
- Access tokens: 15-minute expiry
- Refresh tokens: 7-day expiry, stored hashed in the database
- Password hashing: BCrypt
- Role-based access: GUEST, HOST, ADMIN
- Public endpoints: auth, property listing, amenity listing, reviews by property

## Database Schema

Managed by Flyway migrations (9 versions). Key entities:

- **users** — Authentication & profile data with role enum (GUEST/HOST/ADMIN)
- **properties** — Rental listings with PostGIS geography for location queries
- **bookings** — Reservation lifecycle with status machine and pricing breakdown
- **payment_transactions** — Payment audit trail with provider-agnostic design
- **reviews** — One review per booking, with 1-5 star rating
- **conversations/messages** — Host-guest threaded messaging
- **notifications** — Multi-channel notification delivery with outbox pattern
- **refresh_tokens / password_reset_tokens** — Secure token storage, hashed

## Configuration

Key configuration properties (in `application.properties`):

| Property | Description | Default |
|----------|-------------|---------|
| `harbr.jwt.secretKey` | Base64-encoded JWT signing key | (set) |
| `harbr.jwt.accessTokenExpiration` | Access token TTL in ms | 900000 (15 min) |
| `harbr.jwt.refreshTokenExpiration` | Refresh token TTL in ms | 604800000 (7 days) |
| `harbr.payment.provider` | Payment gateway mode | mock |
| `harbr.search.elasticsearch.enabled` | Toggle Elasticsearch integration | false |
| `harbr.storage.local.base-path` | Local file upload directory | ./uploads |

## License

Private — All rights reserved.
