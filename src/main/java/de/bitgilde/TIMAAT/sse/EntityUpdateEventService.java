package de.bitgilde.TIMAAT.sse;

/*
 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

import de.bitgilde.TIMAAT.sse.api.EntityChangeMessage;
import de.bitgilde.TIMAAT.sse.api.EntityCreateMessage;
import de.bitgilde.TIMAAT.sse.api.EntityDeleteMessage;
import de.bitgilde.TIMAAT.sse.api.EntityType;
import de.bitgilde.TIMAAT.sse.api.EntityUpdateMessage;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for broadcasting entity lifecycle notifications via SSE to subscribed clients.
 * Provides dedicated methods for the three event kinds — create, change, and delete — each
 * wrapping the payload in the corresponding {@link EntityUpdateMessage} subtype so the client
 * can distinguish events without inspecting the SSE event name alone.
 *
 * @author Nico Kotlenga
 * @since 31.07.25
 */
public class EntityUpdateEventService implements Closeable {

  private static final Logger logger = Logger.getLogger(EntityUpdateEventService.class.getName());
  private static final int PING_INTERVAL_IN_SECONDS = 10;

  private final ScheduledExecutorService pingExecutor;
  private volatile SseBroadcaster broadcaster;
  private volatile Sse sse;

  public EntityUpdateEventService() {
    pingExecutor = createPingExecutorService();
  }

  private void initBroadcasterIfNeeded(Sse sse) {
    if (broadcaster == null) {
      synchronized (this) {
        if (broadcaster == null) {
          this.sse = sse;
          this.broadcaster = sse.newBroadcaster();
        }
      }
    }
  }

  /**
   * Broadcasts an {@link EntityCreateMessage} carrying the newly created entity to all registered clients.
   * Does nothing when no client has registered yet.
   *
   * @param <ID_TYPE> the id type
   * @param <T>    the entity type
   * @param entityType to which the {@link EntityCreateMessage} relates to
   * @param id of the created entity
   * @param entity the newly created entity
   */
  public <ID_TYPE, T> void sendEntityCreateMessage(EntityType entityType, ID_TYPE id, T entity) {
    sendMessage(entityType, new EntityCreateMessage<>(id, entity));
  }

  /**
   * Broadcasts an {@link EntityChangeMessage} carrying the updated entity to all registered clients.
   * Does nothing when no client has registered yet.
   *
   * @param <ID_TYPE> the id typ
   * @param <T>    the entity type
   * @param entityType to which the {@link EntityChangeMessage} relates to
   * @param entity the updated entity
   */
  public <ID_TYPE, T> void sendEntityChangeMessage(EntityType entityType, ID_TYPE id, T entity) {
    sendMessage(entityType, new EntityChangeMessage<>(id, entity));
  }

  /**
   * Broadcasts an {@link EntityDeleteMessage} carrying the primary key of the deleted entity
   * to all registered clients. Does nothing when no client has registered yet.
   *
   * @param <ID_TYPE> the id typ
   * @param entityType to which the {@link EntityDeleteMessage} relates to
   * @param entityId the primary key of the deleted entity
   */
  public <ID_TYPE> void sendEntityDeleteMessage(EntityType entityType, ID_TYPE entityId) {
    sendMessage(entityType, new EntityDeleteMessage<>(entityId));
  }

  /**
   * Registers a new SSE client. The first call also initialises the internal broadcaster.
   *
   * @param sse          the JAX-RS SSE context
   * @param sseEventSink the client's event sink
   */
  public void registerConsumer(Sse sse, SseEventSink sseEventSink) {
    initBroadcasterIfNeeded(sse);
    broadcaster.register(sseEventSink);
  }

  @SuppressWarnings("unchecked")
  private <ID_TYPE, M extends EntityUpdateMessage<ID_TYPE>> void sendMessage(EntityType entityType, M message) {
    logger.log(Level.FINE, "Sending entity {0} message for entity type {1}",
            new Object[]{message.getType(), entityType});

    if (broadcaster != null) {
      OutboundSseEvent outboundSseEvent = sse.newEventBuilder().id(UUID.randomUUID().toString())
                                             .mediaType(MediaType.APPLICATION_JSON_TYPE).name(entityType.getEventName())
                                             .data(message.getClass(), message).build();
      broadcaster.broadcast(outboundSseEvent);
    }
  }

  private ScheduledExecutorService createPingExecutorService() {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);
      thread.setName("entity-update-event-service-ping-thread");

      return thread;
    });
    scheduledExecutorService.scheduleAtFixedRate(this::sendPingMessage, PING_INTERVAL_IN_SECONDS,
            PING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    return scheduledExecutorService;
  }

  private void sendPingMessage() {
    if (sse != null && broadcaster != null) {
      OutboundSseEvent pingEvent = sse.newEventBuilder().id(UUID.randomUUID().toString()).name("ping").data("ping")
                                      .build();
      broadcaster.broadcast(pingEvent);
    }
  }

  @PreDestroy
  @Override
  public void close() throws IOException {
    logger.info("Shutting down entity update event service");
    pingExecutor.shutdownNow();
    if (broadcaster != null) {
      broadcaster.close();
    }
  }
}