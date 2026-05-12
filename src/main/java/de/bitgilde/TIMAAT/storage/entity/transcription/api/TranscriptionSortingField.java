package de.bitgilde.TIMAAT.storage.entity.transcription.api;

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

import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription_;
import de.bitgilde.TIMAAT.storage.db.DbSortingField;
import de.bitgilde.TIMAAT.storage.db.SortingFieldPathProducer;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 * Fields which can be used to sort transcriptions
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public enum TranscriptionSortingField implements DbSortingField<Transcription> {
  ID(root -> root.get(Transcription_.id));
  private final SortingFieldPathProducer<Transcription> pathProducer;

  TranscriptionSortingField(SortingFieldPathProducer<Transcription> pathProducer) {
    this.pathProducer = pathProducer;
  }

  @Override
  public Path<?> getPathFromRootEntity(Root<Transcription> root) {
    return pathProducer.getPathFromRoot(root);
  }
}
