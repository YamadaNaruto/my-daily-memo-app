# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A Spring Boot web app for keeping a daily diary/memo. Users register and log in, write one entry per day (title + text + optional image), browse past entries by year/month, and receive a daily web-push reminder. Server-rendered with Thymeleaf; data in MySQL.

Stack: Spring Boot 4.0.6, Java 26 (toolchain), Gradle, Spring Data JPA, Spring Security, Thymeleaf, MySQL, `nl.martijndwars:web-push` for push notifications.

## Commands

Uses the Gradle wrapper (`./gradlew`); no local Gradle needed.

```bash
./gradlew bootRun          # run the app on :8080 (needs MySQL reachable — see below)
./gradlew build            # compile + run tests + assemble
./gradlew test             # run all tests (JUnit Platform)
./gradlew bootJar          # build the runnable jar into build/libs/
./gradlew test --tests 'org.example.mydailymemoapp.MyDailyMemoAppApplicationTests'  # single test class
```

Run the full stack (app + MySQL 8) with Docker, which wires the DB env vars for you:

```bash
docker compose up --build
```

`bootRun` alone expects MySQL on `localhost:3306` with database `memo_app` (see `application.properties`). `docker compose` overrides the datasource to point at the `db` service. `spring.jpa.hibernate.ddl-auto=update`, so schema is created/migrated automatically on startup — there are no migration scripts.

## Architecture

All Java lives in a single package: `src/main/java/org/example/mydailymemoapp/`.

- **`main.java`** — the sole `@Controller`. Note the class is literally named `main` (lowercase). It handles every route: auth pages, `/home`, `/upload`, `/history`, `/diary/{id}`, and the push `/subscribe` endpoint. The `@Scheduled` daily-reminder job also lives here.
- **Entities**: `Diary` (title, content, imagePath, `@CreationTimestamp createdAt`), `User` (unique username, bcrypt password), `PushSubscription` (endpoint + p256dh/auth keys).
- **Services / Repositories**: `DiaryService`/`DiaryRepository`, `UserService`/`UserRepository`, `PushSubscriptionRepository`. Repositories are Spring Data JPA interfaces relying on derived query methods (e.g. `findFirstByCreatedAtBetween`, `findByCreatedAtBetweenOrderByCreatedAtDesc`).
- **Config**: `SecurityConfig` (filter chain), `WebConfig` (static resource handler), `MyDailyMemoAppApplication` (entry point, `@EnableScheduling`).

### Key architectural facts (read before changing behavior)

- **Diaries are global, not per-user.** `Diary` has no relationship to `User`. `/home`, `/history`, and `/upload` operate on all diaries in the DB regardless of who is logged in — the logged-in principal is never consulted when reading or writing entries. Login only gates access to the pages. Any feature that needs per-user diaries must add ownership to the model and filter/scope queries.
- **"Today's entry" is derived by timestamp, not stored as a date.** `DiaryService.findByDate` queries `createdAt` in the `[startOfDay, nextDay)` window. There is no uniqueness constraint, so multiple entries can exist for one day; `/home` shows the first one found.

### Image uploads

`/upload` writes files to a local `uploads/` directory (created if missing) using a random UUID filename, and stores the relative path `uploads/<uuid.ext>` in `Diary.imagePath`. Files are served two ways: `WebConfig` maps `/uploads/**` to `file:uploads/`, and `application.properties` also adds `file:uploads/` to static locations. The `uploads/` folder is committed to git and is host-local — it is **not** a Docker volume, so uploaded images do not persist across container rebuilds.

### Web push notifications

- Client: `static/js/push.js` requests permission, registers `static/sw.js`, subscribes via the VAPID public key, and POSTs the subscription to `/subscribe`. `sw.js` renders incoming pushes.
- Server: `/subscribe` (CSRF-exempt in `SecurityConfig`, `@ResponseBody`) dedupes by endpoint and persists the subscription.
- A `@Scheduled(cron = "0 30 8 * * *")` job in `main.java` sends a reminder to every stored subscription each morning at 08:30 (server time), registering BouncyCastle at send time.
- VAPID keys are configured in `application.properties` (`vapid.public.key` / `vapid.private.key`) and the same public key is hardcoded in `push.js` — keep the two in sync if you rotate them.

### Security

Form login (`/login`), `/register` and `/login` are public, everything else requires auth. Passwords hashed with `BCryptPasswordEncoder`. `UserDetailsServiceImpl` loads users from the DB and assigns role `USER`. CSRF is on everywhere except `/subscribe`.

## Notes

- The DB password, VAPID private key, and `private.pem`/`public.pem` are currently committed in the repo. Treat these as real secrets when working with the code.
- Templates are in `src/main/resources/templates/` (`home`, `history`, `login`, `register`, `diary/detail`); shared CSS in `static/css/style.css`. Much of the UI text and code comments are in Japanese.
