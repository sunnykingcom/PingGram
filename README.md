# PingGram

A production-quality, Telegram/WhatsApp-like real-time messaging web app.

**Stack:** Java 21 · Spring Boot 3.3 · Spring Security (JWT) · Spring WebSocket/STOMP · MySQL 8 · Vanilla JS frontend

---

## Project Structure

```
PingGram/
├── index.html                  ← Frontend (single-page app, green hyperrealistic theme)
├── docker-compose.yml          ← MySQL + Backend containers
└── backend/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/pinggram/
        ├── PingGramApplication.java
        ├── config/
        │   ├── SecurityConfig.java      JWT filter chain, CORS
        │   └── WebSocketConfig.java     STOMP broker, JWT auth interceptor
        ├── controller/
        │   ├── AuthController.java      POST /api/auth/register|login, GET /api/auth/me
        │   ├── ChatController.java      GET|POST /api/chats, POST /api/chats/{id}/read
        │   └── MessageController.java   GET|POST /api/chats/{id}/messages, GET /api/messages/undelivered
        ├── service/
        │   ├── AuthService.java         Register/login logic, online status
        │   ├── ChatService.java         Create/list chats, read-receipts
        │   └── MessageService.java      Send (idempotent), deliver, read, offline sync
        ├── entity/
        │   ├── User.java               id, displayName, email, passwordHash, online
        │   ├── Chat.java               id, name, isGroup, members, messages
        │   ├── ChatMember.java         chat↔user join, role, lastReadAt
        │   └── Message.java            id, uniqueId, content, status (SENT|DELIVERED|READ)
        ├── repository/             Spring Data JPA repositories
        ├── dto/                    Records: AuthRequest/Response, ChatDto, MessageDto …
        ├── security/               JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
        └── exception/              GlobalExceptionHandler, custom exceptions
```

---

## Quick Start (Docker Compose — easiest)

```bash
# 1. Clone / open the project
cd PingGram

# 2. Start MySQL + Spring Boot backend
docker compose up --build

# 3. Open the frontend
open index.html          # macOS
# or serve it with any static server:
npx serve .
```

The backend will auto-create all tables via `spring.jpa.hibernate.ddl-auto=update`.

---

## Manual Start (local dev)

### Prerequisites
- Java 21, Maven 3.9+
- MySQL 8 running locally

### Database
```sql
CREATE DATABASE pinggram CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Run the backend
```bash
cd backend
mvn spring-boot:run
```

### Open the frontend
Serve `index.html` from any static server (e.g. VS Code Live Server, `npx serve .`).
The frontend connects to `http://localhost:8080`.

---

## API Reference

### Auth

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | `{displayName, email, password}` | Register, returns JWT + user |
| POST | `/api/auth/login` | `{email, password}` | Login, returns JWT + user |
| GET | `/api/auth/me` | — | Current user (requires Bearer token) |

### Chats

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/chats` | List all chats for current user |
| POST | `/api/chats` | Start a 1-to-1 chat `{recipientEmail}` |
| POST | `/api/chats/{id}/read` | Mark all messages read |

### Messages

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/chats/{id}/messages` | Fetch history (sorted by timestamp ASC) |
| POST | `/api/chats/{id}/messages` | Send `{content, uniqueId}` — idempotent |
| GET | `/api/messages/undelivered` | Offline sync: returns + marks delivered |

### WebSocket (STOMP over SockJS)

```
Endpoint:  ws://localhost:8080/ws
Connect header:  Authorization: Bearer <token>

Subscribe:
  /topic/chat.{chatId}      → real-time messages in a chat
  /user/queue/messages      → personal incoming messages
  /user/queue/receipts      → delivery/read receipts {messageId, status}
```

---

## Key Design Decisions

### Message lifecycle
`SENT` → `DELIVERED` → `READ`

1. **SENT** — message persisted in DB, broadcast to `/topic/chat.{chatId}`.
2. **DELIVERED** — when the recipient's WebSocket session receives it (or pulls offline queue on reconnect). Sender notified via `/user/queue/receipts`.
3. **READ** — when recipient opens the chat. `POST /api/chats/{id}/read` triggers bulk update + receipt to sender.

### Idempotency
Every send request includes a client-generated `uniqueId` (UUID). The server checks for an existing message with that UUID before inserting, preventing duplicates on network retries.

### Offline sync
If a user is offline, messages stay in DB with `status=SENT`. On reconnect, the client calls `GET /api/messages/undelivered` which atomically fetches and marks them `DELIVERED`.

### Retry logic
`MessageService.sendMessage` is annotated `@Retryable(maxAttempts=3, backoff=@Backoff(delay=200, multiplier=2))` — transparent exponential-backoff retries for transient DB/network errors.

### Scaling WebSocket
For multiple backend instances, replace `enableSimpleBroker` with a Redis or RabbitMQ STOMP broker relay:
```java
// In WebSocketConfig:
config.enableStompBrokerRelay("/topic", "/queue")
    .setRelayHost("localhost")
    .setRelayPort(61613);  // RabbitMQ STOMP port
```
This lets any instance broadcast to all subscribers regardless of which server holds the WS session.

---

## Running Tests

```bash
cd backend
mvn test
```
Tests use H2 in-memory database (profile `test`). Covers register, duplicate email, bad credentials.

---

## Free External APIs (optional integrations)

| Service | Use | Notes |
|---------|-----|-------|
| **Firebase FCM** | Push notifications | Free, unlimited. Use `firebase-admin` SDK |
| **OneSignal** | Web/mobile push | Free: unlimited mobile, 10k web/month |
| **TextBelt** | SMS | Free (1 SMS/day). POST `https://textbelt.com/text` |
| **Cloudinary** | File/image storage | Free tier, Java SDK upload |
| **Nominatim (OSM)** | Geocoding | Free, fair-use. GET `https://nominatim.openstreetmap.org/search` |

---

## Environment Variables (production)

| Variable | Description |
|----------|-------------|
| `APP_JWT_SECRET` | Base64-encoded 256-bit secret |
| `APP_JWT_EXPIRATION_MS` | Token TTL in ms (default 86400000 = 24h) |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
