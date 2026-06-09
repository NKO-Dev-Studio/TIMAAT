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
 * SSE message that signals the deletion of an entity. Because the entity no
 * longer exists in the database, only its primary key is transmitted so clients
 * can remove the corresponding entry from their local state.
 * Use {@link EntityUpdateEventService#sendEntityDeleteMessage} to broadcast this message.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 08.06.26
 */
public class EntityDeleteMessage<ID_TYPE> extends EntityUpdateMessage<ID_TYPE> {

  /**
   * Constructs a delete message for the entity with the given primary key.
   *
   * @param id of the entity
   */
  public EntityDeleteMessage(ID_TYPE id) {
    super(id, EntityUpdateMessageType.DELETE);
  }
}