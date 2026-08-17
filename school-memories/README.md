# Keepsake — the school memory community

**Thirty questions. One school. A permanent archive of the people we were.**

Keepsake turns the classic WhatsApp "school memory challenge" into a permanent digital
experience. People create accounts, join their school, answer one question a day for
30 days, and every answer becomes a permanent, shareable memory that their classmates
can like, comment on, and follow — leaving the school with a lasting digital archive.

It sits somewhere between **Instagram, a digital yearbook, a school reunion platform,
and a memory archive** — with its own identity: warm, editorial, and quiet rather than
loud and competitive.

---

## What's built (Version 1 — the MVP)

| Area | Status |
| --- | --- |
| Authentication (register, login, logout, email verify, password reset, JWT, roles) | ✅ |
| Google sign-in (Identity Services ID tokens, verified locally against Google JWKS) | ✅ |
| User profiles (photo, name, nickname, school, set, grad year, bio, stats) | ✅ |
| 30-day challenge (configurable questions, answer any day, edit/delete, progress) | ✅ |
| Memory posts (text, photo/video upload, mood, soft-delete) | ✅ |
| Home feed (chronological, author/set/day/question/answer/media/reactions) | ✅ |
| Likes/reactions + comments (with counts and optimistic UI) | ✅ |
| Follow system (follow, unfollow, followers, following, suggested classmates) | ✅ |
| Notifications (likes, comments, follows, achievements) | ✅ |
| Achievements (9 badges: First Memory, Storyteller, streaks, Photographer, etc.) | ✅ |
| Leaderboards (memories, likes, comments, longest streak) | ✅ |
| Classmate search (name, nickname, set, graduation year) | ✅ |
| Admin panel (users, schools/sets, questions CRUD, memory/comment moderation, stats, announcements) | ✅ |
| Announcements (published by admins, shown in the feed) | ✅ |
| Docker + Docker Compose | ✅ |
| Integration tests (auth, challenge, social, admin) | ✅ |

**Planned but deliberately not built yet** (marked in the roadmap at the bottom, not faked):
real SMTP email, Guess-Who/Trivia/Bingo games, polls, throwback photo
contests, Memory of the Week, digital yearbook generation, time capsules, reunion
planning, interactive school maps, multi-school theming, PDF generation.

---

## Tech stack

- **Backend** — Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, PostgreSQL 16
- **Frontend** — Next.js 14 (App Router), TypeScript, Tailwind CSS, TanStack Query
- **Infrastructure** — Docker + Docker Compose; uploads stored on the local filesystem via a swappable storage service

Dependencies were kept deliberately small: no Lombok (plain accessors), no Redux, no
axios (a thin fetch wrapper), no animation library (CSS transitions). Every library
earns its place.

---

## Running it

### Option A — Docker (recommended for a quick look)

```bash
docker-compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080

### Option B — Local development

**1. Database (PostgreSQL 16)**

```bash
# any PostgreSQL 16 instance works; this creates a local one
initdb -D .dev/pgdata -U keepsake --auth=trust -E UTF8
pg_ctl -D .dev/pgdata -l .dev/pg.log -o "-p 5433" start
createdb -p 5433 -U keepsake keepsake
```

**2. Backend** (Java 21 + Maven)

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5433/keepsake \
DB_USER=keepsake DB_PASSWORD=keepsake \
mvn spring-boot:run
```

**3. Frontend** (Node 22)

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

The frontend proxies `/api/*` and `/uploads/*` to the backend, so no CORS setup is
needed locally.

### Google sign-in (optional)

