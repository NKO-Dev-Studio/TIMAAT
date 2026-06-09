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

import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;

/**
 * SSE message that signals a modification to an existing entity. The updated entity
 * is embedded so clients can replace their local copy without a separate fetch.
 * Use {@link EntityUpdateEventService#sendEntityChangeMessage} to broadcast this message.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 08.06.26
 */
public class EntityChangeMessage<ID_TYPE> extends EntityUpdateMessage<ID_TYPE> {

  private final Object entity;

  /**
   * Constructs a change message carrying the updated entity.
   *
   * @param id of the entity the message relates to
   * @param entity the updated entity; must not be {@code null}
   */
  public EntityChangeMessage(ID_TYPE id, Object entity) {
    super(id, EntityUpdateMessageType.CHANGE);
    this.entity = entity;
  }

  /**
   * Returns the updated entity.
   *
   * @return the entity; never {@code null}
   */
  public Object getEntity() {
    return entity;
  }
}