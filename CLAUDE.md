# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

TIMAAT is a Jakarta EE web application for collaborative video annotation, packaged as a WAR deployed to Tomcat 10. It is the successor to FIPOP (the persistence unit and database are still named `FIPOP`). Backend is Java + Jersey; frontend is server-served HTML + RequireJS modules in `src/main/webapp/js`.

## Build & Run

- `mvn package` — builds `target/TIMAAT.war`. Deploy by copying into Tomcat's `webapps/` directory; app is then reachable at `http://localhost:8080/TIMAAT`.
- `mvn test` — runs the JUnit 5 suite. Surefire is configured with `-javaagent:.../mockito-core.jar` (see pom.xml:67) because Mockito 5 needs the agent attached on Java 21+. Don't drop that argLine.
- Run a single test: `mvn test -Dtest=FfmpegAudioEngineTest` or `mvn test -Dtest=FfmpegAudioEngineTest#methodName`.
- Java toolchain: `pom.xml` sets `maven.compiler.release=25`, but `.github/workflows/maven.yml` still pins JDK 11 — the CI config is stale relative to the source level. Local builds need JDK 25.
- Annotation processing: EclipseLink's `CanonicalModelProcessor` runs at compile time and generates JPA static metamodel classes into `target/generated-sources/annotations`.

## Runtime configuration

The app reads `~/.timaat/timaat.properties` (Linux/macOS) or `%HOMEDRIVE%%HOMEPATH%\.timaat\timaat.properties` (Windows). Template lives at `src/main/resources/timaat-default.properties`. Required keys: `database.url`, `database.user`, `database.password`, `storage.location` (filesystem root for media), `app.ffmpeg.location`, `app.imagemagick.location`. Async task pool sizing: `app.task.coreParallelCount`, `app.task.maxParallelCount`, `app.task.queueSize`.

For Docker deployments these are surfaced as env vars (`DATABASE_USER`, `DATABASE_HOST`, `APP_TASK_MAXPARALLELCOUNT`, …) — see `docs/docker.md`. `docker/timaat-entrypoint.sh` writes the properties file from env on container start.

## Database

MySQL/MariaDB schema `FIPOP`, collation `utf8mb4_general_ci`.

- Fresh install: load `src/main/resources/database/fipop.sql`.
- Upgrade: apply `src/main/resources/database/db_update.sql` (it is idempotent across versioned blocks). Schema version lives in the `db_version` table; bump it for every schema change and document the change in `docs/database.md`.
- JPA persistence unit `FIPOP-JPA` is declared in `src/main/resources/META-INF/persistence.xml`. Every entity class in `de.bitgilde.TIMAAT.model.FIPOP` must be listed there — EclipseLink does not auto-discover. When adding an entity, add a `<class>` line.

## Architecture

### Request lifecycle

`de.bitgilde.TIMAAT.TIMAATApp` (`@ApplicationPath("timaatapp")`) is the Jersey `Application`. It explicitly enumerates resource and filter classes in `addRestResourceClasses` — new endpoints must be added there or they will 404. Filters in `rest/filter/` (`AuthenticationFilter`, `CORSFilter`, `RangeResponseFilter`) wrap every request; auth is JWT (jjwt) with Argon2 password hashing.

REST endpoints live in `de.bitgilde.TIMAAT.rest.endpoint.Endpoint*` (one class per domain area: Medium, Annotation, Analysis, Actor, Music, …). They delegate to storage components rather than holding business logic themselves.

### Dependency injection

Jersey HK2 is the DI container. All wiring lives in **one** place: `de.bitgilde.TIMAAT.di.binder.TIMAATBinder`. Singletons (file storages, engines, task service, the entity-storage layer) are bound there. When adding a new service or storage, register it in `TIMAATBinder.configure()` — endpoints get instances via `@Inject` only because of bindings here. Note the multi-contract bindings (e.g. `bind(DbTaskStorage.class).to(DbTaskStorage.class).to(TaskStorage.class).to(TaskStateUpdater.class)`) used so one implementation satisfies multiple registry lookups.

`TIMAATBinder` also constructs the `EntityManagerFactory` (it has to run before the persistence layer is touched) and exposes it as a binding.

### Storage layering

Three parallel storage concepts that are easy to confuse:

