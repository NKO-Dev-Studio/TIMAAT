package de.bitgilde.TIMAAT.storage.entity.transcription.api;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Optional;

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
 * Filters which can be passed when listing items of {@link de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage}
 *
 * @author Nico Kotlenga
 * @since 27.12.25
 */
public interface TranscriptionFilterCriteria {

  Optional<Collection<TranscriptionState>> getTranscriptionStates();

  Optional<Collection<TranscriptionType>> getTranscriptionTypes();

  Optional<Integer> getMediumId();

  class Builder {

    private Collection<TranscriptionState> transcriptionStates = null;
    private Collection<TranscriptionType> transcriptionTypes = null;
    private Integer mediumId = null;

    public TranscriptionFilterCriteria.Builder transcriptionStates(Collection<TranscriptionState> transcriptionStates) {
      this.transcriptionStates = transcriptionStates;
      return this;
    }

    public TranscriptionFilterCriteria.Builder transcriptionTypes(Collection<TranscriptionType> transcriptionTypes) {
      this.transcriptionTypes = transcriptionTypes;
      return this;
    }

    public TranscriptionFilterCriteria.Builder mediumId(@Nullable Integer mediumId) {
      this.mediumId = mediumId;
      return this;
    }

    public TranscriptionFilterCriteria build() {
      return new TranscriptionFilterCriteria() {
        @Override
        public Optional<Collection<TranscriptionState>> getTranscriptionStates() {
          return Optional.ofNullable(transcriptionStates);
        }

        @Override
        public Optional<Collection<TranscriptionType>> getTranscriptionTypes() {
          return Optional.ofNullable(transcriptionTypes);
        }

        @Override
        public Optional<Integer> getMediumId() {
          return Optional.ofNullable(mediumId);
        }
      };
    }
  }
}
