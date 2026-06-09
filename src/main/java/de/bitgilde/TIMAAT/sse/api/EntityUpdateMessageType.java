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
 * Discriminator for the three kinds of entity-lifecycle events that
 * {@link EntityUpdateEventService} can broadcast: a new entity was created,
 * an existing entity was changed, or an entity was deleted.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 08.06.26
 */
public enum EntityUpdateMessageType {
  CREATE, CHANGE, DELETE
}