Set `GOOGLE_CLIENT_ID` on the backend (or in `docker-compose.yml`) to a Google
OAuth client ID from the [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
(Web application type; authorized JavaScript origin = your frontend URL). The
login and register pages then show a "Continue with Google" button — the
frontend fetches `/api/auth/oauth-config` to discover whether it's enabled.
Google ID tokens are verified locally against Google's JWKS (cached 24h), so no
network call is needed per login. New Google accounts pick their school on a
one-time onboarding screen; sign-in joins an existing account with the same email.

### Demo accounts

On first boot the backend seeds a demo school, 30 questions, 8 classmates with sample
memories, and an admin:

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@greenfield.demo` | `password123` |
| Classmate | `ada@greenfield.demo` (also `bisi@`, `chidi@`, `dani@`, `emeka@`, `fatima@`, `george@`, `hana@`) | `password123` |

Email verification and password-reset links are logged to the backend console (no SMTP
in the MVP); the verify/reset pages work end-to-end against those links.

---

## Project structure

```
school-memories/
├── backend/                      # Spring Boot REST API
│   └── src/main/java/com/keepsake/backend/
│       ├── auth/                 # register/login/verify/reset + JWT
│       ├── user/                 # profiles, search, suggested, follow wiring
│       ├── school/               # schools + graduating sets
│       ├── challenge/            # the 30 questions + timeline/progress
│       ├── memory/               # memories, reactions, comments (the social core)
│       ├── social/               # follow entity
│       ├── notification/         # in-app notifications
│       ├── achievement/          # badge definitions, unlock rules, leaderboards
│       ├── admin/                # admin management + moderation API
│       ├── announcement/         # school-wide announcements
│       ├── security/             # JWT filter, security config
│       ├── config/               # demo data seeder, static file serving
│       └── common/               # errors, pagination envelope, uploads, mail
│   └── src/test/java/            # integration tests (H2)
├── frontend/                     # Next.js App Router
│   └── src/
│       ├── app/                  # routes (landing, auth, challenge, feed, profile, admin…)
│       ├── components/           # MemoryCard, LikeButton, CommentSection, Nav…
│       └── lib/                  # typed API client, auth context, formatting
├── docker-compose.yml
└── README.md
```

---

## Architecture decisions (the "why")

1. **One question per day per user, but no lockstep.** Users can answer Day 12 before
   Day 2 and go back to old days whenever. The brief said *"do not unnecessarily prevent
   users from completing older days"* — the constraint is only `(user, day)` uniqueness,
   enforced by the database.

2. **The memory is the post.** There is no separate "challenge answer" and "feed post"
   — submitting an answer creates a `memory` row that *is* the social object. That keeps
   likes/comments/notifications consistent and is why "the questions are just the
   mechanism" holds in the data model too.

3. **Soft deletion everywhere.** Memories and comments carry `is_deleted` so moderation
   can hide content without destroying the reaction/comment/notification history, and so
   a restore is one flag flip.

4. **JWT in `localStorage`, not cookies.** Simple, works on any host, matches a
   separate frontend/backend deployment. The trade-off (XSS risk) is documented; the
   upgrade path is an `httpOnly` cookie + CSRF handling, which can be added without
   changing the API surface.

5. **Dev-mode mail.** `MailService` logs verification/reset links when `MAIL_ENABLED`
   is false (the default), so the full flows are testable without SMTP. A real sender
   (SMTP/SES/Postmark) drops in behind the same interface.

6. **Files on disk via `FileStorageService`.** Media URLs are stored in Postgres; the
   bytes live under `UPLOAD_DIR` and are served at `/uploads/*`. Swapping to S3/GCS is
   a change to one class. Content types are whitelisted and sizes capped at 15 MB.

7. **Configurable questions from day one.** The 30 questions are seed data, not code —
   admins can add/rename/reorder/hide days from the admin panel. The timeline endpoint
   reads them in order.

8. **Achievements are evaluated, not stored eagerly.** After every submission the
   service recomputes a user's stats and unlocks anything newly earned — idempotent,
   so re-runs are harmless and definitions are easy to extend.

9. **No hardcoded demo content in the UI.** The seeder populates the database only;
   the frontend renders whatever is there. Empty states are designed for and shown when
   appropriate.

10. **Security model.** Stateless JWT with role claims, `ROLE_ADMIN` guard on
    `/api/admin/**` at the filter level, ownership checks on edit/delete in services,
    BCrypt password hashing, bean validation on all inputs, and a global error handler
    that never leaks stack traces.

---

## API overview

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/register` · `POST /api/auth/login` · `POST /api/auth/logout` · `GET /api/auth/verify-email` · `POST /api/auth/forgot-password` · `POST /api/auth/reset-password` · `GET /api/auth/me` |
| Schools | `GET /api/schools` · `GET /api/schools/{id}/sets` |
| Challenge | `GET /api/challenge` (timeline + progress) · `GET /api/challenge/day/{day}` |
| Memories | `GET /api/memories` (feed) · `GET/PATCH/DELETE /api/memories/{id}` · `POST /api/memories` (multipart) |
| Social | `POST /api/memories/{id}/reactions` · `GET/POST /api/memories/{id}/comments` · `DELETE .../comments/{cid}` |
| Users | `GET /api/users/{handle}` · `GET /api/users/{id}/memories` · `POST /api/users/{id}/follow` · `POST /api/users/{id}/unfollow` · `GET /api/users/search` · `GET /api/users/suggested` · `PATCH /api/users/me` · `POST /api/users/me/avatar` |
| Notifications | `GET /api/notifications` · `GET /api/notifications/unread-count` · `PATCH /api/notifications/{id}/read` · `PATCH /api/notifications/read-all` |
| Achievements | `GET /api/achievements/me` · `GET /api/leaderboards?type=memories\|likes\|comments\|streak` |
| Announcements | `GET /api/announcements` |
| Admin | `GET /api/admin/stats` · users/schools/questions/memories/comments/announcements management |

All list endpoints return a uniform `{ content, page, size, totalElements, totalPages }`
envelope.

---

## Tests

```bash
cd backend && mvn test
```

Integration tests cover the critical flows end-to-end against an in-memory H2:

- **Auth**: register → verify → login → protected route; duplicate email; wrong password; reset flow
- **Challenge**: 30 seeded questions; submit → duplicate rejection; day view
- **Social**: like → comment → counts update → unlike
- **Admin**: RBAC (403 for regular users), question creation, announcements

The API was also verified manually against a live PostgreSQL instance for register,
challenge submission, feed, likes, comments, uploads (avatar + photo), password reset,
follow, search, notifications, achievements, and admin moderation.

---

## Design direction

The interface is intentionally **not** a generic dashboard: warm paper background,
a serif display face (Fraunces) for editorial moments, hairline rules, numbered
"Day 01" typography, subtle fade-ups, and generous whitespace. No gradient heroes,
no glassmorphism, no emoji overload. The emotional target is *nostalgia + community +
warmth + modernity*, and every screen (including empty states and the challenge
timeline) is built to pull people toward reading and writing memories.

---

## Roadmap

**Version 1 — shipped (this repo):** auth, profiles, 30-day challenge, memory
submissions, feed, likes, comments, uploads, follow, notifications, achievements,
leaderboards, search, admin dashboard, Docker, tests, docs.

**Version 2 — planned:** real email delivery, memory games (Guess Who,
Guess the Teacher, School Trivia, School Bingo), polls, throwback photo contest,
Memory of the Week, yearbook builder (v1), better moderation tooling, media pipeline
improvements (thumbnails, video transcoding, S3 storage).

**Version 2 — shipped so far:** Google OAuth (local JWKS verification, button on
login/register, one-time school onboarding).

**Version 3 — planned:** digital yearbook PDF, time capsules, reunion planning
(countdown + RSVP), interactive school map with memories pinned to locations, advanced
analytics, multi-school theming.

---

## Known MVP limitations (honest list)

- Email is log-only; no real delivery yet.
- No CSRF concerns because auth is header-based; JWT lives in `localStorage` (see
  decision #4).
- Feed pagination exists but the UI loads one page; profile loads all memories.
- No WebSockets — notifications poll every 60s.
- Admin "users" search filters the current page server-side rather than a full-text
  query (fine at school scale).
- `ddl-auto: update` is used for schema in this MVP; Flyway/Liquibase migrations are
  the recommended upgrade before production.
