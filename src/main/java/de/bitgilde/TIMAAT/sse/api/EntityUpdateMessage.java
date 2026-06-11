package de.bitgilde.TIMAAT.sse.api;

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

/**
 * Base type for SSE messages that notify clients about entity lifecycle events.
 * Every message carries a {@link EntityUpdateMessageType} discriminator so the
 * client can distinguish between {@link EntityCreateMessage}, {@link EntityChangeMessage}
 * and {@link EntityDeleteMessage} without inspecting the concrete Java type.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 08.06.26
 */
public abstract class EntityUpdateMessage<ID_TYPE> {

  private final EntityUpdateMessageType type;
  private final ID_TYPE id;

  /**
   * Constructs the base with the given type discriminator.
   *
   * @param type the lifecycle-event kind; must not be {@code null}
   */
  protected EntityUpdateMessage(ID_TYPE id, EntityUpdateMessageType type) {
    this.type = type;
    this.id = id;
  }

  /**
   * Returns the lifecycle-event kind encoded in this message.
   *
   * @return the message type; never {@code null}
   */
  public EntityUpdateMessageType getType() {
    return type;
  }

  public ID_TYPE getId() {
    return id;
  }
}