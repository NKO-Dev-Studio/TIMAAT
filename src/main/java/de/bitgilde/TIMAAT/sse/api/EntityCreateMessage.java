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
 * SSE message that signals the creation of a new entity. The full entity object
 * is embedded so clients can add it to their local state without a separate fetch.
 * Use {@link EntityUpdateEventService#sendEntityCreateMessage} to broadcast this message.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 08.06.26
 */
public class EntityCreateMessage<ID_TYPE> extends EntityUpdateMessage<ID_TYPE> {

  private final Object entity;

  /**
   * Constructs a create message carrying the given entity.
   *
   * @param id of the entity this message relates to
   * @param entity the newly created entity; must not be {@code null}
   */
  public EntityCreateMessage(ID_TYPE id, Object entity) {
    super(id, EntityUpdateMessageType.CREATE);
    this.entity = entity;
  }

  /**
   * Returns the newly created entity.
   *
   * @return the entity; never {@code null}
   */
  public Object getEntity() {
    return entity;
  }
}