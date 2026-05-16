package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "transcription_model")
public class TranscriptionModel {
  @EmbeddedId
  private TranscriptionModelId id;

  public TranscriptionModelId getId() {
    return id;
  }

  public void setId(TranscriptionModelId id) {
    this.id = id;
  }

}
