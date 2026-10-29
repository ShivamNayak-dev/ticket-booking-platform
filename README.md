# Ticket Booking Service

A ticket booking application with a **Spring Boot** REST backend and a **React** frontend.

- JWT authentication stored in an **HttpOnly cookie**
- Role-based authorization with **USER, ORGANIZER, and ADMIN** roles
- Event management (create / list / details) with **category and date-range filters** and pagination
- Only **ORGANIZER and ADMIN** roles can create events; normal users cannot create events
- Ticket booking with **pessimistic locking** so the same seat can never be sold twice
- **Redis caching** with a different TTL per cache
- **Waitlist** for sold-out events with queue position tracking (bookings are promoted automatically when someone cancels)
- Validation on every input and one consistent JSON response format

```text
ticket-booking-service/
├── backend/             Spring Boot 3 (Java 17), Maven
├── frontend/            React 18 + Vite
├── database/schema.sql  PostgreSQL schema
├── docker-compose.yml   optional: PostgreSQL + Redis in one command
└── README.md
```

## 1. Requirements

| Tool | Version |
|------|---------|
| JDK | 17 or newer |
| Maven | 3.9+ |
| Node.js | 18+ |
| PostgreSQL | 14+ |
| Redis | 6+ |

## 2. Start PostgreSQL and Redis

**Option A - Docker (easiest)**

```bash
docker compose up -d
```

This starts PostgreSQL (database `ticket_booking`, user `postgres`, password `postgres`, schema loaded automatically) and Redis on the default ports.

**Option B - already installed locally**

```bash
createdb -U postgres ticket_booking
psql -U postgres -d ticket_booking -f database/schema.sql
redis-server
```

> Running `schema.sql` is optional. The backend uses `spring.jpa.hibernate.ddl-auto=update` and creates the tables itself if they are missing. The file is included so the schema can be reviewed and loaded by hand.

## 3. Run the backend

All settings (database, Redis, JWT secret, cache TTLs) live in
`backend/src/main/resources/application.properties`.
That file is listed in `.gitignore` so real credentials never reach Git.
A template with placeholder values is committed as `application.properties.example`:

```bash
cd backend
# only needed if application.properties does not exist yet (for example after a fresh git clone)
cp src/main/resources/application.properties.example src/main/resources/application.properties
# then edit the database user/password if yours are different

mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

On the first start an empty database is filled with a demo organizer and seven events
(set `app.seed.enabled=false` to switch that off):

```text
email:    demo@ticketbook.com
password: Demo@1234
role:     ORGANIZER
```

The demo organizer can create events. To test an ADMIN account, register an account through the application and promote it using a trusted PostgreSQL administrator account:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-email@example.com';
```

Replace `your-email@example.com` with the registered account's email. Use the password you chose during registration. The SQL statement changes the role, not the password.

## 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173/**. Vite forwards every `/api` request to `localhost:8080`
(see `frontend/vite.config.js`), so the cookie works without extra setup.

## 5. API overview

Every response has the same shape:

```json
{ "success": true, "message": "Events fetched successfully", "data": { }, "timestamp": "2026-10-28T10:15:30" }
```

Errors:

```json
{ "success": false, "message": "Validation failed. Please check the highlighted fields.",
  "errors": { "email": "Please enter a valid email address" }, "timestamp": "..." }
```

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | no | Create account (assigns USER role, also logs in, sets cookie) |
| POST | `/api/auth/login` | no | Login, sets the HttpOnly cookie |
| POST | `/api/auth/logout` | no | Clears the cookie |
| GET | `/api/auth/me` | yes | Current user, including role |
| GET | `/api/events` | no | List events. Query: `category`, `from`, `to` (yyyy-MM-dd), `page`, `size` |
| GET | `/api/events/{id}` | no | Event details |
| POST | `/api/events` | ORGANIZER / ADMIN | Create an event |
| POST | `/api/bookings` | yes | Book tickets `{ "eventId": 1, "quantity": 2 }` |
| GET | `/api/bookings/my` | yes | My bookings |
| GET | `/api/bookings/{id}` | yes | One of my bookings |
| POST | `/api/bookings/{id}/cancel` | yes | Cancel a booking (seats go back, waitlist is served) |
| POST | `/api/waitlist` | yes | Join the waitlist of a sold-out event `{ "eventId": 1 }` |
| GET | `/api/waitlist/my` | yes | My waitlist entries with queue position |
| DELETE | `/api/waitlist/{id}` | yes | Leave the waitlist |

Status codes used: `200`, `201`, `400` (validation / bad input), `401` (not logged in / wrong credentials),
`403` (insufficient role or forbidden resource access), `404`, `409` (sold out, duplicate email, already waiting, ...), `500`.

### Role-based authorization

| Role | Permissions |
|------|-------------|
| USER | Browse events, book tickets, cancel bookings, and manage their own waitlist entries |
| ORGANIZER | USER permissions plus event creation |
| ADMIN | Event creation and other administrative permissions explicitly granted by the backend |

Public registration always assigns the `USER` role. A user cannot select `ORGANIZER` or `ADMIN` through the registration request. Trusted administrators can grant elevated roles through the database.

The backend enforces event-creation permissions using Spring Security. Hiding the Create Event link in the frontend is only a UI convenience; the backend also blocks unauthorized requests. A logged-in USER attempting `POST /api/events` receives `403 Forbidden`.

