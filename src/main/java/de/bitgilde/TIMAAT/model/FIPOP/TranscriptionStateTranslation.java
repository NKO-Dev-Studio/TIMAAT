package de.bitgilde.TIMAAT.model.FIPOP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transcription_state_translation", schema = "fipop")
public class TranscriptionStateTranslation {
  @Id
  @Column(name = "id")
  private Integer id;

  @ManyToOne
  @JoinColumn(name="transcription_state_id")
  @JsonIgnore
  private TranscriptionState transcriptionState;

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

  public TranscriptionState getTranscriptionState() {
    return transcriptionState;
  }

  public void setTranscriptionState(TranscriptionState transcriptionState) {
    this.transcriptionState = transcriptionState;
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