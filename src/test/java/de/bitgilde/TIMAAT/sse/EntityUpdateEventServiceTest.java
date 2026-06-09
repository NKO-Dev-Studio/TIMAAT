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
import de.bitgilde.TIMAAT.sse.api.EntityUpdateMessageType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EntityUpdateEventService} covering the three dedicated send methods
 * ({@link EntityUpdateEventService#sendEntityCreateMessage}, {@link EntityUpdateEventService#sendEntityChangeMessage},
 * {@link EntityUpdateEventService#sendEntityDeleteMessage}).
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 08.06.26
 */
class EntityUpdateEventServiceTest {

  private static final EntityType ENTITY_TYPE = EntityType.TRANSCRIPTION;
  private static final Long ENTITY_ID = 1L;


  private EntityUpdateEventService service;

  private Sse sse;
  private SseBroadcaster broadcaster;
  private OutboundSseEvent.Builder eventBuilder;
  private OutboundSseEvent builtEvent;

  @BeforeEach
  void setUp() {
    service = new EntityUpdateEventService();

    sse = mock(Sse.class);
    broadcaster = mock(SseBroadcaster.class);
    eventBuilder = mock(OutboundSseEvent.Builder.class);
    builtEvent = mock(OutboundSseEvent.class);

    when(sse.newBroadcaster()).thenReturn(broadcaster);
    when(sse.newEventBuilder()).thenReturn(eventBuilder);
    when(eventBuilder.id(any())).thenReturn(eventBuilder);
    when(eventBuilder.mediaType(any())).thenReturn(eventBuilder);
    when(eventBuilder.name(any())).thenReturn(eventBuilder);
    when(eventBuilder.data(any(Class.class), any())).thenReturn(eventBuilder);
    when(eventBuilder.build()).thenReturn(builtEvent);
  }

  @AfterEach
  void tearDown() throws IOException {
    service.close();
  }

  @Test
  void sendEntityCreateMessage_doesNotThrow_whenNoBroadcasterRegistered() {
    assertThatCode(() -> service.sendEntityCreateMessage(ENTITY_TYPE, ENTITY_ID, "entity")).doesNotThrowAnyException();
  }

  @Test
  void sendEntityCreateMessage_broadcastsEvent_whenConsumerRegistered() {
    service.registerConsumer(sse, mock(SseEventSink.class));

    service.sendEntityCreateMessage(ENTITY_TYPE, ENTITY_ID, "entity");

    verify(broadcaster).broadcast(builtEvent);
  }

  @Test
  void sendEntityCreateMessage_usesJsonMediaType() {
    service.registerConsumer(sse, mock(SseEventSink.class));

    service.sendEntityCreateMessage(ENTITY_TYPE, ENTITY_ID, "entity");

    verify(eventBuilder).mediaType(MediaType.APPLICATION_JSON_TYPE);
  }

  @Test
  void sendEntityCreateMessage_sendsEntityCreateMessage_withCorrectEntity() {
    service.registerConsumer(sse, mock(SseEventSink.class));
    String entity = "myEntity";

    service.sendEntityCreateMessage(ENTITY_TYPE, ENTITY_ID, entity);

    ArgumentCaptor<EntityCreateMessage<?>> captor = ArgumentCaptor.forClass(EntityCreateMessage.class);
    verify(eventBuilder).data(eq(EntityCreateMessage.class), captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(EntityUpdateMessageType.CREATE);
    assertThat(captor.getValue().getId()).isEqualTo(ENTITY_ID);
    assertThat(captor.getValue().getEntity()).isSameAs(entity);
  }

  @Test
  void sendEntityChangeMessage_broadcastsEvent_whenConsumerRegistered() {
    service.registerConsumer(sse, mock(SseEventSink.class));
    service.sendEntityChangeMessage(ENTITY_TYPE, ENTITY_ID, "entity");

    verify(broadcaster).broadcast(builtEvent);
  }

  @Test
  void sendEntityChangeMessage_usesEntityClassSimpleNameAsEventName() {
    service.registerConsumer(sse, mock(SseEventSink.class));

    service.sendEntityChangeMessage(ENTITY_TYPE, ENTITY_ID, "entity");
    verify(eventBuilder).name("transcription");
  }

  @Test
  void sendEntityChangeMessage_sendsEntityChangeMessage_withCorrectEntity() {
    service.registerConsumer(sse, mock(SseEventSink.class));
    String entity = "myEntity";

    service.sendEntityChangeMessage(ENTITY_TYPE, ENTITY_ID, entity);

    ArgumentCaptor<EntityChangeMessage<?>> captor = ArgumentCaptor.forClass(EntityChangeMessage.class);
    verify(eventBuilder).data(eq(EntityChangeMessage.class), captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(EntityUpdateMessageType.CHANGE);
    assertThat(captor.getValue().getId()).isEqualTo(ENTITY_ID);
    assertThat(captor.getValue().getEntity()).isSameAs(entity);
  }

  @Test
  void sendEntityDeleteMessage_broadcastsEvent_whenConsumerRegistered() {
    service.registerConsumer(sse, mock(SseEventSink.class));

    service.sendEntityDeleteMessage(EntityType.TRANSCRIPTION, 1L);

    verify(broadcaster).broadcast(builtEvent);
  }

  @Test
  void sendEntityDeleteMessage_sendsEntityDeleteMessage_withCorrectMessage() {
    service.registerConsumer(sse, mock(SseEventSink.class));

    service.sendEntityDeleteMessage(ENTITY_TYPE, ENTITY_ID);

    ArgumentCaptor<EntityDeleteMessage> captor = ArgumentCaptor.forClass(EntityDeleteMessage.class);
    verify(eventBuilder).data(eq(EntityDeleteMessage.class), captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(EntityUpdateMessageType.DELETE);
    assertThat(captor.getValue().getId()).isEqualTo(ENTITY_ID);
  }
}