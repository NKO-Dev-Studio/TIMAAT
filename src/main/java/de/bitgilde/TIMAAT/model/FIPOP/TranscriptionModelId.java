package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TranscriptionModelId implements Serializable {
  private static final long serialVersionUID = -2982668900563654074L;
  @Column(name = "model_identifier")
  private String modelIdentifier;

  @Column(name = "engine_identifier")
  private String engineIdentifier;

  public String getModelIdentifier() {
    return modelIdentifier;
  }

  public void setModelIdentifier(String modelIdentifier) {
    this.modelIdentifier = modelIdentifier;
  }

  public String getEngineIdentifier() {
    return engineIdentifier;
  }

  public void setEngineIdentifier(String engineIdentifier) {
    this.engineIdentifier = engineIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TranscriptionModelId entity = (TranscriptionModelId) o;
    return Objects.equals(this.modelIdentifier, entity.modelIdentifier) && Objects.equals(this.engineIdentifier, entity.engineIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelIdentifier, engineIdentifier);
  }
}