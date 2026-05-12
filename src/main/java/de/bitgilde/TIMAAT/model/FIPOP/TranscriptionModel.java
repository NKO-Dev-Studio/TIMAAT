package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "transcription_model")
public class TranscriptionModel {
  @EmbeddedId
  private TranscriptionModelId id;

  @Column(name = "`default`")
  private Boolean defaultTranscriptionModel;

  @Column(name = "active")
  private Boolean active;

  public TranscriptionModelId getId() {
    return id;
  }

  public void setId(TranscriptionModelId id) {
    this.id = id;
  }

  public Boolean getDefaultTranscriptionModel() {
    return defaultTranscriptionModel;
  }

  public void setDefaultField(Boolean defaultTranscriptionModel) {
    this.defaultTranscriptionModel = defaultTranscriptionModel;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

}