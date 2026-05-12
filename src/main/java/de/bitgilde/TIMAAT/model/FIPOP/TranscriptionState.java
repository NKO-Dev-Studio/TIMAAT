package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "transcription_state")
public class TranscriptionState {
  @Id
  @Column(name = "id", nullable = false)
  private Integer id;

  @OneToMany(mappedBy = "transcriptionState")
  private Set<TranscriptionStateTranslation> transcriptionStateTranslations;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Set<TranscriptionStateTranslation> getTranscriptionStateTranslations() {
    return transcriptionStateTranslations;
  }

  public void setTranscriptionStateTranslations(Set<TranscriptionStateTranslation> transcriptionStateTranslations) {
    this.transcriptionStateTranslations = transcriptionStateTranslations;
  }
}