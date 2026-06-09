# Entity Update Events (SSE)

TIMAAT broadcasts entity lifecycle events to connected browser clients via Server-Sent Events (SSE). This allows the UI to react to changes made in other browser sessions without polling.

## Architecture overview

```
Backend service                     Frontend
─────────────────────────────────   ──────────────────────────────────
EntityUpdateEventService            TIMAAT.EntityUpdate
  │  SseBroadcaster                   │  EventSourcePolyfill
  │                                   │
  ├── sendEntityCreateMessage()   ←→  │  addEventListener("transcription", …)
  ├── sendEntityChangeMessage()        │  addEventListener("medium-audio-analysis", …)
  └── sendEntityDeleteMessage()        └  addEventListener("medium", …)
          │
          │ SSE  GET /api/entity-update-events
          └──────────────────────────────────────────────────────────▶
```

The `EntityUpdateEventService` is a HK2 singleton bound in `TIMAATBinder`. It is injected into any backend service that needs to broadcast events. The `EndpointEntityUpdateEvents` REST endpoint (`GET /entity-update-events`) accepts SSE connections from authenticated clients and registers their sink with the broadcaster.

## Message types

Every SSE event data payload is a JSON-serialised `EntityUpdateMessage` subclass. All subtypes share these base fields:

| Field  | Type                    | Description                             |
|--------|-------------------------|-----------------------------------------|
| `type` | `EntityUpdateMessageType` | Discriminator: `CREATE`, `CHANGE`, or `DELETE` |
| `id`   | varies                  | Primary key of the affected entity      |

### `EntityCreateMessage` (`type: "CREATE"`)

Signals that a new entity was persisted. Carries the full entity so the client can insert it into local state without an additional fetch.

```json
{
  "type": "CREATE",
  "id": 42,
  "entity": { /* full entity DTO */ }
}
```

### `EntityChangeMessage` (`type: "CHANGE"`)

Signals that an existing entity was modified. Carries the updated entity (or a partial update DTO) so the client can replace its local copy.

```json
{
  "type": "CHANGE",
  "id": 42,
  "entity": { /* updated entity or partial DTO */ }
}
```

### `EntityDeleteMessage` (`type: "DELETE"`)

Signals that an entity was removed. Only the primary key is transmitted — the entity no longer exists in the database.

```json
{
  "type": "DELETE",
  "id": 42
}
```

## Entity types and SSE event names

The SSE protocol distinguishes events by name (the `event:` field in the stream). On the backend, every `EntityType` enum value maps to an event name string:

| `EntityType` enum constant  | SSE event name          |
|-----------------------------|-------------------------|
| `TRANSCRIPTION`             | `transcription`         |
| `MEDIUM_AUDIO_ANALYSIS`     | `medium-audio-analysis` |
| `MEDIUM`                    | `medium`                |

## Sending an update from a backend service

Inject `EntityUpdateEventService` and call the appropriate method:

```java
@Inject
private EntityUpdateEventService entityUpdateEventService;

// after creating a new entity
entityUpdateEventService.sendEntityCreateMessage(EntityType.TRANSCRIPTION, created.getId(), transcriptionDto);

// after updating an entity
entityUpdateEventService.sendEntityChangeMessage(EntityType.TRANSCRIPTION, id, updatedDto);

// after deleting an entity
entityUpdateEventService.sendEntityDeleteMessage(EntityType.TRANSCRIPTION, id);
```

Each method is a no-op when no client has connected yet, so callers do not need to guard the call.

## Frontend subscription

`TIMAAT.EntityUpdate` (`js/timaat.entityupdate.js`) manages the `EventSourcePolyfill` connection.

**Initialise the connection** (called once after login):

```js
TIMAAT.EntityUpdate.initEntityUpdate();
```

This opens an SSE connection to `api/entity-update-events` with the JWT Bearer token and registers all previously queued listeners.

**Register a listener** (may be called before or after `initEntityUpdate`):

```js
TIMAAT.EntityUpdate.registerEntityUpdateListener('transcription', function(message) {
    // message is the parsed JSON object (EntityCreateMessage / EntityChangeMessage / EntityDeleteMessage)
    if (message.type === 'CREATE') { /* … */ }
    if (message.type === 'CHANGE') { /* … */ }
    if (message.type === 'DELETE') { /* … */ }
});
```

Use the SSE event name (see table above) as the first argument. The callback receives the already-parsed JSON object.

## Keep-alive pings

`EntityUpdateEventService` schedules a daemon thread that broadcasts a `ping` event every 10 seconds to all connected clients. This prevents proxies and browsers from closing idle SSE connections. Clients should silently ignore `ping` events.

## Adding a new entity type

1. Add a constant to `EntityType` with the desired SSE event name string.
2. Inject `EntityUpdateEventService` in the backend service that owns the entity's lifecycle.
3. Call `sendEntityCreate/Change/DeleteMessage` at the relevant lifecycle points.
4. Register a listener in the frontend module responsible for that entity with `TIMAAT.EntityUpdate.registerEntityUpdateListener`.