- `storage/entity/**` — DB-backed entity storage classes (`MediumStorage`, `AnnotationStorage`, `TranscriptionStorage`, …). These are the JPA access layer for the FIPOP entities.
- `storage/file/**` — filesystem storage for binary blobs (`VideoFileStorage`, `AudioFileStorage`, `ImageFileStorage`, `TranscriptionFileStorage`, `TemporaryFileStorage`). They write under `storage.location` with the layout documented in `docs/fs-storage.md` (per-medium subdirectories keyed by medium ID). When that layout changes, add a migration script under `src/main/resources/scripts/fs-migration/`.
- `db/` — low-level JDBC helpers (`DbAccessComponent`) used when JPA is too heavy.

The corresponding domain models are in `model/FIPOP/` (JPA entities, generated/maintained against the schema) and `rest/model/` (DTOs used over the wire).

### Async task framework (`service/task/`)

Long-running work (audio analysis, transcription preparation) goes through this framework rather than being run inline in a REST handler.

- `api/Task` + `TaskType` + concrete `Task` subclasses describe a unit of work.
- `storage/TaskStorageRegistry` and `storage/TaskStateUpdaterRegistry` route a task to the right persistence handler by its `TaskType`. To add a new task type: define `XxxTask`, register its `TaskStorage` and `TaskStateUpdater` bindings in `TIMAATBinder`, and register a corresponding `TaskExecutor` in `execution/TaskExecutorFactory`.
- `TaskService` is the entry point — it persists the task, then hands it to `TaskExecutorService`. On startup it resumes any tasks left in a non-terminal state.
- `TaskExecutorService` owns a thread pool sized by the `app.task.*` properties.

### Transcription (`service/transcription/`)

`TranscriptionService` orchestrates speech-to-text via the external `studio.nko-dev:speech-to-text-service-client-uni-erfurt` client. It plugs into the task framework as a `TaskStateUpdater` (see binding in `TIMAATBinder.java:87`). Transcription file output goes through `TranscriptionFileStorage`; DB rows are persisted via `TranscriptionStorage`.

### Media processing (`processing/`)

`processing/audio/FfmpegAudioEngine` and `processing/video/FfmpegVideoEngine` shell out to `ffmpeg` (path from `app.ffmpeg.location`). Audio analysis produces waveform + frequency files written via the binary readers/writers in `processing/audio/io/` — these are the only components with meaningful unit-test coverage today (`src/test/java/de/bitgilde/TIMAAT/audio/io/`).

### Realtime channels

- `notification/NotificationWebSocket` — WebSocket endpoint for user-targeted push notifications; subscriptions tracked in `UserSubscriptions`.
- `sse/EntityUpdateEventService` + `rest/endpoint/EndpointEntityUpdateEvents` — Server-Sent Events stream so the UI can react to entity lifecycle changes (create/change/delete) from other sessions. `EntityUpdateEventService` is a singleton broadcaster; services inject it and call `sendEntityCreate/Change/DeleteMessage()`. Each call wraps the payload in the corresponding `EntityUpdateMessage` subclass (`EntityCreateMessage`, `EntityChangeMessage`, `EntityDeleteMessage`) and broadcasts it as an SSE event named after the `EntityType` (e.g. `transcription`, `medium-audio-analysis`). The frontend subscribes via `TIMAAT.EntityUpdate.registerEntityUpdateListener(eventName, callback)` in `js/timaat.entityupdate.js`. See `docs/entity-update-events.md` for the full protocol, message shapes, and instructions for adding new entity types.

### Publication output (`publication/`)

Generates standalone publishable bundles from annotations using template files in `src/main/resources/*.template` (registered to the WAR by the war-plugin config in `pom.xml`). `PublicationAuthenticationFilter` + `PublicationServlet` serve the protected publication endpoints; they are added to the Jersey resource set alongside the REST endpoints.

## Frontend

`src/main/webapp/` is plain HTML/CSS/JS, no build step. JS is organized as RequireJS modules under `js/` with vendor libs vendored under `third-party/` (jQuery, Bootstrap, datatables, leaflet, wavesurferjs, dropzone, summernote, select2, …). The frontend talks to the backend over the `/timaatapp/*` REST API and the SSE + WebSocket channels above.