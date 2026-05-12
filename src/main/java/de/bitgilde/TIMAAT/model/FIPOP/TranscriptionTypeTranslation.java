package de.bitgilde.TIMAAT.model.FIPOP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transcription_type_translation")
public class TranscriptionTypeTranslation {
  @Id
  @Column(name = "id")
  private Integer id;

  @ManyToOne
  @JoinColumn(name="transcription_type_id")
  @JsonIgnore
  private TranscriptionType transcriptionType;

  @Column(name = "language_id")
  private Integer languageId;

  @Column(name = "state_name")
  private String stateName;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public TranscriptionType getTranscriptionType() {
    return transcriptionType;
  }

  public void setTranscriptionType(TranscriptionType transcriptionType) {
    this.transcriptionType = transcriptionType;
  }

  public Integer getLanguageId() {
    return languageId;
  }

  public void setLanguageId(Integer languageId) {
    this.languageId = languageId;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

}