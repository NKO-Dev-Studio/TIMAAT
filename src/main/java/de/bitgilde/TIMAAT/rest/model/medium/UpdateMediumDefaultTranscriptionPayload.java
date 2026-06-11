package de.bitgilde.TIMAAT.rest.model.medium;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This payload can be used to update the default transcription of a {@link de.bitgilde.TIMAAT.model.FIPOP.Medium}
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 28.05.26
 */
public class UpdateMediumDefaultTranscriptionPayload {

  private static final String TRANSCRIPTION_ID_FIELD_NAME = "transcriptionId";

  @JsonProperty(TRANSCRIPTION_ID_FIELD_NAME)
  private final int transcriptionId;

  @JsonCreator
  public UpdateMediumDefaultTranscriptionPayload(@JsonProperty(TRANSCRIPTION_ID_FIELD_NAME) int transcriptionId) {
    this.transcriptionId = transcriptionId;
  }

  public int getTranscriptionId() {
    return transcriptionId;
  }
}