### Quick try with curl

```bash
# login and keep the cookie
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@ticketbook.com","password":"Demo@1234"}'

# filter events
curl "http://localhost:8080/api/events?category=MUSIC&from=2026-11-01&to=2026-12-31"

# book 2 tickets
curl -b cookies.txt -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" -d '{"eventId":1,"quantity":2}'
```

### Testing the double-booking protection

The seeded event **Intimate Jazz Night** has only 2 seats. Find its ID in the event list, then fire several requests at the same time:

```bash
EVENT_ID=7   # replace with the real id
seq 1 6 | xargs -P 6 -I{} curl -s -o /dev/null -w "%{http_code}\n" -b cookies.txt \
  -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" -d "{\"eventId\":$EVENT_ID,\"quantity\":1}"
```

Expected: exactly two `201` and four `409`, provided the event starts with two available seats and the requests use valid booking credentials. The event then shows as sold out, another eligible user can join the waitlist, and cancelling a booking promotes that user automatically.

## 6. How it works

### Authentication and authorization

Register/login create a JWT (HS256, 24h by default) and send it back in a cookie that is
`HttpOnly` (not readable by JavaScript), `SameSite=Lax`, and `Secure` when `app.cookie.secure=true`.
`JwtAuthenticationFilter` reads the cookie on every request. The frontend never sees or stores the token;
it asks `/api/auth/me` on page load to learn whether the user is logged in. Passwords are hashed with BCrypt.

The `User` entity stores the account role (`USER`, `ORGANIZER`, or `ADMIN`). During authentication, the JWT filter loads the user's role from the database and supplies the corresponding Spring Security authority. The security configuration permits event browsing publicly but restricts `POST /api/events` to `ORGANIZER` and `ADMIN`.

The frontend hides the Create Event link from normal users and protects the creation route. These frontend checks improve navigation, while the backend authorization rules provide the actual security. Public registration always assigns `USER`; elevated roles must be granted through a trusted administrative process.

Demo organizer credentials:

```text
email:    demo@ticketbook.com
password: Demo@1234
```

No predefined admin email or password is seeded. Create an account normally and grant it the `ADMIN` role through PostgreSQL to test admin access.

### Seat locking (pessimistic)

`BookingService.bookTickets` runs in a transaction and loads the event with
`SELECT ... FOR UPDATE` (`EventRepository.findByIdForUpdate`). Concurrent buyers of the same event queue up
at that row lock, so each one sees the up-to-date `availableSeats`. A database `CHECK` constraint
(`available_seats >= 0`) is a second safety net. Cancelling, joining the waitlist and waitlist promotion
lock the event row in the same order (event first), which avoids deadlocks.

Simplification: the lock protects a seat **counter** per event (you book a quantity, not seat numbers like A12).

### Redis caching

| Cache | What | TTL (default) | Cleared when |
|-------|------|---------------|--------------|
| `event` | single event details | 10 min | seats are booked / released |
| `eventList` | filtered, paginated lists | 2 min | any event is created, any seat count changes |
| `userBookings` | a user's tickets | 5 min | that user books or cancels (or is promoted from the waitlist) |

TTLs are configurable in `application.properties`. Entries are removed **after the database commit**
(`CacheService`), so a reader cannot put old data back into Redis. If Redis is down, the app logs a warning and
falls back to the database.

### Waitlist

Only offered when an event is sold out. Each entry is one ticket. Position = number of people still waiting
who joined earlier + 1. When a booking is cancelled the freed seats are given, in order, to the people at
the front of the queue (a confirmed booking is created for them and their entry becomes `PROMOTED`).

### Validation

Bean Validation annotations on every request DTO, plus checks in the service layer (event in the future,
seats available, ownership of bookings, ...). `GlobalExceptionHandler` converts all errors into the common JSON format.

## 7. Backend structure

```text
com.ticketbooking
├── config          SecurityConfig, CorsConfig, CacheConfig, CacheErrorConfig, DataSeeder
├── controller      Auth, Event, Booking, Waitlist controllers
├── dto             request/ (validated inputs) response/ (ApiResponse + response records)
├── entity           User, Event, Booking, WaitlistEntry + enums (including Role)
├── exception       custom exceptions + GlobalExceptionHandler
├── repository      Spring Data JPA repositories
├── security        JwtService, JwtAuthenticationFilter, AuthCookieService, entry point
├── service         Auth, Event, Booking, Waitlist, Cache services
└── specification   dynamic filters for the event search
```

## 8. Frontend structure

```text
src/
├── api/          axios client + one file per resource
├── components/   Navbar, EventCard, EventFilters, Pagination, BookingCard, WaitlistCard, ProtectedRoute, ...
├── context/      AuthContext (login state and current user's role)
├── pages/        Home, EventDetails, Login, Register, CreateEvent, MyBookings, MyWaitlist, NotFound
├── styles/       index.css, components.css, pages.css
└── utils/        constants, formatters, error helpers
```

## 9. Known limitations

- No automated test suite is included.
- Seats are a counter per event, not individually numbered seats.
- Payments are not implemented; a booking is confirmed immediately.
- Event creation is restricted to `ORGANIZER` and `ADMIN` roles. Public registration assigns `USER`; admin privileges must be granted through a trusted process.
- Event update/delete permissions and a full admin-management interface are not documented as implemented in this version.
