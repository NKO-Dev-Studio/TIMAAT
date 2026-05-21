package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "transcription_type")
public class TranscriptionType {
  @Id
  @Column(name = "id", nullable = false)
  private Integer id;

  @OneToMany(mappedBy = "transcriptionType")
  private Set<TranscriptionTypeTranslation> transcriptionTypeTranslations;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Set<TranscriptionTypeTranslation> getTranscriptionTypeTranslations() {
    return transcriptionTypeTranslations;
  }

  public void setTranscriptionTypeTranslations(Set<TranscriptionTypeTranslation> transcriptionTypeTranslations) {
    this.transcriptionTypeTranslations = transcriptionTypeTranslations;
  }
}