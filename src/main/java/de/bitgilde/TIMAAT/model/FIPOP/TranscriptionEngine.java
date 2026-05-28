package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transcription_engine")
public class TranscriptionEngine {
  @Id
  @Column(name = "engine_identifier")
  private String engineIdentifier;

  @Column(name = "engine_name")
  private String engineName;

  public String getEngineIdentifier() {
    return engineIdentifier;
  }

  public void setEngineIdentifier(String engineIdentifier) {
    this.engineIdentifier = engineIdentifier;
  }

  public String getEngineName() {
    return engineName;
  }

  public void setEngineName(String engineName) {
    this.engineName = engineName;
  }

}